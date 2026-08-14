package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

public class Bridge {
    public static boolean on=true;
//    private static final int keyCode=
    public static boolean unpress=false;
    public static boolean canStand=false;
    public static KeyBinding bridgeKey=new KeyBinding("Bridge", Keyboard.KEY_B,"Fair Advantage");
    public static void tick(){
        if (unpress){
            KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(),false);
            unpress=false;
            Minecraft.getMinecraft().thePlayer.setSneaking(false);
            return;
        }
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        if (mop != null&&bridgeKey.isKeyDown() && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            switch (mop.sideHit) {
                case NORTH:
                case SOUTH:
                case EAST:
                case WEST:
                    KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(),true);
                    unpress=true;
                    Minecraft.getMinecraft().thePlayer.setSneaking(true);
                    break;
            }

        }
    }
}
