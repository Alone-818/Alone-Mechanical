package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.function.Supplier;

/** Scrollable chapter directory on the left, independently paginated content on the right. */
public final class TutorialScreen extends Screen {
    private record Chapter(String id,Supplier<ItemStack> icon) {}
    private static final List<Chapter> CHAPTERS=List.of(
        new Chapter("start",()->new ItemStack(ModItems.TUTORIAL_BOOK.get())),
        new Chapter("manufacturing",()->new ItemStack(ModItems.BASIC_CIRCUIT.get())),
        new Chapter("battery_manufacturing",()->new ItemStack(ModItems.ADVANCED_BATTERY.get())),
        new Chapter("grid",()->new ItemStack(ModBlocks.POWER_CORE.get())),
        new Chapter("structure",()->new ItemStack(ModItems.ENGINEERING_HAMMER.get())),
        new Chapter("machines",()->new ItemStack(ModBlocks.CRUSHER_CORE.get())),
        new Chapter("builds",()->new ItemStack(ModItems.ENGINEERING_HAMMER.get())),
        new Chapter("towers",()->new ItemStack(ModBlocks.DISTILLATION_COMPONENT.get())),
        new Chapter("boiler",()->new ItemStack(ModBlocks.BOILER_CORE.get())),
        new Chapter("nuclear_reactor",()->new ItemStack(ModBlocks.REACTOR_FUEL_ROD.get())),
        new Chapter("reactor_safety",()->new ItemStack(ModBlocks.REINFORCED_CASING.get())),
        new Chapter("nuclear_fuel",()->new ItemStack(ModItems.ENRICHED_URANIUM_COMPOUND.get())),
        new Chapter("ammonia_fertilizer",()->new ItemStack(ModItems.FERTILIZER.get())),
        new Chapter("pumps",()->new ItemStack(ModBlocks.ELECTRIC_PUMP.get())),
        new Chapter("reactors",()->new ItemStack(ModBlocks.REACTION_CORE.get())),
        new Chapter("sulfuric_acid",()->new ItemStack(ModItems.SULFUR.get())),
        new Chapter("ore_processing",()->new ItemStack(ModItems.IRON_COMPOUND_CRYSTAL.get())),
        new Chapter("nonmetal_processing",()->new ItemStack(net.minecraft.world.item.Items.DIAMOND)),
        new Chapter("heavy_water",()->new ItemStack(ModBlocks.DISTILLATION_COMPONENT.get())),
        new Chapter("dimension_fluids",()->new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL)),
        new Chapter("void_ores",()->new ItemStack(net.minecraft.world.item.Items.IRON_ORE)),
        new Chapter("separation",()->new ItemStack(ModBlocks.DECOMPOSITION_COMPONENT.get())),
        new Chapter("conversion",()->new ItemStack(ModBlocks.CONVERSION_CORE.get())),
        new Chapter("storage",()->new ItemStack(ModBlocks.ITEM_VAULT.get())),
        new Chapter("energy_storage",()->new ItemStack(ModBlocks.ENERGY_MATRIX.get())),
        new Chapter("goggles",()->new ItemStack(ModItems.ENGINEERING_GOGGLES.get())),
        new Chapter("pipes",()->new ItemStack(ModBlocks.PIPES.get("item_pipe_3").get())),
        new Chapter("upgrades",()->new ItemStack(ModBlocks.PROCESSING_MATRIX.get())),
        new Chapter("help",()->new ItemStack(ModItems.WRENCH.get())));
    private final Button[] chapters=new Button[CHAPTERS.size()];
    private int chapter,page,left,top,panelWidth,panelHeight,bodyX,bodyWidth,linesPerPage;
    private List<FormattedCharSequence> lines=List.of();
    private Component content=Component.empty();
    private Button previous,next;
    private int chapterScroll,visibleChapters;
    private boolean draggingDirectory;
    public TutorialScreen(){super(tr("title"));}
    public static void open(){Minecraft.getInstance().setScreen(new TutorialScreen());}
    private static Component tr(String key,Object... args){return Component.translatable("tutorial.alone_mechanical."+key,args);}
    public int chapterCount(){return CHAPTERS.size();}
    public int chapterIndex(){return chapter;}
    public String chapterId(){return CHAPTERS.get(chapter).id;}
    public Component chapterContent(){return content;}
    public int pageIndex(){return page;}
    public boolean directoryVisible(){return true;}
    public int directoryScrollIndex(){return chapterScroll;}
    public int directoryVisibleRows(){return visibleChapters;}
    public int pageCount(){return Math.max(1,(lines.size()+linesPerPage-1)/Math.max(1,linesPerPage));}
    @Override protected void init() {
        panelWidth=Math.min(520,width-16);panelHeight=Math.min(282,height-16);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        bodyX=left+160;bodyWidth=panelWidth-178;linesPerPage=Math.max(1,(panelHeight-84)/12);
        visibleChapters=Math.max(1,(panelHeight-76)/22);
        chapterScroll=Math.max(0,Math.min(chapterScroll,CHAPTERS.size()-visibleChapters));
        for(int i=0;i<CHAPTERS.size();i++) {
            int index=i;
            chapters[i]=addRenderableWidget(new GuideButton(left+8,top+40+(i-chapterScroll)*22,132,20,tr(CHAPTERS.get(i).id+".title"),b->selectChapter(index),index));
            chapters[i].setTooltip(Tooltip.create(tr(CHAPTERS.get(i).id+".title")));
        }
        previous=addRenderableWidget(new GuideButton(bodyX,top+panelHeight-24,54,16,tr("previous"),b->turn(-1),-1));
        next=addRenderableWidget(new GuideButton(left+panelWidth-66,top+panelHeight-24,54,16,tr("next"),b->turn(1),-1));
        var close=addRenderableWidget(new GuideButton(left+panelWidth-27,top+6,18,16,Component.literal("×"),b->onClose(),-1));
        close.setTooltip(Tooltip.create(tr("close")));
        wrap();
    }
    public void selectChapter(int index){chapter=Math.max(0,Math.min(CHAPTERS.size()-1,index));if(chapter<chapterScroll)chapterScroll=chapter;else if(chapter>=chapterScroll+visibleChapters)chapterScroll=Math.max(0,chapter-visibleChapters+1);page=0;wrap();}
    private void wrap(){
        var id=CHAPTERS.get(chapter).id;
        Component body=switch(id){
            case "grid" -> tr(id+".body",Config.SIGNAL_RANGE.get(),Config.PYLON_RANGE.get(),2*Config.SUPPLY_RADIUS.get()+1,2*Config.SUPPLY_RADIUS.get()+1,Config.SUPPLY_HEIGHT.get());
            case "structure" -> tr(id+".body",Config.MAX_MACHINE_SIZE.get());
            case "energy_storage" -> tr(id+".body",Config.MAX_MACHINE_SIZE.get(),Config.MACHINE_CAPACITY.get(),Config.MACHINE_CHARGE_RATE.get());
            case "upgrades" -> tr(id+".body",Config.MACHINE_CAPACITY.get());
            default -> tr(id+".body");
        };
        content=Component.literal(body.getString().replace("\\n","\n"));lines=font.split(content,Math.max(30,bodyWidth));page=Math.min(page,pageCount()-1);updateButtons();
    }
    private void updateButtons(){
        for(int i=0;i<chapters.length;i++){chapters[i].visible=i>=chapterScroll&&i<chapterScroll+visibleChapters;chapters[i].setY(top+40+(i-chapterScroll)*22);chapters[i].active=i!=chapter;}
        previous.active=chapter>0 || page>0;next.active=chapter+1<CHAPTERS.size() || page+1<pageCount();
    }
    private void turn(int direction){
        if(direction>0){if(page+1<pageCount())page++;else if(chapter+1<CHAPTERS.size()){selectChapter(chapter+1);return;}}
        else if(page>0)page--;else if(chapter>0){selectChapter(chapter-1);page=pageCount()-1;}
        updateButtons();
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){
        if(mx>=left+6&&mx<left+150&&my>=top+28&&my<top+panelHeight-8){chapterScroll=Math.max(0,Math.min(CHAPTERS.size()-visibleChapters,chapterScroll+(delta>0?-1:1)));updateButtons();return true;}
        if(mx>=bodyX && mx<left+panelWidth && my>=top+26 && my<top+panelHeight){turn(delta<0?1:-1);return true;}
        return super.mouseScrolled(mx,my,delta);
    }
    private void dragDirectory(double mouseY){double ratio=(mouseY-top-40)/Math.max(1,visibleChapters*22-2);chapterScroll=Math.max(0,Math.min(CHAPTERS.size()-visibleChapters,(int)Math.round(ratio*(CHAPTERS.size()-visibleChapters))));updateButtons();}
    @Override public boolean mouseClicked(double mx,double my,int button){if(button==0&&mx>=left+140&&mx<=left+149&&my>=top+40&&my<top+40+visibleChapters*22){draggingDirectory=true;dragDirectory(my);return true;}return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(draggingDirectory&&button==0){dragDirectory(my);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){if(button==0&&draggingDirectory){draggingDirectory=false;return true;}return super.mouseReleased(mx,my,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT || key==GLFW.GLFW_KEY_PAGE_DOWN){turn(1);return true;}
        if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_PAGE_UP){turn(-1);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    private void box(GuiGraphics g,int x,int y,int w,int h,int border,int fill){g.fill(x,y,x+w,y+h,border);g.fill(x+1,y+1,x+w-1,y+h-1,fill);}
    private String fit(Component component,int available){String s=component.getString();return font.width(s)<=available?s:font.plainSubstrByWidth(s,Math.max(0,available-font.width("…")))+"…";}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);
        box(g,left,top,panelWidth,panelHeight,0xff718492,0xff17232d);
        g.fill(left+1,top+1,left+panelWidth-1,top+26,0xff2b3d4b);
        g.renderItem(new ItemStack(ModItems.TUTORIAL_BOOK.get()),left+10,top+5);
        g.drawString(font,title,left+33,top+9,0xffe6eff1,false);
        g.drawString(font,tr("directory"),left+11,top+29,0xff99adbb,false);
        g.fill(left+150,top+29,left+151,top+panelHeight-9,0xff354c5b);
        int trackHeight=visibleChapters*22-2,thumb=Math.max(12,trackHeight*visibleChapters/CHAPTERS.size());
        int thumbY=top+40+(trackHeight-thumb)*chapterScroll/Math.max(1,CHAPTERS.size()-visibleChapters);
        g.fill(left+143,top+40,left+147,top+40+trackHeight,0xff263b49);g.fill(left+143,thumbY,left+147,thumbY+thumb,0xff6bc4b3);
        g.drawString(font,fit(tr("scroll_directory"),132),left+9,top+panelHeight-20,0xff99adbb,false);
        g.drawString(font,fit(tr(CHAPTERS.get(chapter).id+".title"),bodyWidth),bodyX,top+36,0xff7cd1bf,false);
        g.fill(bodyX,top+50,left+panelWidth-14,top+51,0xff354c5b);
        int start=page*linesPerPage,end=Math.min(lines.size(),start+linesPerPage);
        for(int i=start;i<end;i++)g.drawString(font,lines.get(i),bodyX,top+59+(i-start)*12,0xffdbe5e9,false);
        g.drawCenteredString(font,tr("page",page+1,pageCount()),bodyX+bodyWidth/2,top+panelHeight-20,0xffa4b9c5);
        super.render(g,mx,my,partial);
    }
    private final class GuideButton extends Button {
        private final int index;
        GuideButton(int x,int y,int w,int h,Component title,OnPress action,int index){super(x,y,w,h,title,action,DEFAULT_NARRATION);this.index=index;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            boolean selected=index>=0&&index==chapter;
            box(g,getX(),getY(),width,height,selected?0xff6bc4b3:isHoveredOrFocused()&&active?0xff8babbc:0xff3f5665,selected?0xff294741:0xff22323f);
            int color=selected?0xffa3ead4:active?0xffe0eaf0:0xff738997;
            if(index>=0){
                float iconScale=Math.min(1f,(height-2)/16f);
                g.pose().pushPose();g.pose().translate(getX()+3,getY()+(height-16*iconScale)/2,0);g.pose().scale(iconScale,iconScale,1);
                g.renderItem(CHAPTERS.get(index).icon.get(),0,0);g.pose().popPose();
                g.drawString(font,fit(getMessage(),width-27),getX()+23,getY()+(height-8)/2,color,false);
            }
            else g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+(height-8)/2,color);
        }
    }
}
