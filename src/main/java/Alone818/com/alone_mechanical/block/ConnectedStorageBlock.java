package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Only segments sharing one physical vertical column share a continuous frame. */
public final class ConnectedStorageBlock extends MachinePartBlock {
    public ConnectedStorageBlock(PartKind kind,Properties properties){super(kind,properties);}
    @Override public void setPlacedBy(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide&&kind.isTank()&&level.getBlockEntity(pos) instanceof StorageBlockEntity storage){
            var fluid=Alone818.com.alone_mechanical.item.PortableTankItem.contents(stack).copy();
            if(!fluid.isEmpty())fluid.setAmount(Math.min(fluid.getAmount(),StorageBlockEntity.CAPACITY));
            // Restore this physical segment before rebuilding content-based vertical partitions.
            storage.tank.setFluid(fluid);storage.sync();
        }
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var drops=super.getDrops(state,builder);
        if(kind.isTank()&&builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof StorageBlockEntity storage)
            for(var stack:drops)if(stack.getItem() instanceof Alone818.com.alone_mechanical.item.PortableTankItem)
                Alone818.com.alone_mechanical.item.PortableTankItem.storeFluid(stack,storage.tank.getFluid());
        return drops;
    }
    public static boolean sameFamily(BlockState a,BlockState b){
        return a.getBlock() instanceof ConnectedStorageBlock first&&b.getBlock() instanceof ConnectedStorageBlock second
            &&(first.kind.isTank()&&second.kind.isTank()||first.kind==PartKind.ITEM_VAULT&&second.kind==PartKind.ITEM_VAULT);
    }
    @Override public int getLightBlock(BlockState state,BlockGetter world,BlockPos pos){return 0;}
    public static boolean joins(BlockGetter world,BlockPos pos,Direction direction){
        if(!direction.getAxis().isVertical())return false;
        var neighbor=pos.relative(direction);
        if(!sameFamily(world.getBlockState(pos),world.getBlockState(neighbor)))return false;
        if(!(world.getBlockEntity(pos) instanceof StorageBlockEntity first)||!(world.getBlockEntity(neighbor) instanceof StorageBlockEntity second))return false;
        if(!first.isTank())return true;
        var lower=direction==Direction.UP?pos:neighbor;
        var upper=direction==Direction.UP?second:first;
        String upperContent=upper.storedContentKey();
        if(upperContent==null)return true;
        // Match the bottom-up content partitions used by StorageBlockEntity.column(),
        // using only the supplied render-region snapshot, never loading world chunks.
        for(var p=lower;p.getY()>=world.getMinBuildHeight();p=p.below()){
            if(!sameFamily(world.getBlockState(pos),world.getBlockState(p))||!(world.getBlockEntity(p) instanceof StorageBlockEntity storage))break;
            String content=storage.storedContentKey();if(content!=null)return content.equals(upperContent);
        }
        return true;
    }
    /** Side bits: 1 joins above, 2 joins below. End caps always retain their own frame. */
    public int connectionMask(BlockGetter world,BlockPos pos,Direction face){
        return face.getAxis().isVertical()?0:(joins(world,pos,Direction.UP)?1:0)|(joins(world,pos,Direction.DOWN)?2:0);
    }
}
