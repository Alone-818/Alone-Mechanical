package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid = Alone_mechanical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{GasWaterRendering.register(event);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e) { e.enqueueWork(() -> {
        MenuScreens.<Alone818.com.alone_mechanical.menu.MachineMenu,net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<Alone818.com.alone_mechanical.menu.MachineMenu>>register(ModMenus.MACHINE.get(), (menu,inventory,title)->menu.machine.turbine()?new TurbineScreen(menu,inventory,title):new MachineScreen(menu,inventory,title)); MenuScreens.register(ModMenus.GRID.get(), GridScreen::new);
        MenuScreens.register(ModMenus.STORAGE.get(), StorageScreen::new);
        MenuScreens.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
        MenuScreens.register(ModMenus.PUMP.get(),PumpScreen::new);
        MenuScreens.register(ModMenus.ENERGY_MATRIX.get(),EnergyMatrixScreen::new);
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.STRUCTURAL_GLASS.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLUID_TANK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GAS_TANK.get(),RenderType.cutout());ItemBlockRenderTypes.setRenderLayer(ModBlocks.STORAGE_TANK.get(),RenderType.cutout());ItemBlockRenderTypes.setRenderLayer(ModBlocks.ITEM_VAULT.get(),RenderType.cutout());
    }); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.GRID_NODE.get(), GridWireRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.PIPE.get(), PipeConnectorRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.STORAGE.get(), FluidTankRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.CREATIVE_TANK.get(), CreativeTankRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.GENERATOR.get(), SolarPanelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.MACHINE_PART.get(),TurbineRotorRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.MACHINE.get(),TowerFluidRenderer::new);
    }
    @SubscribeEvent public static void additionalModels(ModelEvent.RegisterAdditional e) {
        e.register(PipeConnectorRenderer.PULL_MODEL); e.register(PipeConnectorRenderer.PUSH_MODEL);
        e.register(SolarPanelRenderer.SOLAR_PANEL);e.register(SolarPanelRenderer.LUNAR_PANEL);
        e.register(TurbineRotorRenderer.FRAME);e.register(TurbineRotorRenderer.FAN);
        e.register(new net.minecraft.resources.ResourceLocation("alone_mechanical","block/tank_gas"));
        for(int mask=0;mask<256;mask++) if(Alone818.com.alone_mechanical.block.StructuralGlassBlock.validMask(mask)) e.register(ConnectedGlassModel.variant(mask));
        for(String family:new String[]{"storage_tank","item_vault"}){for(int mask=0;mask<4;mask++)e.register(ConnectedStorageModel.variant(family,false,mask));for(int mask=0;mask<16;mask++)e.register(ConnectedStorageModel.variant(family,true,mask));}
    }
    @SubscribeEvent public static void connectedGlass(ModelEvent.ModifyBakingResult e) {
        for(var entry:ModBlocks.BLOCKS.getEntries())if(entry.get() instanceof Alone818.com.alone_mechanical.block.ConnectedStorageBlock storage){
            String family=storage.kind.isTank()?"storage_tank":"item_vault";java.util.Map<String,net.minecraft.client.resources.model.BakedModel> parts=new java.util.HashMap<>();
            for(int mask=0;mask<4;mask++)parts.put("side_"+mask,e.getModels().get(ConnectedStorageModel.variant(family,false,mask)));
            for(int mask=0;mask<16;mask++)parts.put("top_"+mask,e.getModels().get(ConnectedStorageModel.variant(family,true,mask)));
            for(var state:storage.getStateDefinition().getPossibleStates()){var key=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);var original=e.getModels().get(key);if(original!=null)e.getModels().put(key,new ConnectedStorageModel(original,parts));}
        }
        java.util.Map<Integer,net.minecraft.client.resources.model.BakedModel> variants=new java.util.HashMap<>();
        for(int mask=0;mask<256;mask++) if(Alone818.com.alone_mechanical.block.StructuralGlassBlock.validMask(mask))
            variants.put(mask,e.getModels().get(ConnectedGlassModel.variant(mask)));
        // Only block-state entries are wrapped; the inventory model keeps its single-block frame.
        for(var state:ModBlocks.STRUCTURAL_GLASS.get().getStateDefinition().getPossibleStates()) {
            var key=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
            var original=e.getModels().get(key);
            if(original!=null) e.getModels().put(key,new ConnectedGlassModel(original,variants));
        }
    }
    private ClientSetup() {}
}
