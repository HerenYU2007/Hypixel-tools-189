package is.bobbys.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Random;

public final class AutoClicker {
    private static int delay = 0;
    private static int randDelay=-1;
    public static boolean on=false;
    public static final Random r=new Random();
    public static final KeyBinding atkKey=new KeyBinding("AutoAttack", Keyboard.KEY_B,"Fair Advantage");

    public static void updateLeftClick() {
        if (!on) return;
        delay--;
        if (delay > 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!atkKey.isKeyDown()) return; // 必须按住左键
        // ❌ 正在挖方块
        if (mc.playerController.getIsHittingBlock()) return;
        if (delay<0){
            if (randDelay==-1) randDelay=r.nextInt(3)+2;
        }
        if (randDelay>0){
            if (--randDelay>0) return;
        }

        if (MovingObjectPosition.MovingObjectType.ENTITY == mc.objectMouseOver.typeOfHit) {
            if (mc.objectMouseOver.entityHit instanceof EntityPlayer){
                EntityPlayer p=(EntityPlayer) mc.objectMouseOver.entityHit;
//                if (ModU.myColor==ModU.getPlayerColor(p)) return;
                if (p.getHealth()<=0) return;
            }
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase){
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
                delay=10;
                randDelay=-1;
            }

        }

    }
}
