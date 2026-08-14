package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;

public class ProDetect {
    public static boolean on=true;
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static int timer=0;
    public static final HashMap<Integer,Integer> map=new HashMap<>();
    public static void tick(){
        if (!on) return;
        if (++timer!=20) return;//every 5 tick
        timer=0;
        if (mc.theWorld == null) {
            return;
        }
        map.clear();
        int color;
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityPlayer)) {
                continue;
            }
            color=ModU.getPlayerColor((EntityPlayer) obj);
            if (color==-1) continue;
            if (map.containsKey(color)) continue;
            map.put(color,ModU.getProtection((EntityPlayer) obj));
        }
    }
}
