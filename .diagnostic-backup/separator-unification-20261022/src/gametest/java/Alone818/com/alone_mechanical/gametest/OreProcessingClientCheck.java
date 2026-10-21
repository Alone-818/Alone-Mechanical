package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import Alone818.com.alone_mechanical.recipe.RecipeDisplays;
import com.mojang.logging.LogUtils;
import mezz.jei.api.*;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@JeiPlugin
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class OreProcessingClientCheck implements IModPlugin {
    private static IJeiRuntime runtime;
    private static boolean done;
    private static int stage,ticks,total;
    private static final List<mezz.jei.api.gui.IRecipeLayoutDrawable> previews=new ArrayList<>();
    private static boolean redesign(){return Boolean.getBoolean("alone_mechanical.verifyOreRedesign");}
    private static boolean nonmetal(){return redesign()||Boolean.getBoolean("alone_mechanical.verifyNonmetalProcessing");}
    private static boolean enabled(){return Boolean.getBoolean("alone_mechanical.verifyOreProcessing")||nonmetal();}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","ore_processing_check");}
    @Override public void onRuntimeAvailable(IJeiRuntime value){if(enabled())runtime=value;}
    private static void verify(Minecraft mc,boolean nonmetalMode){
        for(var item:nonmetalMode?ModItems.NONMETAL_ORE_INTERMEDIATES:ModItems.ORE_INTERMEDIATES){
            var stack=new ItemStack(item.get());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            require(model!=mc.getModelManager().getMissingModel()&&model.getParticleIcon().contents().width()==16,"Intermediate item must have a native baked sprite: "+item.getId());
            var text=new ArrayList<Component>();ItemDescriptions.append(stack,text,true);
            require(text.size()==2&&text.stream().noneMatch(c->c.getString().startsWith("tooltip.")),"Intermediate Shift descriptions must be translated");
        }
        for(var fluid:nonmetalMode?List.<ModFluids.ProcessFluid>of():ModFluids.ORE_PROCESS_FLUIDS){
            var stack=new FluidStack(fluid.source().get(),1000);var sprite=FluidRendering.sprite(stack);
            require(!sprite.contents().name().getPath().contains("missingno")&&sprite.contents().getUniqueFrames().count()>1,"Process fluid must have an animated atlas sprite");
            require(!stack.getDisplayName().getString().startsWith("fluid."),"Process fluid name must be translated");
            require(IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor()!=0,"Process fluid must have a visible tint");
        }
        int count=0;
        for(String machine:nonmetalMode?List.of("crusher","infuser"):List.of("crusher","infuser","reaction","separator_slurry_separation","separator_liquid_fractionation")){
            var type=RecipeType.create("alone_mechanical",machine,RecipeDisplays.Display.class);var category=runtime.getRecipeManager().getRecipeCategory(type);
            require(category!=null,"Ore JEI category missing: "+machine);
            for(var recipe:runtime.getRecipeManager().createRecipeLookup(type).get().toList())if(recipe.id().getNamespace().equals("alone_mechanical")&&recipe.id().getPath().startsWith("ore_")&&recipe.id().getPath().startsWith("ore_nonmetal_")==nonmetalMode){
                var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
                require(!layout.getRecipeSlotsView().getSlotViews().isEmpty(),"Ore JEI recipe has no ingredients");
                if(!recipe.id().getPath().equals("ore_acid_recovery"))require(!recipe.gasInfo().isBlank()&&!recipe.gasInfo().contains("jei.alone_mechanical"),"JEI must show translated final-route yield separately from the single step");
                if((nonmetalMode?List.of("ore_nonmetal_wash_lapis","ore_nonmetal_crush_washed_diamond","ore_nonmetal_infusion_redstone","ore_nonmetal_press_diamond"):List.of("ore_infusion_iron","ore_separate_iron","ore_reduce_iron","ore_acid_recovery")).contains(recipe.id().getPath()))previews.add(layout);
                count++;
            }
        }
        require(count==(nonmetalMode?15:19)&&previews.size()==4,"JEI must register every processing step and four representative layouts");
        LogUtils.getLogger().info("{} JEI CHECK PASSED: {} real layouts, translated final-route yields and six native item models",nonmetalMode?"NONMETAL":"ORE",count);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!enabled()||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++total<2400,"Ore client preview timed out");
            if(mc.player==null||mc.level==null||runtime==null)return;
            if(stage==0){if(mc.screen!=null)return;if(redesign()){verify(mc,false);previews.clear();}verify(mc,nonmetal());mc.setScreen(new Materials());stage=1;return;}
            if(++ticks<25)return;ticks=0;
            if(stage==1){capture("ore-materials");mc.setScreen(new Recipes());stage=2;return;}
            if(stage==2){
                capture("ore-jei");var guide=new TutorialScreen();mc.setScreen(guide);boolean found=false;
                for(int i=0;i<guide.chapterCount();i++){guide.selectChapter(i);if(guide.chapterId().equals(nonmetal()?"nonmetal_processing":"ore_processing")){found=true;break;}}
                require(found&&!guide.chapterContent().getString().contains("tutorial.alone_mechanical"),"Localized ore-processing tutorial chapter must exist");stage=3;return;
            }
            capture("ore-tutorial");LogUtils.getLogger().info("{} PROCESSING CLIENT CHECK PASSED: materials, actual JEI recipes and tutorial chapter",nonmetal()?"NONMETAL":"ORE");done=true;mc.stop();
        }catch(Exception error){LogUtils.getLogger().error("ORE PROCESSING CLIENT CHECK FAILED",error);done=true;mc.stop();}
    }
    private static void capture(String name)throws java.io.IOException{
        var path=Path.of("..","build",redesign()?"ore-redesign-client":nonmetal()?"nonmetal-simple-client":"ore-processing-client",name+".png");Files.createDirectories(path.getParent());
        try(var screenshot=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){screenshot.writeToFile(path);}
    }
    private static final class Materials extends Screen {
        Materials(){super(Component.literal("Ore processing materials"));}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xff17232d);g.drawCenteredString(font,redesign()?"Ore processing · Clumps / Crystals / Shards":nonmetal()?"Diamond / Redstone / Lapis · Clumps / Shards":"2x / 3x / 5x · Ore processing",width/2,12,0xffe4edf1);
            var materials=new ArrayList<>(nonmetal()?ModItems.NONMETAL_ORE_INTERMEDIATES:ModItems.ORE_INTERMEDIATES);
            if(redesign()){materials.clear();materials.addAll(ModItems.ORE_INTERMEDIATES);materials.addAll(ModItems.NONMETAL_ORE_INTERMEDIATES);}
            for(int i=0;i<materials.size();i++){
                int x=34+(i%3)*125,y=(redesign()?34:42)+(i/3)*(redesign()?34:42);var stack=new ItemStack(materials.get(i).get());
                g.fill(x-4,y-4,x+24,y+24,0xff2b4351);g.renderItem(stack,x+2,y+2);g.drawString(font,stack.getHoverName(),x+30,y+6,0xffdbe5e9,false);
            }
            if(nonmetal()){
                var products=List.of(net.minecraft.world.item.Items.DIAMOND,net.minecraft.world.item.Items.REDSTONE,net.minecraft.world.item.Items.LAPIS_LAZULI);
                int offset=redesign()?40:0;
                for(int i=0;i<3;i++){int x=34+i*125;g.renderItem(new ItemStack(products.get(i)),x+2,145+offset);g.drawString(font,new String[]{"2 / 3 / 5","10 / 15 / 25","18 / 27 / 45"}[i],x+30,150+offset,0xffdbe5e9,false);}
                g.drawCenteredString(font,"Shared clump crushing · Nonmetal: at most three steps",width/2,redesign()?214:201,0xff8cd5bd);return;
            }
            for(int i=0;i<7;i++){
                int x=20+i*56;var stack=new FluidStack(ModFluids.ORE_PROCESS_FLUIDS.get(i).source().get(),1000);
                g.fill(x-1,133,x+29,192,0xff718492);FluidRendering.draw(g,stack,1000,x,134,28,57);
                g.drawWordWrap(font,stack.getDisplayName(),x-4,198,52,0xffdbe5e9);
            }
        }
    }
    private static final class Recipes extends Screen {
        Recipes(){super(Component.literal("Ore processing JEI"));}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xff17232d);g.drawCenteredString(font,nonmetal()?"Actual JEI: purification / clumps / fracturing / washing":"Actual JEI: infusion / crystals / reduction / recovery",width/2,12,0xffe4edf1);
            for(int i=0;i<previews.size();i++){
                var pose=g.pose();pose.pushPose();pose.translate(18+(i%2)*211,43+(i/2)*98,0);pose.scale(.95f,.95f,1);
                g.fill(-4,-4,172,94,0xffeeeeee);previews.get(i).drawRecipe(g,-100,-100);g.flush();pose.popPose();
            }
        }
    }
}
