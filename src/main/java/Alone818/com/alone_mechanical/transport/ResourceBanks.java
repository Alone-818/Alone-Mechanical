package Alone818.com.alone_mechanical.transport;
import Alone818.com.alone_mechanical.gas.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;
import java.util.function.Supplier;

/** Live views over physical segments; rebuilding or splitting never copies stored resources. */
public final class ResourceBanks {
    public static IItemHandlerModifiable items(Supplier<List<IItemHandlerModifiable>> sources) {
        return new IItemHandlerModifiable() {
            private IItemHandlerModifiable handler(int[] slot) {
                if (slot[0] < 0) return null;
                for (var h : sources.get()) { if (slot[0] < h.getSlots()) return h; slot[0] -= h.getSlots(); }
                return null;
            }
            public int getSlots() { return sources.get().stream().mapToInt(IItemHandler::getSlots).sum(); }
            public ItemStack getStackInSlot(int s) { int[] i={s}; var h=handler(i); return h==null?ItemStack.EMPTY:h.getStackInSlot(i[0]); }
            public void setStackInSlot(int s,ItemStack v) { int[] i={s}; var h=handler(i); if(h!=null)h.setStackInSlot(i[0],v); }
            public ItemStack insertItem(int s,ItemStack v,boolean sim) { int[] i={s}; var h=handler(i); return h==null?v:h.insertItem(i[0],v,sim); }
            public ItemStack extractItem(int s,int n,boolean sim) { int[] i={s}; var h=handler(i); return h==null?ItemStack.EMPTY:h.extractItem(i[0],n,sim); }
            public int getSlotLimit(int s) { int[] i={s}; var h=handler(i); return h==null?0:h.getSlotLimit(i[0]); }
            public boolean isItemValid(int s,ItemStack v) { int[] i={s}; var h=handler(i); return h!=null&&h.isItemValid(i[0],v); }
        };
    }
    public static FluidStack fluidContents(IFluidHandler h) {
        FluidStack out=FluidStack.EMPTY;
        for(int i=0;i<h.getTanks();i++){ var f=h.getFluidInTank(i); if(f.isEmpty())continue; if(out.isEmpty())out=f.copy(); else if(out.isFluidEqual(f))out.grow(f.getAmount()); }
        return out;
    }
    public static IFluidHandler fluids(Supplier<List<IFluidHandler>> sources) {
        return new IFluidHandler() {
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int i){FluidStack out=FluidStack.EMPTY;for(var h:sources.get()){var f=fluidContents(h);if(f.isEmpty())continue;if(out.isEmpty())out=f.copy();else if(out.isFluidEqual(f))out.grow(f.getAmount());}return out;}
            public int getTankCapacity(int i){int n=0;for(var h:sources.get())for(int t=0;t<h.getTanks();t++)n+=h.getTankCapacity(t);return n;}
            public boolean isFluidValid(int i,FluidStack f){var current=getFluidInTank(0);return current.isEmpty()||current.isFluidEqual(f);}
            public int fill(FluidStack f,FluidAction action){if(f.isEmpty()||!isFluidValid(0,f))return 0;int n=0;for(var h:sources.get()){var offer=f.copy();offer.setAmount(f.getAmount()-n);n+=h.fill(offer,action);if(n==f.getAmount())break;}return n;}
            public FluidStack drain(FluidStack f,FluidAction action){if(f.isEmpty())return FluidStack.EMPTY;int n=0;for(var h:sources.get()){var request=f.copy();request.setAmount(f.getAmount()-n);n+=h.drain(request,action).getAmount();if(n==f.getAmount())break;}var out=f.copy();out.setAmount(n);return n==0?FluidStack.EMPTY:out;}
            public FluidStack drain(int n,FluidAction action){var request=getFluidInTank(0);if(n<=0||request.isEmpty())return FluidStack.EMPTY;request.setAmount(Math.min(n,request.getAmount()));return drain(request,action);}
        };
    }
    public static IGasHandler gases(Supplier<List<IGasHandler>> sources) {
        return new IGasHandler(){
            public GasStack contents(){GasStack out=GasStack.EMPTY;for(var h:sources.get()){var g=h.contents();if(g.isEmpty())continue;if(out.isEmpty())out=g;else if(out.gas().equals(g.gas()))out=out.withAmount(out.amount()+g.amount());}return out;}
            public int capacity(){return sources.get().stream().mapToInt(IGasHandler::capacity).sum();}
            public int fill(GasStack g,boolean sim){var current=contents();if(g.isEmpty()||!current.isEmpty()&&!current.gas().equals(g.gas()))return 0;int n=0;for(var h:sources.get()){n+=h.fill(g.withAmount(g.amount()-n),sim);if(n==g.amount())break;}return n;}
            public GasStack drain(int n,boolean sim){var current=contents();int amount=0;for(var h:sources.get()){if(!h.contents().gas().equals(current.gas()))continue;amount+=h.drain(Math.max(0,n-amount),sim).amount();if(amount>=n)break;}return current.withAmount(amount);}
        };
    }
    private ResourceBanks(){}
}
