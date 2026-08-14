package is.bobbys.gui;

import is.bobbys.ClientEvents;
import is.bobbys.ModU;
import is.bobbys.mod.AutoClicker;
import is.bobbys.mod.GenDetect;
import is.bobbys.mod.ProDetect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.util.Map;

/*
颜色代码：不同的颜色对应不同的代码，以下是一些常用的颜色代码：
§0 - 黑色
§1 - 深蓝色
§2 - 深绿色
§3 - 深青色
§4 - 深红色
§5 - 深紫色
§6 - 橙色
§7 - 灰色
§8 - 深灰色
§9 - 浅蓝色
§a - 浅绿色
§b - 浅青色
§c - 浅红色
§d - 浅紫色
§e - 黄色
§f - 白色
 */
public class MyGuiChat extends GuiChat {
    public MyGuiChat(){
        super();
    }
    public MyGuiChat(String s){
        super(s);
    }
    private float oldGamma=30.0F;
    private static final Minecraft mc=Minecraft.getMinecraft();
    @Override
    public void sendChatMessage(String msg, boolean addToChat) {
        if (msg.startsWith(".")) {
            switch (msg){
                case ".on gen":
                    GenDetect.on=true;
//                    EntityOtherPlayerMP
                    mc.thePlayer.addChatMessage(new ChatComponentText("§aTurn on GenDetect"));
                    break;
                case ".off gen":
                    GenDetect.on=false;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§cTurn off GenDetect"));
                    break;
                case ".on esp":
                    ClientEvents.espOn=true;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§aTurn on Esp"));
                    break;
                case ".off esp":
                    ClientEvents.espOn=false;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§cTurn off Esp"));
                    break;
                case ".on ac":
                    AutoClicker.on=true;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§aTurn on AutoClicker"));
                    break;
                case ".off ac":
                    AutoClicker.on=false;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§cTurn off AutoClicker"));
                    break;
                case ".on pro":
                    ProDetect.on=true;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§aTurn on ProDetect"));
                    break;
                case ".off pro":
                    ProDetect.on=false;
                    mc.thePlayer.addChatMessage(new ChatComponentText("§cTurn off ProDetect"));
                    break;
                case ".night":
                    if (mc.gameSettings.gammaSetting==100.0F){
                        mc.gameSettings.gammaSetting=oldGamma;
                    }else {
                        oldGamma=mc.gameSettings.gammaSetting;
                        mc.gameSettings.gammaSetting=100.0F;
                    }
                    break;
                case ".help":
                    mc.thePlayer.addChatMessage(new ChatComponentText("gen\n" +
                            "esp\n" +
                            "ac(unstable)\n" +
                            "pro\n" +
                            ".night\n" +
                            "fb\n"));
                    break;
                case ".p":
                    for (EntityPlayer player: mc.theWorld.playerEntities) {
                        Map<Integer,Integer> enchants = EnchantmentHelper.getEnchantments(player.getCurrentArmor(3));

                        say(player.getName());
                        for(Map.Entry<Integer,Integer> entry
                                : enchants.entrySet())
                        {

                            int enchantId = entry.getKey();
                            int level = entry.getValue();
                            Enchantment enchant =Enchantment.getEnchantmentById(enchantId);

                            say(enchant.getTranslatedName(level));

                        }
                    }
                    break;
                default:
                    mc.thePlayer.addChatMessage(new ChatComponentText("§eUnknown Command??"));


            }
            return;
        }

        super.sendChatMessage(msg, addToChat);
    }
    public static void say(String s){
        mc.thePlayer.addChatMessage(new ChatComponentText(s));
    }
}
