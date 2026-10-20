package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.machine.FieldAutomation;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.network.FieldConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** A menu-bound editor: edits are addressed by registry ID and acknowledged by the server. */
public final class FieldConfigScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int ROWS=6, TEXT=0xffe4edf1, MUTED=0xff93a8b6, ACCENT=0xff63c6b4;
    private final Map<ResourceLocation,FieldConfigSync.Entry> entries=new LinkedHashMap<>();
    private final Map<ResourceLocation,String> drafts=new HashMap<>();
    private final Map<ResourceLocation,FieldConfigSync.Update> pending=new HashMap<>();
    private final List<Row> rows=new ArrayList<>();
    private EditBox search;
    private Button filter,previous,next;
    private boolean nearby=true,received,refreshing;
    private int page,ticks;
    private String query="";
    public FieldConfigScreen(MachineMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=358;imageHeight=232;}
    private static Component tr(String key,Object...args){return Component.translatable("gui.alone_mechanical."+key,args);}
    public static void receive(FieldConfigSync.Snapshot snapshot){
        if(Minecraft.getInstance().screen instanceof FieldConfigScreen screen&&screen.menu.containerId==snapshot.menuId())screen.accept(snapshot);
    }
    private void accept(FieldConfigSync.Snapshot snapshot){
        entries.clear();for(var entry:snapshot.entries()){
            entries.put(entry.type(),entry);var update=pending.get(entry.type());
            if(update!=null&&entry.limit()==update.count()&&entry.enabled()==update.enabled())pending.remove(entry.type());
        }
        received=true;refreshRows();
    }
    @Override protected void init(){
        super.init();menu.fieldConfiguration=true;menu.configurationView=true;
        search=addRenderableWidget(new EditBox(font,leftPos+10,topPos+32,193,18,tr("field_search")));
        search.setMaxLength(100);search.setHint(tr("field_search"));search.setValue(query);
        search.setResponder(value->{commitAll();query=value;page=0;rebuildRows();});
        filter=button(211,32,137,18,tr(nearby?"field_nearby":"field_all"),()->{commitAll();nearby=!nearby;page=0;rebuildRows();});
        button(296,7,52,18,tr("field_back"),this::onClose);
        previous=button(10,194,20,16,Component.literal("‹"),()->changePage(-1));
        next=button(112,194,20,16,Component.literal("›"),()->changePage(1));
        button(270,194,78,16,tr("field_apply"),this::commitAll);
        rebuildRows();request();
    }
    private Button button(int x,int y,int width,int height,Component label,Runnable action){return addRenderableWidget(Button.builder(label,b->action.run()).bounds(leftPos+x,topPos+y,width,height).build());}
    private void request(){FieldConfigSync.CHANNEL.sendToServer(new FieldConfigSync.Request(menu.containerId));}
    private Component name(ResourceLocation id){var type=ForgeRegistries.ENTITY_TYPES.getValue(id);return type==null?Component.literal(id.toString()):type.getDescription();}
    private List<FieldConfigSync.Entry> filtered(){
        String term=query.strip().toLowerCase(Locale.ROOT);
        var exact=ResourceLocation.tryParse(term);boolean exactId=term.contains(":")&&ForgeRegistries.ENTITY_TYPES.containsKey(exact);
        return entries.values().stream().filter(e->exactId?e.type().equals(exact):term.isEmpty()?(!nearby||e.count()>0||e.configured()):name(e.type()).getString().toLowerCase(Locale.ROOT).contains(term)||e.type().toString().contains(term))
            .sorted(Comparator.<FieldConfigSync.Entry>comparingInt(e->e.count()>0?0:1).thenComparing(e->name(e.type()).getString()).thenComparing(e->e.type().toString())).toList();
    }
    private void changePage(int delta){commitAll();page+=delta;rebuildRows();}
    private void rebuildRows(){
        if(search==null)return;
        for(var row:rows){removeWidget(row.toggle);removeWidget(row.minus);removeWidget(row.number);removeWidget(row.plus);}rows.clear();
        var list=filtered();page=Math.max(0,Math.min(page,Math.max(0,(list.size()-1)/ROWS)));
        for(var entry:list.stream().skip((long)page*ROWS).limit(ROWS).toList())rows.add(new Row(entry.type(),rows.size()));
        previous.active=page>0;next.active=(page+1)*ROWS<list.size();filter.setMessage(tr(nearby?"field_nearby":"field_all"));
    }
    private void refreshRows(){
        if(rows.stream().noneMatch(row->row.number.isFocused())&&drafts.isEmpty()){
            var ids=filtered().stream().skip((long)page*ROWS).limit(ROWS).map(FieldConfigSync.Entry::type).toList();
            if(!ids.equals(rows.stream().map(row->row.id).toList())){rebuildRows();return;}
        }
        for(var row:rows)row.refresh();
    }
    private Integer count(ResourceLocation id){
        String value=drafts.get(id);if(value==null)return pending.containsKey(id)?pending.get(id).count():entries.get(id).limit();
        try{return Integer.parseInt(value);}catch(NumberFormatException ignored){return null;}
    }
    private boolean enabled(ResourceLocation id){return pending.containsKey(id)?pending.get(id).enabled():entries.get(id).enabled();}
    private void send(ResourceLocation id,int count,boolean enabled){
        var update=new FieldConfigSync.Update(menu.containerId,id,count,enabled);pending.put(id,update);drafts.remove(id);FieldConfigSync.CHANNEL.sendToServer(update);
    }
    private void commit(ResourceLocation id){
        if(!drafts.containsKey(id))return;Integer value=count(id);
        if(value==null)drafts.remove(id);else send(id,value,enabled(id));
    }
    private void commitAll(){for(var id:List.copyOf(drafts.keySet()))commit(id);refreshRows();}
    @Override protected void containerTick(){
        super.containerTick();if(++ticks%20==0)request();
        for(var row:rows)if(!row.number.isFocused())commit(row.id);
        refreshRows();
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(getFocused() instanceof EditBox edit&&edit.isFocused()){
            if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){commitAll();setFocused(null);return true;}
            if(key==GLFW.GLFW_KEY_ESCAPE){commitAll();setFocused(null);return true;}
            return edit.keyPressed(key,scan,modifiers)||key!=GLFW.GLFW_KEY_TAB;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onClose(){commitAll();minecraft.setScreen(new MachineScreen(menu,minecraft.player.getInventory(),title));}
    @Override public void removed(){commitAll();menu.fieldConfiguration=false;menu.configurationView=false;super.removed();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff14212d);
        g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+25,0xff263d4b);
        for(int i=0;i<ROWS;i++)g.fill(leftPos+8,topPos+70+i*20,leftPos+350,topPos+89+i*20,i%2==0?0xff20323f:0xff192b38);
    }
    private void text(GuiGraphics g,Component value,int x,int y,int width,int color){String s=value.getString();if(font.width(s)>width)s=font.plainSubstrByWidth(s,width-font.width("…"))+"…";g.drawString(font,s,x,y,color,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        text(g,tr(menu.breeding?"field_breeding_title":"field_slaughter_title"),10,12,278,TEXT);
        text(g,tr("field_species"),12,57,120,MUTED);text(g,tr("field_current"),136,57,38,MUTED);
        text(g,tr("field_switch"),183,57,48,MUTED);text(g,tr(menu.breeding?"field_maximum":"field_minimum"),238,57,110,ACCENT);
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);var entry=entries.get(row.id);int y=76+i*20;
            text(g,name(row.id),12,y,120,TEXT);g.drawCenteredString(font,Integer.toString(entry.count()),153,y,entry.count()>0?ACCENT:MUTED);
        }
        if(rows.isEmpty())text(g,tr(!received?"field_loading":query.isBlank()&&nearby?"field_no_nearby":"field_no_results"),14,102,330,MUTED);
        g.drawCenteredString(font,tr("page_number",page+1,Math.max(1,(filtered().size()+ROWS-1)/ROWS)),71,198,MUTED);
        text(g,tr(!received?"field_loading":!pending.isEmpty()?"field_saving":!drafts.isEmpty()?"field_unsaved":"field_synced"),141,198,120,ACCENT);
        text(g,tr(menu.breeding?"field_breeding_hint":"field_slaughter_hint"),10,217,338,MUTED);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);
        for(int i=0;i<rows.size();i++)if(mx>=leftPos+10&&mx<leftPos+133&&my>=topPos+70+i*20&&my<topPos+90+i*20)g.renderTooltip(font,List.of(name(rows.get(i).id),Component.literal(rows.get(i).id.toString())),Optional.empty(),mx,my);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){if(mx>=leftPos&&mx<leftPos+imageWidth&&my>=topPos+70&&my<topPos+190){changePage(delta>0?-1:1);return true;}return super.mouseScrolled(mx,my,delta);}
    private final class Row {
        final ResourceLocation id;final Button toggle,minus,plus;final EditBox number;
        Row(ResourceLocation id,int index){
            this.id=id;int y=72+index*20;
            toggle=button(179,y,48,16,Component.empty(),()->{Integer count=count(id);if(count!=null)send(id,count,!enabled(id));refresh();});
            minus=button(233,y,16,16,Component.literal("−"),()->step(-1));
            number=addRenderableWidget(new EditBox(font,leftPos+253,topPos+y,60,16,tr(menu.breeding?"field_maximum":"field_minimum")));
            number.setMaxLength(7);number.setFilter(s->s.isEmpty()||s.chars().allMatch(c->c>='0'&&c<='9')&&s.length()<=7&&Integer.parseInt(s)<=FieldAutomation.MAX_POPULATION);
            number.setTooltip(Tooltip.create(tr("field_number_help")));number.setResponder(value->{if(!refreshing)drafts.put(id,value);});
            plus=button(317,y,16,16,Component.literal("+"),()->step(1));
            minus.setTooltip(Tooltip.create(tr("field_step_help")));plus.setTooltip(Tooltip.create(tr("field_step_help")));refresh();
        }
        void step(int direction){Integer count=count(id);if(count!=null)send(id,Math.max(0,Math.min(FieldAutomation.MAX_POPULATION,count+direction*(hasShiftDown()?10:1))),enabled(id));refresh();}
        void refresh(){toggle.setMessage(tr(enabled(id)?"field_enabled":"field_disabled"));if(!number.isFocused()){refreshing=true;number.setValue(drafts.getOrDefault(id,Integer.toString(count(id)==null?entries.get(id).limit():count(id))));refreshing=false;}}
    }
}
