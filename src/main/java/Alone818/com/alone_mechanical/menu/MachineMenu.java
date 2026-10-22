package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.init.ModMenus;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import java.util.Objects;

public class MachineMenu extends AbstractContainerMenu implements FluidMenuData.Access,GasMenuData.Access {
    public static final int GUI_WIDTH = 358, GUI_HEIGHT = 232;
    public static final int INPUT_X = 8, OUTPUT_X = 188, STORAGE_Y = 41;
    public static final int INVENTORY_X = 188, INVENTORY_Y = 150, HOTBAR_Y = 208;
    public final MachineBlockEntity machine;
    private final SyncedIntData data;
    private final net.minecraft.core.BlockPos anchor;
    private int inputPage, outputPage;
    private final FluidMenuData fluidData;
    public final boolean liquidReactor;
    public final boolean reactor;
    public final boolean separator;
    public final boolean synthesis;
    public boolean boiler(){return machine.boiler();}
    public boolean towerChannelActive(int channel){if(machine.airSeparationTower())return channel>=2;return !machine.tower()||machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.EXCHANGE_TOWER)||channel!=1&&(channel!=3||machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.DISTILLATION_TOWER));}
    public boolean threeColumns(){return !fourColumns()&&(reactor||separator);}
    public boolean fourColumns(){return machine.heatProcessor()||machine.tower();}
    public Alone818.com.alone_mechanical.machine.PartKind storeKind(int channel){return machine.tower()?value(45+channel)==1?Alone818.com.alone_mechanical.machine.PartKind.GAS_TANK:Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK:machine.reactorStore(channel);}
    public int columnChannel(int column){return separator?(column==0?0:column+1):column;}
    public final boolean converter;
    public static final int CONVERT_GAS_TO_LIQUID=10,CONVERT_LIQUID_TO_GAS=11;
    public static final int TOWER_SPEED_BASE=20;
    public static final int AIR_TOGGLE=10;
    public static final int REACTOR_TOGGLE=140;
    public static final int GAS_OUTPUT_MODE_BASE=130;
    public static final int SYNTHESIS_PREVIOUS=200, SYNTHESIS_NEXT=201;
    private final GasMenuData gasData;
    private int secondInputPage;
    private final int ownSlots;
    public final boolean fieldMachine,breeding;
    public boolean harvesting(){return machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.HARVEST);}
    public boolean fieldConfiguration;
    public boolean configurationView;
    public boolean populationMachine(){return fieldMachine&&(breeding||machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.SLAUGHTER));}
    private int targetCursor;
    public net.minecraft.world.entity.EntityType<?> targetType(){var types=Alone818.com.alone_mechanical.machine.FieldAutomation.selectable();return types.get(Math.floorMod(value(31),types.size()));}
    private record Opening(MachineBlockEntity machine,net.minecraft.core.BlockPos anchor){}
    private static Opening opening(Inventory inv,FriendlyByteBuf buf){
        var machine=(MachineBlockEntity)Objects.requireNonNull(inv.player.level().getBlockEntity(buf.readBlockPos()));var anchor=buf.readBlockPos();
        // A newly selected component can reach the client after the menu-open packet.
        // Field menus include their authoritative snapshot so slot counts never use the old machine type.
        if(buf.readableBytes()>0){var tag=buf.readNbt();if(tag!=null)machine.load(tag);}
        return new Opening(machine,anchor);
    }
    private MachineMenu(int id,Inventory inv,Opening opening){this(id,inv,opening.machine,opening.anchor);}
    public MachineMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id,inv,opening(inv,buf)); }
    public MachineMenu(int id, Inventory inv, MachineBlockEntity machine) {this(id,inv,machine,machine.getBlockPos());}
    public MachineMenu(int id, Inventory inv, MachineBlockEntity machine,net.minecraft.core.BlockPos anchor) {
        super(ModMenus.MACHINE.get(), id); this.machine = machine;
        this.anchor=anchor.immutable();
        boolean client = inv.player.level().isClientSide;
        liquidReactor=machine.liquidReactor();reactor=machine.tankReactor()||machine.alloySmelter()||machine.infuser()||machine.turbine()||machine.heatProcessor()||machine.tower();
        separator=machine.separator();
        synthesis=machine.synthesisMachine();
        converter=machine.converter();
        fieldMachine=machine.fieldMachine();breeding=machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.BREEDING);
        fieldConfiguration=fieldMachine&&!populationMachine()&&!harvesting();
        int itemStores=0;for(int i=0;i<3;i++)if(machine.reactorStore(columnChannel(i))==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT)itemStores++;
        ownSlots=converter||fourColumns()?0:threeColumns()?itemStores*9:fieldMachine&&!breeding&&!harvesting()?27:54;
        fluidData=new FluidMenuData(inv.player,4,machine::fluidChannel);
        gasData=new GasMenuData(inv.player,4,machine::gasChannel);
        data = new SyncedIntData(62, client ? null : i -> switch (i) {
            case 52 -> machine.reactorSafety.temperature();
            case 53 -> machine.reactorSafety.residual();
            case 54 -> machine.reactorSafety.enabled()?1:0;
            case 55 -> machine.reactorSafety.grade();
            case 56 -> machine.reactorSafety.failed()?1:0;
            case 57 -> machine.reactorSafety.stage();
            case 58 -> machine.reactorSafety.countdown();
            case 59 -> machine.reactorSafety.pressure();
            case 60 -> machine.synthesisRecipeIndex();
            case 61 -> machine.synthesisRecipeCount();
            case 50,51 -> machine.gasOutputMode(i-48).ordinal();
            case 49 -> machine.towerContents.enabled()?1:0;
            case 40 -> machine.towerContents.speedPercent();
            case 41 -> machine.towerContents.volume();
            case 42 -> machine.towerContents.lowerVolume();
            case 43 -> machine.towerContents.upperVolume();
            case 44 -> machine.towerContents.processingBudget();
            case 45,46,47,48 -> machine.reactorStore(i-45)==Alone818.com.alone_mechanical.machine.PartKind.GAS_TANK?1:0;
            case 37 -> machine.turbineRotors();
            case 38 -> machine.turbineRotors()*Alone818.com.alone_mechanical.Config.TURBINE_FE_PER_ROTOR.get();
            case 39 -> machine.turbineRotors()*Alone818.com.alone_mechanical.Config.TURBINE_STEAM_PER_ROTOR.get();
            case 36 -> machine.field.limit(targetType());
            case 35 -> machine.speedPercent();
            case 31 -> targetCursor;case 32 -> machine.field.enabled(targetType())?1:0;
            case 33 -> Alone818.com.alone_mechanical.Config.FIELD_RADIUS.get();case 34 -> Alone818.com.alone_mechanical.Config.FIELD_HEIGHT.get();
            case 0 -> machine.formed() ? 1 : 0; case 1 -> machine.powered() ? 1 : 0;
            case 2 -> machine.progress(); case 3 -> machine.duration(); case 4 -> machine.energyCost();
            case 5 -> machine.energyCoils(); case 6 -> machine.efficiencyCoils(); case 7 -> machine.reinforced();
            case 16 -> machine.energy().getEnergyStored(); case 17 -> machine.batteryCapacity(); case 18 -> machine.energyMatrices();
            case 19 -> machine.itemChannel(0).getSlots(); case 20 -> machine.itemChannel(2).getSlots();
            case 21 -> inputPage; case 22 -> outputPage;
            case 23 -> pages(0); case 24 -> pages(2);
            case 25 -> machine.processingMatrices(); case 26 -> machine.parallelCapacity(); case 27 -> machine.activeBatch();
            case 28 -> secondInputPage;case 29 -> pages(separator?3:1);
            case 30 -> machine.conversionMode().ordinal();
            default -> i < 12 ? machine.fluidChannel(i-8).getFluidInTank(0).getAmount() : machine.gasChannel(i-12).contents().amount();
        });
        addDataSlots(data);
        var input = MenuItemPage.create(client, () -> machine.itemChannel(0), () -> inputPage);
        var output = MenuItemPage.create(client, () -> machine.itemChannel(2), () -> outputPage);
        if(threeColumns()){
            for(int column=0;column<3;column++)if(machine.reactorStore(columnChannel(column))==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT){
                int role=columnChannel(column);var paged=MenuItemPage.create(client,()->machine.itemChannel(role),()->page(role),9);
                for(int y=0;y<3;y++)for(int x=0;x<3;x++)addSlot(new SlotItemHandler(paged,x+y*3,14+column*115+x*18,43+y*18){@Override public boolean mayPlace(ItemStack s){return role<2;}@Override public boolean isActive(){return !configurationView;}});
            }
        }else if(!converter&&!fourColumns()){
            if(!fieldMachine||breeding||harvesting())for (int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new SlotItemHandler(input,x+y*9,INPUT_X+x*18,STORAGE_Y+y*18){@Override public boolean mayPlace(ItemStack stack){return !harvesting()||stack.is(Alone818.com.alone_mechanical.init.ModItems.FERTILIZER.get());}@Override public boolean isActive(){return !configurationView&&(!fieldMachine||!fieldConfiguration);}});
            for (int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new SlotItemHandler(output,x+y*9,OUTPUT_X+x*18,STORAGE_Y+y*18){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean isActive(){return !configurationView&&(!populationMachine()||!fieldConfiguration);}});
        }
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x + y * 9 + 9, INVENTORY_X + x * 18, INVENTORY_Y + y * 18){@Override public boolean isActive(){return !configurationView&&(!populationMachine()||!fieldConfiguration);}});
        for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x, INVENTORY_X + x * 18, HOTBAR_Y){@Override public boolean isActive(){return !configurationView&&(!populationMachine()||!fieldConfiguration);}});
    }
    public int value(int i) { return data.value(i); }
    public net.minecraft.network.chat.Component synthesisTargetName(){
        if(!synthesis||machine.getLevel()==null)return net.minecraft.network.chat.Component.empty();
        var recipes=machine.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
            .filter(recipe->!recipe.getResultItem(machine.getLevel().registryAccess()).isEmpty()&&recipe.getIngredients().stream().anyMatch(i->!i.isEmpty()))
            .sorted(java.util.Comparator.comparing(recipe->recipe.getId().toString())).toList();
        if(recipes.isEmpty())return net.minecraft.network.chat.Component.translatable("gui.alone_mechanical.no_recipe");
        return recipes.get(Math.min(value(60),recipes.size()-1)).getResultItem(machine.getLevel().registryAccess()).getHoverName();
    }
    public Alone818.com.alone_mechanical.machine.ConversionMode conversionMode(){return Alone818.com.alone_mechanical.machine.ConversionMode.byIndex(value(30));}
    @Override public FluidMenuData fluidData(){return fluidData;}
    @Override public GasMenuData gasData(){return gasData;}
    @Override public void broadcastChanges(){super.broadcastChanges();fluidData.broadcast(containerId);gasData.broadcast(containerId);}
    private int page(int channel){return channel==0?inputPage:channel==2?outputPage:secondInputPage;}
    public int pageValue(int channel){return value(channel==0?21:channel==2?22:28);}
    public int pageCount(int channel){return value(channel==0?23:channel==2?24:29);}
    private int pages(int channel) {int size=threeColumns()?9:27;return Math.max(1, (machine.itemChannel(channel).getSlots()+size-1)/size); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if(button==SYNTHESIS_PREVIOUS||button==SYNTHESIS_NEXT){
            if(!synthesis||player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;
            return machine.cycleSynthesisRecipe(button==SYNTHESIS_PREVIOUS?-1:1);
        }
        if(button==REACTOR_TOGGLE){
            if(player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;
            boolean changed=machine.toggleReactor();if(changed)broadcastChanges();return changed;
        }
        if(button>=GAS_OUTPUT_MODE_BASE&&button<GAS_OUTPUT_MODE_BASE+6){
            if(player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;
            int index=button-GAS_OUTPUT_MODE_BASE;boolean changed=machine.setGasOutputMode(2+index/3,Alone818.com.alone_mechanical.machine.GasOutputMode.byIndex(index%3));if(changed)broadcastChanges();return changed;
        }
        if(machine.tower()){
            if(machine.airSeparationTower()&&button==AIR_TOGGLE){
                if(player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;
                boolean changed=machine.setAirSeparationEnabled(!machine.towerContents.enabled());if(changed)broadcastChanges();return changed;
            }
            if(button<TOWER_SPEED_BASE+1||button>TOWER_SPEED_BASE+100||player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;
            boolean changed=machine.setTowerSpeedPercent(button-TOWER_SPEED_BASE);if(changed)broadcastChanges();return changed;
        }
        if(fieldMachine&&button>=20&&button<=22){
            if(player.level().isClientSide||player.containerMenu!=this||!stillValid(player)||!breeding&&!machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.SLAUGHTER))return false;
            if(button==22)machine.field.configure(targetType(),machine.field.limit(targetType()),!machine.field.enabled(targetType()));else targetCursor=Math.floorMod(targetCursor+(button==20?-1:1),Alone818.com.alone_mechanical.machine.FieldAutomation.selectable().size());
            broadcastChanges();return true;
        }
        if(converter){
            if((button!=CONVERT_GAS_TO_LIQUID&&button!=CONVERT_LIQUID_TO_GAS)||player.level().isClientSide||!stillValid(player))return false;
            boolean changed=machine.setConversionMode(Alone818.com.alone_mechanical.machine.ConversionMode.byIndex(button-CONVERT_GAS_TO_LIQUID));
            if(changed)broadcastChanges();return changed;
        }
        if (button < 0 || button > (threeColumns()?5:3) || !stillValid(player)) return false;
        if(button>=4)secondInputPage=Math.max(0,Math.min(pages(separator?3:1)-1,secondInputPage+(button==4?-1:1)));
        else if (button < 2) inputPage = Math.max(0, Math.min(pages(0)-1, inputPage + (button==0 ? -1 : 1)));
        else outputPage = Math.max(0, Math.min(pages(2)-1, outputPage + (button==2 ? -1 : 1)));
        broadcastChanges(); return true;
    }
    @Override public boolean stillValid(Player p) {
        return !machine.isRemoved() && p.distanceToSqr(anchor.getX() + .5, anchor.getY() + .5, anchor.getZ() + .5) <= 100
            && (p.level().isClientSide || machine.formed() && MultiblockStructure.valid(machine));
    }
    @Override public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < ownSlots) { if (!moveItemStackTo(stack, ownSlots, slots.size(), true)) return ItemStack.EMPTY; }
        else {
            if(harvesting()&&!stack.is(Alone818.com.alone_mechanical.init.ModItems.FERTILIZER.get()))return ItemStack.EMPTY;
            ItemStack remaining = ItemHandlerHelper.insertItem(machine.itemChannel(0), stack.copy(), false);
            if(reactor&&!remaining.isEmpty())remaining=ItemHandlerHelper.insertItem(machine.itemChannel(1),remaining,false);
            if (remaining.getCount() == stack.getCount()) return ItemStack.EMPTY;
            stack.setCount(remaining.getCount());
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(p, stack); return original;
    }
}
