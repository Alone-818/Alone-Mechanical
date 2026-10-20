package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.blockentity.ElectricPumpBlockEntity;
import Alone818.com.alone_mechanical.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import java.util.Objects;

public final class PumpMenu extends AbstractContainerMenu implements FluidMenuData.Access {
    public final ElectricPumpBlockEntity pump;
    private final SyncedIntData data;
    private final FluidMenuData fluidData;
    public PumpMenu(int id,Inventory inventory,FriendlyByteBuf buf){this(id,inventory,(ElectricPumpBlockEntity)Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));}
    public PumpMenu(int id,Inventory inventory,ElectricPumpBlockEntity pump){
        super(ModMenus.PUMP.get(),id);this.pump=pump;boolean client=inventory.player.level().isClientSide;
        fluidData=new FluidMenuData(inventory.player,1,i->pump.output());
        data=new SyncedIntData(10,client?null:i->switch(i){case 0->pump.energy().getEnergyStored();case 1->pump.capacity();case 2->pump.progress();case 3->ElectricPumpBlockEntity.CYCLE;case 4->pump.cost();case 5->pump.speed();case 6->pump.efficiency();case 7->pump.energyCoils();case 8->pump.matrices();default->pump.status();});addDataSlots(data);
        var upgrades=client?new ItemStackHandler(3):pump.upgrades;
        for(int slot=0;slot<3;slot++){int role=slot;addSlot(new SlotItemHandler(upgrades,slot,104+slot*22,75){
            @Override public boolean mayPlace(ItemStack stack){return ElectricPumpBlockEntity.accepts(role,stack);}
            @Override public int getMaxStackSize(){return ElectricPumpBlockEntity.limit(role);}
            // SlotItemHandler's default stack-size query temporarily clears the real handler.
            @Override public int getMaxStackSize(ItemStack stack){return mayPlace(stack)?Math.min(getMaxStackSize(),stack.getMaxStackSize()):0;}
            @Override public void setChanged(){super.setChanged();if(!client)pump.upgradesChanged();}
        });}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,8+x*18,121+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,179));
    }
    public int value(int index){return data.value(index);}
    @Override public FluidMenuData fluidData(){return fluidData;}
    @Override public void broadcastChanges(){super.broadcastChanges();fluidData.broadcast(containerId);}
    @Override public boolean stillValid(Player player){return !pump.isRemoved()&&player.level()==pump.getLevel()&&player.level().getBlockEntity(pump.getBlockPos())==pump&&player.distanceToSqr(pump.getBlockPos().getX()+.5,pump.getBlockPos().getY()+.5,pump.getBlockPos().getZ()+.5)<=100;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<3){if(!moveItemStackTo(stack,3,slots.size(),true))return ItemStack.EMPTY;}
        else{boolean moved=false;for(int upgrade=0;upgrade<3;upgrade++)if(ElectricPumpBlockEntity.accepts(upgrade,stack)){moved=moveItemStackTo(stack,upgrade,upgrade+1,false);break;}if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
