package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class TutorialBookGameTests {
    @GameTest(template="empty")
    public static void rightClickIsServerSafeAndPreservesTheBook(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var item=ModItems.TUTORIAL_BOOK.get();
        for(var hand:InteractionHand.values()) {
            player.setItemInHand(hand,new ItemStack(item));var result=item.use(h.getLevel(),player,hand);
            h.assertTrue(result.getResult().consumesAction() && result.getObject()==player.getItemInHand(hand) && result.getObject().getCount()==1,"Server use must succeed without loading client screens or consuming a book");
        }
        h.assertTrue(item.getDefaultInstance().getMaxStackSize()==1,"Tutorial book must be a single utility item");h.succeed();
    }
    @GameTest(template="empty")
    public static void bookTakesPriorityOverBlockActions(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var p=h.absolutePos(new BlockPos(3,2,3));h.setBlock(new BlockPos(3,2,3),Blocks.STONE);
        var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TUTORIAL_BOOK.get()));
        var event=new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,p,hit);MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction() && h.getLevel().getBlockState(p).is(Blocks.STONE),"Book interaction must suppress block use without altering the world");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.TUTORIAL_BOOK.get()));
        var offhand=new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,p,hit);MinecraftForge.EVENT_BUS.post(offhand);
        h.assertTrue(offhand.isCanceled() && player.getOffhandItem().getCount()==1,"An offhand guide must open when the main hand is empty");h.succeed();
    }
    @GameTest(template="empty")
    public static void guideCanBeCraftedFromBookAndIronNugget(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),2,2);
        grid.setItem(0,new ItemStack(Items.BOOK));grid.setItem(3,new ItemStack(Items.IRON_NUGGET));
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation(Alone_mechanical.MODID,"tutorial_book")).orElseThrow();
        h.assertTrue(recipe.matches(grid,h.getLevel()) && recipe.assemble(grid,h.getLevel().registryAccess()).is(ModItems.TUTORIAL_BOOK.get()),"The utility guide must have a valid shapeless survival recipe");
        grid.setItem(3,new ItemStack(Items.COBBLESTONE));h.assertFalse(recipe.matches(grid,h.getLevel()),"Unrelated ingredients must not craft the guide");h.succeed();
    }
}
