package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.init.ModMenus;
import Alone818.com.alone_mechanical.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class EnergyMatrixMenu extends AbstractContainerMenu {
    public final UUID matrixId;
    public final BlockPos anchor;
    private final Player owner;
    private final net.minecraft.world.level.Level originalLevel;
    private final SyncedLongData data;
    public EnergyMatrixMenu(int id,Inventory inventory,FriendlyByteBuf buf){this(id,inventory,buf.readUUID(),buf.readBlockPos());}
    public EnergyMatrixMenu(int id,Inventory inventory,UUID matrixId,BlockPos anchor){
        super(ModMenus.ENERGY_MATRIX.get(),id);this.matrixId=matrixId;this.anchor=anchor.immutable();owner=inventory.player;originalLevel=owner.level();
        data=new SyncedLongData(15,owner.level().isClientSide?null:this::read);addDataSlots(data);
    }
    private long read(int index){
        if(!(owner.level() instanceof ServerLevel level))return 0;
        var saved=EnergyMatrixData.get(level);var bank=saved.byId(matrixId);if(!saved.active(level,bank))return 0;
        long now=level.getGameTime();var core=PowerGridData.get(level).connectedCore(level,bank.center());
        return switch(index){
            case 0->bank.energy;case 1->bank.capacity();case 2->bank.cells;case 3->bank.mode.ordinal();
            case 4->bank.flow.input(now);case 5->bank.flow.output(now);case 6->core==null?0:1;
            case 7->core==null?0:core.externalFlow().input(now)+core.matrixFlow().input(now);
            case 8->core==null?0:core.externalFlow().output(now)+core.matrixFlow().output(now);
            case 9->core==null?0:core.externalFlow().input(now);case 10->core==null?0:core.externalFlow().output(now);
            case 11->core==null?0:core.matrixFlow().input(now);case 12->core==null?0:core.matrixFlow().output(now);
            case 13->core==null?0:core.energy().getEnergyStored();default->bank.rate();
        };
    }
    public long value(int index){return data.value(index);}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button<0||button>=MatrixMode.values().length||player!=owner||player.containerMenu!=this||!stillValid(player)||!(player.level() instanceof ServerLevel level))return false;
        boolean changed=EnergyMatrixData.get(level).mode(level,matrixId,MatrixMode.values()[button]);if(changed)broadcastChanges();return changed;
    }
    @Override public boolean stillValid(Player player){
        if(player!=owner||player.level()!=originalLevel||player.distanceToSqr(anchor.getX()+.5,anchor.getY()+.5,anchor.getZ()+.5)>100)return false;
        if(!(player.level() instanceof ServerLevel level))return true;
        var saved=EnergyMatrixData.get(level);var bank=saved.at(anchor);return bank!=null&&bank.id.equals(matrixId)&&saved.active(level,bank)&&!level.getBlockState(anchor).isAir();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
