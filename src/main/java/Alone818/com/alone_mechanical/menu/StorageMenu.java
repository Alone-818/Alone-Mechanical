package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.init.ModMenus;
import Alone818.com.alone_mechanical.machine.PartKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import java.util.Objects;

public class StorageMenu extends AbstractContainerMenu implements FluidMenuData.Access,GasMenuData.Access {
    public final StorageBlockEntity storage;
    private final SyncedIntData data;
    private int page;
    private final int ownSlots;
    public final int rows;
    private final FluidMenuData fluidData;
    private final GasMenuData gasData;
    public StorageMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,(StorageBlockEntity)Objects.requireNonNull(inv.player.level().getBlockEntity(b.readBlockPos())),b.readableBytes()>0?b.readVarInt():3);}
    public StorageMenu(int id,Inventory inv,StorageBlockEntity storage){this(id,inv,storage,storage.vaultRows());}
    private StorageMenu(int id,Inventory inv,StorageBlockEntity storage,int rows){
        super(ModMenus.STORAGE.get(),id);this.storage=storage;this.rows=rows==6||rows==9?6:3;ownSlots=storage.isTank()?0:this.rows*9;
        fluidData=new FluidMenuData(inv.player,1,i->storage.fluids());
        gasData=new GasMenuData(inv.player,1,i->storage.gases());
        data=new SyncedIntData(7,inv.player.level().isClientSide?null:i->switch(i){
            case 0->storage.kind().ordinal();case 1->{if(storage.kind()==PartKind.FLUID_TANK)yield storage.fluids().getFluidInTank(0).getAmount();if(storage.kind()==PartKind.GAS_TANK)yield storage.gases().contents().amount();int n=0;var items=storage.items();for(int s=0;s<items.getSlots();s++)n+=items.getStackInSlot(s).getCount();yield n;}
            case 2->storage.kind()==PartKind.ITEM_VAULT?storage.items().getSlots():storage.kind()==PartKind.FLUID_TANK?storage.fluids().getTankCapacity(0):storage.gases().capacity();
            case 3->storage.column().size();case 4->storage.mode().ordinal();case 5->page;default->Math.max(1,(storage.items().getSlots()+Math.max(1,ownSlots)-1)/Math.max(1,ownSlots));
        });addDataSlots(data);
        var paged=MenuItemPage.create(inv.player.level().isClientSide,storage::items,()->page,Math.max(1,ownSlots));
        for(int y=0;y<this.rows&&ownSlots>0;y++)for(int x=0;x<9;x++)addSlot(new SlotItemHandler(paged,x+y*9,8+x*18,59+y*18));
        int bottom=ownSlots>0?96+this.rows*18:116;
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,8+x*18,bottom+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,bottom+58));
    }
    public int value(int i){return data.value(i);}
    @Override public FluidMenuData fluidData(){return fluidData;}
    @Override public GasMenuData gasData(){return gasData;}
    @Override public void broadcastChanges(){super.broadcastChanges();fluidData.broadcast(containerId);gasData.broadcast(containerId);}
    public int pageSize(){return ownSlots;}
    public int controlsY(){return 62+rows*18;}
    public int guiHeight(){return ownSlots>0?178+rows*18:200;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(!stillValid(p)||p.level().isClientSide||button<0||button>3||ownSlots==0)return false;
        if(button>=2){
            if(!(p instanceof net.minecraft.server.level.ServerPlayer server)||server instanceof net.minecraftforge.common.util.FakePlayer||server.connection==null)return false;
            int newRows=(button-1)*3;if(newRows==rows)return true;
            int firstIndex=page*ownSlots;var carried=getCarried().copy();setCarried(ItemStack.EMPTY);storage.setVaultRows(newRows);
            net.minecraftforge.network.NetworkHooks.openScreen(server,storage,buf->{buf.writeBlockPos(storage.getBlockPos());buf.writeVarInt(newRows);});
            if(server.containerMenu instanceof StorageMenu next){next.page=Math.min(Math.max(0,next.value(6)-1),firstIndex/next.ownSlots);next.setCarried(carried);next.broadcastChanges();}
            return true;
        }
        page=Math.max(0,Math.min(value(6)-1,page+(button==0?-1:1)));broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player p){return !storage.isRemoved()&&p.level()==storage.getLevel()&&p.distanceToSqr(storage.getBlockPos().getX()+.5,storage.getBlockPos().getY()+.5,storage.getBlockPos().getZ()+.5)<=100;}
    @Override public ItemStack quickMoveStack(Player p,int i){Slot slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(i<ownSlots){if(!moveItemStackTo(stack,ownSlots,slots.size(),true))return ItemStack.EMPTY;}else if(ownSlots==0||!moveItemStackTo(stack,0,ownSlots,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;}
}
