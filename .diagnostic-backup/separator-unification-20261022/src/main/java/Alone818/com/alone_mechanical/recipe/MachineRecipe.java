package Alone818.com.alone_mechanical.recipe;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModRecipes;
import Alone818.com.alone_mechanical.machine.*;
import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Two independent channels of each resource; the industrial core's component selects the family. */
public final class MachineRecipe implements Recipe<SimpleContainer> {
    public final ResourceLocation id, machine;
    public final Ingredient[] ingredients;
    public final int[] counts;
    public final ItemStack[] outputs;
    public final FluidStack[] fluids;
    public final GasStack[] gases;
    public final int ticks, energy;
    public final ConversionMode conversion;
    /** Optional exclusive alternative to item Output 1; each recipe independently selects either at 50%. */
    public final ItemStack alternateOutput;
    private SmeltingRecipe smeltingSource;
    public MachineRecipe(ResourceLocation id, ResourceLocation machine, Ingredient[] inputs, int[] counts,
                         ItemStack[] outputs, FluidStack[] fluids, GasStack[] gases, int ticks, int energy) {
        this(id,machine,inputs,counts,outputs,fluids,gases,ticks,energy,null);
    }
    public MachineRecipe(ResourceLocation id, ResourceLocation machine, Ingredient[] inputs, int[] counts,
                         ItemStack[] outputs, FluidStack[] fluids, GasStack[] gases, int ticks, int energy,ConversionMode conversion) {
        this(id,machine,inputs,counts,outputs,fluids,gases,ticks,energy,conversion,ItemStack.EMPTY);
    }
    public MachineRecipe(ResourceLocation id, ResourceLocation machine, Ingredient[] inputs, int[] counts,
                         ItemStack[] outputs, FluidStack[] fluids, GasStack[] gases, int ticks, int energy,ConversionMode conversion,ItemStack alternateOutput) {
        this.id = id; this.machine = machine; this.ingredients = inputs; this.counts = counts;
        this.outputs = outputs; this.fluids = fluids; this.gases = gases; this.ticks = ticks; this.energy = energy;
        this.conversion=conversion;
        this.alternateOutput=alternateOutput.copy();
    }
    public boolean matchesMachine(MachineBlockEntity m) {
        return matchesMachine(m,1);
    }
    public boolean matchesMachine(MachineBlockEntity m,int batches) {
        if(!safeBatch(batches))return false;
        if (!machine.equals(m.machineType())) return false;
        if(machine.equals(MachineDefinitions.REACTION_CORE)&&!validReactor())return false;
        if(MachineDefinitions.tower(machine)&&!validTower())return false;
        if(machine.equals(MachineDefinitions.DISTILLATION_TOWER)&&hasSecondOutput()&&!m.towerContents.hasSecondOutput())return false;
        if(!validIndustrial())return false;
        if(machine.equals(MachineDefinitions.ALLOY_SMELTER)&&!validAlloy())return false;
        if(machine.equals(MachineDefinitions.BOILER)&&!validBoiler())return false;
        if(machine.equals(MachineDefinitions.NUCLEAR_REACTOR)&&!validNuclear())return false;
        if(MachineDefinitions.separator(machine)!=null&&!validSeparator())return false;
        if(m.converter()&&conversion!=m.conversionMode())return false;
        return matchesInputs(m,batches);
    }
    public PartKind reactorInputKind(int channel){
        if(channel<0||channel>1)return null;
        boolean item=counts[channel]>0&&!ingredients[channel].isEmpty();
        boolean fluid=!fluids[channel].isEmpty(),gas=!gases[channel].isEmpty();
        if((item?1:0)+(fluid?1:0)+(gas?1:0)!=1)return null;
        return item?PartKind.ITEM_VAULT:fluid?PartKind.FLUID_TANK:PartKind.GAS_TANK;
    }
    public PartKind reactorOutputKind(){
        boolean item=!outputs[0].isEmpty(),fluid=!fluids[2].isEmpty(),gas=!gases[2].isEmpty();
        if((item?1:0)+(fluid?1:0)+(gas?1:0)!=1)return null;
        return item?PartKind.ITEM_VAULT:fluid?PartKind.FLUID_TANK:PartKind.GAS_TANK;
    }
    public boolean validReactor(){
        return ticks>0&&energy>=0&&alternateOutput.isEmpty()&&reactorInputKind(0)!=null&&reactorInputKind(1)!=null&&reactorOutputKind()!=null
            &&outputs[1].isEmpty()&&fluids[3].isEmpty()&&gases[3].isEmpty();
    }
    public boolean matchesReactorInputs(MachineBlockEntity m){
        if(!machine.equals(MachineDefinitions.REACTION_CORE)||!validReactor())return false;
        var first=m.reactorStore(0);var second=m.reactorStore(1);var expectedFirst=reactorInputKind(0);var expectedSecond=reactorInputKind(1);
        return ((first==expectedFirst&&second==expectedSecond)||(first==expectedSecond&&second==expectedFirst))&&matchesInputs(m,1);
    }
    public boolean hasSecondOutput(){return !outputs[1].isEmpty()||!fluids[3].isEmpty()||!gases[3].isEmpty();}
    public boolean matchesSeparatorInputs(MachineBlockEntity m){
        var profile=MachineDefinitions.separator(machine);
        return profile!=null&&validSeparator()&&m.reactorStore(0)==profile.input()&&matchesInputs(m,1);
    }
    private boolean matchesInputs(MachineBlockEntity m,int batches){
        if (smeltingSource != null) {
            int available=0;var input=m.itemChannel(0);
            for(int slot=0;slot<input.getSlots();slot++)if(smeltingInput(m,input.getStackInSlot(slot))) {
                available+=input.getStackInSlot(slot).getCount();if(available>=batches)return true;
            }
            return false;
        }
        for (int i = 0; i < 2; i++) {
            int inputChannel=m.recipeInputChannel(i,this);
            long available=0;var channel=m.itemChannel(inputChannel);
            for(int slot=0;slot<channel.getSlots();slot++)if(ingredients[i].test(channel.getStackInSlot(slot)))available+=channel.getStackInSlot(slot).getCount();
            if((long)counts[i]*batches>available)return false;
            var fluid=m.fluidChannel(inputChannel).getFluidInTank(0);var gas=m.gasChannel(inputChannel).contents();
            if (!fluids[i].isEmpty() && (!fluid.isFluidEqual(fluids[i]) || fluid.getAmount() < (long)fluids[i].getAmount()*batches)) return false;
            if (!gases[i].isEmpty() && (!gas.gas().equals(gases[i].gas()) || gas.amount() < (long)gases[i].amount()*batches)) return false;
        }
        return true;
    }
    public boolean outputFits(MachineBlockEntity m) {
        return outputFits(m,1);
    }
    public boolean outputFits(MachineBlockEntity m,int batches) {
        if(!safeBatch(batches))return false;
        if(machine.equals(MachineDefinitions.REACTION_CORE)&&!validReactor())return false;
        if(MachineDefinitions.tower(machine)&&!validTower())return false;
        if(!validIndustrial())return false;
        if(machine.equals(MachineDefinitions.ALLOY_SMELTER)&&!validAlloy())return false;
        if(machine.equals(MachineDefinitions.BOILER)&&!validBoiler())return false;
        if(machine.equals(MachineDefinitions.NUCLEAR_REACTOR)&&!validNuclear())return false;
        if(MachineDefinitions.separator(machine)!=null&&!validSeparator())return false;
        for (int i = 0; i < 2; i++) {
            int outputChannel=m.recipeOutputChannel(i,machine);
            if(i==0&&!alternateOutput.isEmpty()){
                if(!randomOutputFits(m,batches))return false;
            }else if (!net.minecraftforge.items.ItemHandlerHelper.insertItem(m.itemChannel(outputChannel),scaled(outputs[i],batches),true).isEmpty()) return false;
            var fluid=scaled(fluids[i+2],batches);var gas=gases[i+2].withAmount(gases[i+2].amount()*batches);
            if (!fluid.isEmpty() && m.fluidChannel(outputChannel).fill(fluid, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) != fluid.getAmount()) return false;
            if (!m.gasOutputFits(outputChannel,gas)) return false;
        }
        return true;
    }
    public void finish(MachineBlockEntity m) {
        finish(m,1);
    }
    /** Revalidate all inputs and outputs before touching the physical machine caches. */
    public boolean finish(MachineBlockEntity m,int batches) {
        if(!matchesMachine(m,batches) || !outputFits(m,batches))return false;
        if (smeltingSource != null) {
            int remaining=batches;var input=m.itemChannel(0);
            for(int slot=0;slot<input.getSlots() && remaining>0;slot++)if(smeltingInput(m,input.getStackInSlot(slot)))
                remaining-=input.extractItem(slot,remaining,false).getCount();
            net.minecraftforge.items.ItemHandlerHelper.insertItem(m.itemChannel(2),scaled(outputs[0],batches),false);
            return true;
        }
        for (int i = 0; i < 2; i++) {
            int inputChannel=m.recipeInputChannel(i,this);
            int outputChannel=m.recipeOutputChannel(i,machine);
            int remaining=counts[i]*batches;var channel=m.itemChannel(inputChannel);
            for(int slot=0;slot<channel.getSlots()&&remaining>0;slot++)if(ingredients[i].test(channel.getStackInSlot(slot)))remaining-=channel.extractItem(slot,remaining,false).getCount();
            if(i==0&&!alternateOutput.isEmpty()){
                int alternateBatches=0;
                for(int batch=0;batch<batches;batch++)if(m.getLevel().random.nextBoolean())alternateBatches++;
                net.minecraftforge.items.ItemHandlerHelper.insertItem(m.itemChannel(outputChannel),scaled(outputs[i],batches-alternateBatches),false);
                net.minecraftforge.items.ItemHandlerHelper.insertItem(m.itemChannel(outputChannel),scaled(alternateOutput,alternateBatches),false);
            }else net.minecraftforge.items.ItemHandlerHelper.insertItem(m.itemChannel(outputChannel),scaled(outputs[i],batches),false);
            m.fluidChannel(inputChannel).drain(scaled(fluids[i],batches), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            m.fluidChannel(outputChannel).fill(scaled(fluids[i + 2],batches), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            m.gasChannel(inputChannel).drain(gases[i].amount()*batches,false);
            m.produceGas(outputChannel,gases[i+2].withAmount(gases[i+2].amount()*batches));
        }
        return true;
    }
    private boolean randomOutputFits(MachineBlockEntity m,int batches){
        var bank=m.itemChannel(m.recipeOutputChannel(0,machine));
        if(batches==1)return net.minecraftforge.items.ItemHandlerHelper.insertItem(bank,outputs[0],true).isEmpty()
            &&net.minecraftforge.items.ItemHandlerHelper.insertItem(bank,alternateOutput,true).isEmpty();
        // Reserve room for either whole-batch outcome in a shared copy; no RNG is consumed by simulation.
        var copy=new net.minecraftforge.items.ItemStackHandler(bank.getSlots()){
            @Override public int getSlotLimit(int slot){return bank.getSlotLimit(slot);}
            @Override public boolean isItemValid(int slot,ItemStack stack){return bank.isItemValid(slot,stack);}
        };
        for(int slot=0;slot<bank.getSlots();slot++)copy.setStackInSlot(slot,bank.getStackInSlot(slot).copy());
        return net.minecraftforge.items.ItemHandlerHelper.insertItem(copy,scaled(outputs[0],batches),false).isEmpty()
            &&net.minecraftforge.items.ItemHandlerHelper.insertItem(copy,scaled(alternateOutput,batches),false).isEmpty();
    }
    private boolean safeBatch(int batches){
        if(batches<1||batches>MachineBlockEntity.MAX_PARALLEL)return false;
        if((long)alternateOutput.getCount()*batches>Integer.MAX_VALUE)return false;
        for(int i=0;i<2;i++)if((long)counts[i]*batches>Integer.MAX_VALUE||(long)outputs[i].getCount()*batches>Integer.MAX_VALUE)return false;
        for(int i=0;i<4;i++)if((long)fluids[i].getAmount()*batches>Integer.MAX_VALUE||(long)gases[i].amount()*batches>Integer.MAX_VALUE)return false;
        return true;
    }
    private static ItemStack scaled(ItemStack stack,int batches) {return stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()*batches);}
    private boolean validIndustrial(){
        if(!alternateOutput.isEmpty()&&(!machine.equals(MachineDefinitions.INFUSER)||alternateOutput.getCount()!=outputs[0].getCount()
            ||ItemStack.isSameItemSameTags(alternateOutput,outputs[0])))return false;
        if(!machine.equals(MachineDefinitions.FORMING_MACHINE)&&!machine.equals(MachineDefinitions.INFUSER))return true;
        if(counts[0]<=0||ingredients[0].isEmpty()||counts[1]!=0||!ingredients[1].isEmpty()||outputs[0].isEmpty()||!outputs[1].isEmpty()
            ||!fluids[0].isEmpty()||!gases[0].isEmpty()||!fluids[2].isEmpty()||!fluids[3].isEmpty()||!gases[2].isEmpty()||!gases[3].isEmpty())return false;
        int auxiliary=(!fluids[1].isEmpty()?1:0)+(!gases[1].isEmpty()?1:0);
        return auxiliary==(machine.equals(MachineDefinitions.INFUSER)?1:0);
    }
    private boolean validBoiler(){
        if(ticks<=0||energy<0)return false;
        for(int i=0;i<2;i++)if(counts[i]!=0||!ingredients[i].isEmpty()||!outputs[i].isEmpty())return false;
        if(fluids[0].isEmpty()||fluids[1].isEmpty()||!gases[0].isEmpty()||!gases[1].isEmpty())return false;
        for(int i=0;i<4;i++){
            boolean fluid=!fluids[i].isEmpty(), gas=!gases[i].isEmpty();
            if((fluid?1:0)+(gas?1:0)!=1)return false;
        }
        return true;
    }
    private boolean validNuclear(){
        return validBoiler()&&energy==0&&!fluids[2].isEmpty()&&gases[2].isEmpty();
    }
    private boolean validTower(){
        for(int i=0;i<2;i++)if(counts[i]!=0||!ingredients[i].isEmpty()||!outputs[i].isEmpty())return false;
        if(!alternateOutput.isEmpty()||ticks<=0||energy<=0)return false;
        if(machine.equals(MachineDefinitions.AIR_SEPARATION_TOWER))return java.util.Arrays.stream(fluids).allMatch(FluidStack::isEmpty)&&gases[0].isEmpty()&&gases[1].isEmpty()&&!gases[2].isEmpty()&&!gases[3].isEmpty();
        boolean exchange=machine.equals(MachineDefinitions.EXCHANGE_TOWER);
        for(int i=0;i<4;i++){
            int phases=(fluids[i].isEmpty()?0:1)+(gases[i].isEmpty()?0:1);
            if(exchange&&phases!=1)return false;
            if(!exchange&&(i==0||i==2)&&phases!=1)return false;
            if(!exchange&&(i==1&&phases!=0||i==3&&phases>1))return false;
        }
        if(machine.equals(MachineDefinitions.DISTILLATION_TOWER))return !fluids[0].isEmpty();
        if(machine.equals(MachineDefinitions.CONDENSATION_TOWER))return !gases[0].isEmpty()&&!fluids[2].isEmpty()&&fluids[3].isEmpty()&&gases[3].isEmpty();
        return exchange;
    }
    private boolean validAlloy() {
        int auxiliaries=(counts[1]>0&&!ingredients[1].isEmpty()?1:0)+(!fluids[1].isEmpty()?1:0)+(!gases[1].isEmpty()?1:0);
        return counts[0]>0&&!ingredients[0].isEmpty()&&auxiliaries==1&&!outputs[0].isEmpty()&&outputs[1].isEmpty()
            &&fluids[0].isEmpty()&&gases[0].isEmpty()&&fluids[2].isEmpty()&&fluids[3].isEmpty()&&gases[2].isEmpty()&&gases[3].isEmpty();
    }
    private boolean validSeparator(){
        var profile=MachineDefinitions.separator(machine);if(profile==null)return false;
        for(int channel=0;channel<4;channel++){
            boolean item=channel<2?counts[channel]>0&&!ingredients[channel].isEmpty():!outputs[channel-2].isEmpty();
            boolean fluid=!fluids[channel].isEmpty(),gas=!gases[channel].isEmpty();var kind=profile.kind(channel);
            if((item?1:0)+(fluid?1:0)+(gas?1:0)!=(channel==1?0:1))return false;
            if(kind==PartKind.ITEM_VAULT&&!item||kind==PartKind.FLUID_TANK&&!fluid||kind==PartKind.GAS_TANK&&!gas)return false;
        }
        return true;
    }
    private static FluidStack scaled(FluidStack stack,int batches) {
        if(stack.isEmpty())return FluidStack.EMPTY;
        var copy=stack.copy();copy.setAmount(stack.getAmount()*batches);return copy;
    }
    private boolean smeltingInput(MachineBlockEntity m,ItemStack input) {
        if(input.isEmpty() || !smeltingSource.matches(new SimpleContainer(input.copy()),m.getLevel()))return false;
        var output=smeltingSource.assemble(new SimpleContainer(input.copy()),m.getLevel().registryAccess());
        return ItemStack.isSameItemSameTags(output,outputs[0]) && output.getCount()==outputs[0].getCount();
    }
    public static MachineRecipe fromSmelting(SmeltingRecipe source, ItemStack input, net.minecraft.server.level.ServerLevel level) {
        ItemStack output=source.assemble(new SimpleContainer(input.copy()),level.registryAccess());
        if(output.isEmpty())return null;
        var adapted=new MachineRecipe(new ResourceLocation(source.getId().getNamespace(),"arc_furnace/"+source.getId().getPath()),
            new ResourceLocation("alone_mechanical","arc_furnace"),
            new Ingredient[]{source.getIngredients().get(0),Ingredient.EMPTY},new int[]{1,0},
            new ItemStack[]{output.copy(),ItemStack.EMPTY},
            new FluidStack[]{FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY},
            new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},
            Math.max(1,Math.min(1000000,source.getCookingTime())),Alone818.com.alone_mechanical.Config.ARC_FURNACE_ENERGY.get());
        adapted.smeltingSource=source;return adapted;
    }
    @Override public boolean matches(SimpleContainer c, Level l) {
        for (int i = 0; i < 2; i++) if (counts[i] > 0 && (!ingredients[i].test(c.getItem(i)) || c.getItem(i).getCount() < counts[i])) return false;
        return true;
    }
    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return outputs[0].copy(); }
    @Override public boolean canCraftInDimensions(int x, int y) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return outputs[0].copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.MACHINE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.MACHINE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        @Override public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient[] inputs = {Ingredient.EMPTY, Ingredient.EMPTY}; int[] counts = {0, 0};
            ItemStack[] outputs = {ItemStack.EMPTY, ItemStack.EMPTY};
            FluidStack[] fluids = {FluidStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY};
            GasStack[] gases = {GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY, GasStack.EMPTY};
            JsonArray in = array(json, "item_inputs"), out = array(json, "item_outputs");
            for (int i = 0; i < in.size(); i++) { if(in.get(i).isJsonNull())continue;JsonObject obj = in.get(i).getAsJsonObject();if(obj.size()==0)continue; inputs[i] = Ingredient.fromJson(obj.get("ingredient")); counts[i] = positive(obj, "count", 1, 64); }
            for (int i = 0; i < out.size(); i++) { if(out.get(i).isJsonNull()||out.get(i).getAsJsonObject().size()==0)continue;outputs[i] = ShapedRecipe.itemStackFromJson(out.get(i).getAsJsonObject()); if (outputs[i].isEmpty() || outputs[i].getCount() > outputs[i].getMaxStackSize()) throw new JsonSyntaxException("Invalid machine output"); }
            readFluids(array(json, "fluid_inputs"), fluids, 0); readFluids(array(json, "fluid_outputs"), fluids, 2);
            readGases(array(json, "gas_inputs"), gases, 0); readGases(array(json, "gas_outputs"), gases, 2);
            var machine=new ResourceLocation(GsonHelper.getAsString(json,"machine"));ConversionMode conversion=null;
            if (!machine.equals(MachineDefinitions.AIR_SEPARATION_TOWER)&&in.isEmpty() && fluids[0].isEmpty() && fluids[1].isEmpty() && gases[0].isEmpty() && gases[1].isEmpty()) throw new JsonSyntaxException("Machine recipe needs an input");
            if(machine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER)){
                try{conversion=ConversionMode.parse(GsonHelper.getAsString(json,"conversion"));}catch(IllegalArgumentException e){throw new JsonSyntaxException(e.getMessage(),e);}
            }
            int energy=(machine.equals(MachineDefinitions.BOILER)||machine.equals(MachineDefinitions.NUCLEAR_REACTOR))?GsonHelper.getAsInt(json,"energy_per_tick",0):positive(json,"energy_per_tick",20,1000000);
            if(energy<0||energy>1000000)throw new JsonSyntaxException("Invalid energy_per_tick");
            var alternate=json.has("alternate_output")?ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"alternate_output")):ItemStack.EMPTY;
            if(json.has("alternate_output")&&(alternate.isEmpty()||alternate.getCount()>alternate.getMaxStackSize()))throw new JsonSyntaxException("Invalid alternate output");
            var recipe=new MachineRecipe(id,machine,inputs,counts,outputs,fluids,gases,positive(json,"ticks",100,1000000),energy,conversion,alternate);
            validateConversion(recipe);validateAlloy(recipe);validateSeparator(recipe);validateIndustrial(recipe);validateReactor(recipe);if(recipe.machine.equals(MachineDefinitions.BOILER)&&!recipe.validBoiler())throw new JsonSyntaxException("Boiler needs two fluid/gas inputs and two fluid/gas outputs");if(recipe.machine.equals(MachineDefinitions.NUCLEAR_REACTOR)&&!recipe.validNuclear())throw new JsonSyntaxException("Nuclear reactor requires two liquid inputs, liquid waste Output 1 and a heated fluid/gas Output 2, with zero FE cost");return recipe;
        }
        private static void validateIndustrial(MachineRecipe recipe){
            if(MachineDefinitions.tower(recipe.machine)&&!recipe.validTower())throw new JsonSyntaxException("Distillation: liquid Input 1 and one/two fluid/gas outputs; condensation: gas Input 1 to liquid Output 1; exchange: two independent fluid/gas inputs and outputs. No items or mixed phases per channel.");
            if(!recipe.validIndustrial())throw new JsonSyntaxException("Forming needs item Input 1 -> item Output 1 only; infusion additionally needs exactly one fluid OR gas on Input 2");
        }
        private static void validateReactor(MachineRecipe recipe){
            if(recipe.machine.equals(MachineDefinitions.REACTION_CORE)&&!recipe.validReactor())throw new JsonSyntaxException("Biphasic reactor needs one item/fluid/gas medium for each of two inputs and one output, with no secondary output");
        }
        private static void validateSeparator(MachineRecipe recipe){
            if(recipe.machine.getPath().startsWith("separator_")&&recipe.machine.getNamespace().equals("alone_mechanical")&&(MachineDefinitions.separator(recipe.machine)==null||!recipe.validSeparator()))
                throw new JsonSyntaxException("Separator recipe needs one Input 1, no Input 2, and exactly one product on each of Output 1 and Output 2 matching its storage profile");
        }
        private static void validateAlloy(MachineRecipe recipe) {
            if(recipe.machine.equals(MachineDefinitions.ALLOY_SMELTER)&&!recipe.validAlloy())
                throw new JsonSyntaxException("Alloy smelting needs item Input 1, exactly one item/fluid/gas Input 2 and one item Output 1");
        }
        private static void validateConversion(MachineRecipe r){
            if(!r.machine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER))return;
            boolean gasInput=r.conversion==ConversionMode.GAS_TO_LIQUID;
            if(r.conversion==null||r.counts[0]!=0||r.counts[1]!=0||!r.outputs[0].isEmpty()||!r.outputs[1].isEmpty()
                ||!r.fluids[1].isEmpty()||!r.fluids[3].isEmpty()||!r.gases[1].isEmpty()||!r.gases[3].isEmpty()
                ||(gasInput?(!r.fluids[0].isEmpty()||!r.gases[2].isEmpty()||r.gases[0].isEmpty()||r.fluids[2].isEmpty()):(!r.gases[0].isEmpty()||!r.fluids[2].isEmpty()||r.fluids[0].isEmpty()||r.gases[2].isEmpty())))
                throw new JsonSyntaxException("Converter recipe requires exactly one input and one opposite-phase output matching conversion");
        }
        private static int positive(JsonObject o, String key, int fallback, int max) {
            int n = GsonHelper.getAsInt(o, key, fallback); if (n <= 0 || n > max) throw new JsonSyntaxException("Invalid " + key); return n;
        }
        private static JsonArray array(JsonObject json, String key) {
            JsonArray a = json.has(key) ? GsonHelper.getAsJsonArray(json, key) : new JsonArray();
            if (a.size() > 2) throw new JsonSyntaxException(key + " supports two channels"); return a;
        }
        private static void readFluids(JsonArray a, FluidStack[] target, int offset) {
            for (int i = 0; i < a.size(); i++) {
                if(a.get(i).isJsonNull())continue;JsonObject o = a.get(i).getAsJsonObject();if(o.size()==0)continue; ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(o, "fluid"));
                if (!ForgeRegistries.FLUIDS.containsKey(id) || id.equals(new ResourceLocation("minecraft", "empty"))) throw new JsonSyntaxException("Unknown fluid " + id);
                target[i + offset] = new FluidStack(ForgeRegistries.FLUIDS.getValue(id), positive(o, "amount", 1000, MachineBlockEntity.TANK_CAPACITY));
            }
        }
        private static void readGases(JsonArray a, GasStack[] target, int offset) {
            for (int i = 0; i < a.size(); i++) {
                if(a.get(i).isJsonNull())continue;JsonObject o = a.get(i).getAsJsonObject();if(o.size()==0)continue; target[i + offset] = new GasStack(new ResourceLocation(GsonHelper.getAsString(o, "gas")), positive(o, "amount", 1000, MachineBlockEntity.TANK_CAPACITY));
            }
        }
        @Override public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf b) {
            ResourceLocation machine = b.readResourceLocation(); int ticks = b.readVarInt(), energy = b.readVarInt();
            Ingredient[] in = new Ingredient[2]; int[] counts = new int[2]; ItemStack[] out = new ItemStack[2]; FluidStack[] f = new FluidStack[4]; GasStack[] g = new GasStack[4];
            for (int i = 0; i < 2; i++) { in[i] = Ingredient.fromNetwork(b); counts[i] = b.readVarInt(); out[i] = b.readItem(); }
            for (int i = 0; i < 4; i++) { f[i] = b.readFluidStack(); g[i] = new GasStack(b.readResourceLocation(), b.readVarInt()); }
            ConversionMode conversion=machine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER)?b.readEnum(ConversionMode.class):null;
            var alternate=b.readItem();
            var recipe=new MachineRecipe(id, machine, in, counts, out, f, g, ticks, energy,conversion,alternate);validateConversion(recipe);validateAlloy(recipe);validateSeparator(recipe);validateIndustrial(recipe);validateReactor(recipe);if(recipe.machine.equals(MachineDefinitions.BOILER)&&!recipe.validBoiler())throw new JsonSyntaxException("Boiler needs two fluid/gas inputs and two fluid/gas outputs");if(recipe.machine.equals(MachineDefinitions.NUCLEAR_REACTOR)&&!recipe.validNuclear())throw new JsonSyntaxException("Nuclear reactor requires two liquid inputs, liquid waste Output 1 and a heated fluid/gas Output 2, with zero FE cost");return recipe;
        }
        @Override public void toNetwork(FriendlyByteBuf b, MachineRecipe r) {
            b.writeResourceLocation(r.machine); b.writeVarInt(r.ticks); b.writeVarInt(r.energy);
            for (int i = 0; i < 2; i++) { r.ingredients[i].toNetwork(b); b.writeVarInt(r.counts[i]); b.writeItem(r.outputs[i]); }
            for (int i = 0; i < 4; i++) { b.writeFluidStack(r.fluids[i]); b.writeResourceLocation(r.gases[i].gas()); b.writeVarInt(r.gases[i].amount()); }
            if(r.machine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER))b.writeEnum(r.conversion);
            b.writeItem(r.alternateOutput);
        }
    }
}
