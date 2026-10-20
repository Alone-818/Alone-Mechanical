package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ItemDescriptionGameTests {
    @GameTest(template="empty")
    public static void everyModItemUsesOneShiftHintAndExpandedDescriptions(GameTestHelper h){
        for(var entry:ModItems.ITEMS.getEntries()){
            var stack=entry.get().getDefaultInstance();var raw=new ArrayList<Component>();
            stack.getItem().appendHoverText(stack,h.getLevel(),raw,TooltipFlag.Default.NORMAL);
            h.assertTrue(raw.isEmpty(),"Individual items must not leak descriptions outside the shared Shift policy: "+entry.getId());
            var collapsed=new ArrayList<Component>();ItemDescriptions.append(stack,collapsed,false);
            h.assertTrue(collapsed.size()==1&&collapsed.get(0).getContents() instanceof TranslatableContents text&&text.getKey().equals(ItemDescriptions.EXPAND),"Every item and BlockItem must expose exactly one common collapsed hint: "+entry.getId());
            var expanded=new ArrayList<Component>();ItemDescriptions.append(stack,expanded,true);
            h.assertTrue(expanded.size()>=2&&expanded.stream().noneMatch(c->c.getContents() instanceof TranslatableContents text&&text.getKey().equals(ItemDescriptions.EXPAND)),"Shift must replace the hint with function and information: "+entry.getId());
        }
        var foreign=new ArrayList<Component>();ItemDescriptions.append(new ItemStack(Items.IRON_INGOT),foreign,false);
        h.assertTrue(foreign.isEmpty(),"The description policy must only apply to this mod");h.succeed();
    }
    @GameTest(template="empty")
    public static void everyDescriptionExistsInChineseAndEnglish(GameTestHelper h)throws Exception{
        for(String language:new String[]{"zh_cn","en_us"}){
            try(var input=ItemDescriptionGameTests.class.getResourceAsStream("/assets/alone_mechanical/lang/"+language+".json")){
                h.assertTrue(input!=null,"Language resources must exist");
                var translations=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                h.assertTrue(translations.has(ItemDescriptions.EXPAND),"The common Shift hint must be localized");
                for(var entry:ModItems.ITEMS.getEntries()){
                    var stack=entry.get().getDefaultInstance();
                    h.assertTrue(translations.has(stack.getDescriptionId()),"Missing "+language+" item display name: "+entry.getId());
                    if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof PipeBlock)continue;
                    for(String section:new String[]{"function","info"}){
                        String key=ItemDescriptions.key(stack,section);
                        h.assertTrue(translations.has(key)&&!translations.get(key).getAsString().isBlank(),"Missing "+language+" item description: "+key);
                    }
                }
                for(String chapter:new String[]{"start","grid","structure","machines","reactors","separation","conversion","storage","pipes","upgrades","help"}){
                    var body=translations.get("tutorial.alone_mechanical."+chapter+".body");
                    h.assertTrue(body!=null&&body.isJsonPrimitive()&&!body.getAsString().isBlank(),"Both languages must contain nonempty tutorial text: "+chapter);
                }
            }
        }h.succeed();
    }
}
