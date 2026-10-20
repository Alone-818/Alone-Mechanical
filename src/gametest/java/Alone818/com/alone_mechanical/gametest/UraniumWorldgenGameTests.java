package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModItems;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class UraniumWorldgenGameTests {
    private static final ResourceLocation ID=new ResourceLocation(Alone_mechanical.MODID,"ore_uranium");
    private static PlacedFeature feature(GameTestHelper h){return h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getOptional(ID).orElseThrow();}
    private static com.google.gson.JsonObject placement(PlacedFeature feature,int index){return PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE,feature.placement().get(index)).result().orElseThrow().getAsJsonObject();}

    @GameTest(template="empty") public static void worldgenRegistryAndFrequencyFallBetweenOrdinaryGoldAndIron(GameTestHelper h){
        var placed=feature(h);var configured=placed.feature().value();
        h.assertTrue(configured.feature()==Feature.ORE&&configured.config() instanceof OreConfiguration,"Data pack registers a real vanilla ore feature");
        var config=(OreConfiguration)configured.config();
        h.assertTrue(config.size==8&&config.targetStates.size()==2&&config.discardChanceOnAirExposure==0,"Vein size 8 has both stone/deepslate targets without added cave rejection");
        h.assertTrue(placed.placement().size()==4&&placed.placement().get(0) instanceof CountPlacement&&placed.placement().get(1) instanceof InSquarePlacement&&placed.placement().get(2) instanceof HeightRangePlacement&&placed.placement().get(3) instanceof BiomeFilter,"Count, square spread, absolute height and biome filter are all applied");
        int uranium=placement(placed,0).get("count").getAsInt();
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        int gold=placement(registry.get(new ResourceLocation("minecraft","ore_gold")),0).get("count").getAsInt();
        int iron=placement(registry.get(new ResourceLocation("minecraft","ore_iron_middle")),0).get("count").getAsInt();
        h.assertTrue(uranium==8&&uranium>gold+1&&uranium<iron,"8 attempts exceed ordinary gold's 4 plus at most 1 lower attempt and stay below 10 middle iron attempts (iron also has other bands)");h.succeed();
    }

    @GameTest(template="empty") public static void biomeModifierAddsOreExactlyOnceToOverworldBiomesOnly(GameTestHelper h){
        var featureKey=net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,ID);
        int overworld=0;
        for(var biome:h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).holders().toList()){
            var steps=biome.value().getGenerationSettings().features();int step=GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
            long occurrences=steps.stream().flatMap(HolderSet::stream).filter(f->f.is(featureKey)).count();
            if(biome.is(BiomeTags.IS_OVERWORLD)){
                overworld++;h.assertTrue(occurrences==1&&steps.size()>step&&steps.get(step).stream().anyMatch(f->f.is(featureKey)),"Overworld biome has exactly one uranium placement in underground ores: "+biome.key().location());
            }else h.assertTrue(occurrences==0,"Other biome families must not receive uranium ore: "+biome.key().location());
        }
        h.assertTrue(overworld>0,"Overworld biome tag is populated");
        h.assertTrue(!h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.NETHER_WASTES).is(BiomeTags.IS_OVERWORLD),"Nether is excluded");h.succeed();
    }

    @GameTest(template="empty") public static void actualPlacementSamplesStayInHeightBandAndPeakNearZero(GameTestHelper h){
        var placed=feature(h);var context=new PlacementContext(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),Optional.of(placed));var random=RandomSource.create(818);
        int middle=0,edges=0,total=0;
        for(int sample=0;sample<2000;sample++){
            List<BlockPos> positions=List.of(BlockPos.ZERO);
            for(var modifier:placed.placement()){
                if(modifier instanceof BiomeFilter)continue;
                var previous=positions;positions=previous.stream().flatMap(p->modifier.getPositions(context,random,p)).toList();
            }
            h.assertTrue(positions.size()==8,"Actual placement pipeline samples eight attempts per chunk");
            for(var pos:positions){
                h.assertTrue(pos.getY()>=-64&&pos.getY()<=64&&pos.getX()>=0&&pos.getX()<16&&pos.getZ()>=0&&pos.getZ()<16,"All sampled origins respect their height and horizontal chunk bounds");
                if(Math.abs(pos.getY())<=16)middle++;if(Math.abs(pos.getY())>=48)edges++;total++;
            }
        }
        h.assertTrue(total==16000&&middle>edges*3,"Triangular sampling favors Y=0 over equally wide extreme bands");h.succeed();
    }

    @GameTest(template="empty") public static void registeredFeatureActuallyReplacesStoneAndDeepslateAndPreservesOtherBlocks(GameTestHelper h){
        var configured=feature(h).feature().value();var center=h.absolutePos(new BlockPos(10,10,10));var min=center.offset(-7,-7,-7);var max=center.offset(7,7,7);
        for(var base:new Block[]{Blocks.STONE,Blocks.DEEPSLATE,Blocks.NETHERRACK}){
            for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlock(pos,base.defaultBlockState(),2);
            for(int seed=0;seed<24;seed++)configured.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),RandomSource.create(seed),center);
            int ordinary=0,deep=0;
            for(var pos:BlockPos.betweenClosed(min,max)){var state=h.getLevel().getBlockState(pos);if(state.is(ModBlocks.URANIUM_ORE.get()))ordinary++;if(state.is(ModBlocks.DEEPSLATE_URANIUM_ORE.get()))deep++;}
            if(base==Blocks.STONE)h.assertTrue(ordinary>0&&deep==0,"Actual ore feature selects ordinary uranium in stone");
            else if(base==Blocks.DEEPSLATE)h.assertTrue(deep>0&&ordinary==0,"Actual ore feature selects deep uranium in deepslate");
            else h.assertTrue(ordinary==0&&deep==0,"Netherrack is not a valid ore replacement target");
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void eitherOreVariantFeedsTheExistingUraniumLineAndSilkTouchKeepsOre(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,3,3));var pick=new ItemStack(Items.IRON_PICKAXE);var silk=pick.copy();silk.enchant(Enchantments.SILK_TOUCH,1);
        for(var ore:new Block[]{ModBlocks.URANIUM_ORE.get(),ModBlocks.DEEPSLATE_URANIUM_ORE.get()}){
            var state=ore.defaultBlockState();h.getLevel().setBlock(pos,state,2);
            h.assertTrue(pick.isCorrectToolForDrops(state)&&!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),"Mining requires iron or better");
            var raw=Block.getDrops(state,h.getLevel(),pos,null,null,pick);var preserved=Block.getDrops(state,h.getLevel(),pos,null,null,silk);
            h.assertTrue(raw.size()==1&&raw.get(0).is(ModItems.RAW_URANIUM.get())&&raw.get(0).getCount()==1,"Ordinary mining produces the existing raw uranium item");
            h.assertTrue(preserved.size()==1&&preserved.get(0).is(ore.asItem()),"Silk Touch preserves the matching ore variant");
        }
        h.succeed();
    }
}
