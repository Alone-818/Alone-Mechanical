package Alone818.com.alone_mechanical.menu;

import Alone818.com.alone_mechanical.blockentity.GeneratorBlockEntity;
import Alone818.com.alone_mechanical.init.ModMenus;
import Alone818.com.alone_mechanical.power.GeneratorKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import java.util.Objects;

public final class GeneratorMenu extends AbstractContainerMenu {
    public final GeneratorBlockEntity generator;
    public final boolean thermal;
    private final SyncedIntData data;
    public GeneratorMenu(int id,Inventory inv,FriendlyByteBuf buf) {
        this(id,inv,(GeneratorBlockEntity)Objects.requireNonNull(inv.player.level().getBlockEntity(buf.readBlockPos())));
    }
    public GeneratorMenu(int id,Inventory inv,GeneratorBlockEntity generator) {
        super(ModMenus.GENERATOR.get(),id);this.generator=generator;thermal=generator.kind()==GeneratorKind.THERMAL;
        data=new SyncedIntData(7,inv.player.level().isClientSide?null:i->switch(i) {
            case 0->generator.energy().getEnergyStored();case 1->generator.energy().getMaxEnergyStored();
            case 2->generator.generation();case 3->generator.remaining();case 4->generator.duration();case 5->generator.status();default->generator.kind().ordinal();
        });addDataSlots(data);
        if(thermal)addSlot(new SlotItemHandler(generator.fuel,0,28,55));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,126+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,184));
    }
    public int value(int index) { return data.value(index); }
    @Override public boolean stillValid(Player player) {
        return generator.getLevel()==player.level() && !generator.isRemoved() && player.distanceToSqr(generator.getBlockPos().getCenter())<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();int own=thermal?1:0;
        if(thermal && index==0) { if(!moveItemStackTo(stack,own,slots.size(),true))return ItemStack.EMPTY; }
        else if(thermal && generator.fuel.isItemValid(0,stack)) { if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY; }
        else if(index<own+27) { if(!moveItemStackTo(stack,own+27,slots.size(),false))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,own,own+27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
