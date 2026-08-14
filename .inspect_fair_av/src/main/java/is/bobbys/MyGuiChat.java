package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.ChatComponentText;
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

    @Override
    public void sendChatMessage(String msg, boolean addToChat) {
        if (msg.startsWith(".")) {
            switch (msg){
                case ".on gen":
                    GenDetect.on=true;
//                    EntityOtherPlayerMP
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§aTurn on GenDetect"));
                    break;
                case ".off gen":
                    GenDetect.on=false;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cTurn off GenDetect"));
                    break;
                case ".on esp":
                    ClientEvents.espOn=true;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§aTurn on Esp"));
                    break;
                case ".off esp":
                    ClientEvents.espOn=false;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cTurn off Esp"));
                    break;
                case ".on ac":
                    AutoClicker.on=true;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§aTurn on AutoClicker"));
                    break;
                case ".off ac":
                    AutoClicker.on=false;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cTurn off AutoClicker"));
                    break;
                case ".on pro":
                    ProDetect.on=true;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§aTurn on ProDetect"));
                    break;
                case ".off pro":
                    ProDetect.on=false;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cTurn off ProDetect"));
                    break;
                case ".night":
                    Minecraft mc=Minecraft.getMinecraft();
                    if (mc.gameSettings.gammaSetting==100.0F){
                        mc.gameSettings.gammaSetting=oldGamma;
                    }else {
                        oldGamma=mc.gameSettings.gammaSetting;
                        mc.gameSettings.gammaSetting=100.0F;
                    }
                    break;
                default:
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§eUnknown Command??"));
            }
            return;
        }

        super.sendChatMessage(msg, addToChat);
    }
}
