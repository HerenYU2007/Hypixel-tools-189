package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;


public class Zoom {
    public static final KeyBinding zoomKey = new KeyBinding("Zoom", Keyboard.KEY_C, "Fair Advantage");
    private static boolean zooming = false;

    private static float oldFov;
    private static float zoomFov = 15F;
    private static float oldSensitivity;
    public static void tick(){
        Minecraft mc = Minecraft.getMinecraft();

        if (zoomKey.isKeyDown()) {

            if (!zooming) {
                zooming = true;
                oldFov = mc.gameSettings.fovSetting;
                oldSensitivity = mc.gameSettings.mouseSensitivity;
                mc.gameSettings.mouseSensitivity = oldSensitivity * 0.25F;
            }

            mc.gameSettings.fovSetting = zoomFov;
        }
        else {

            if (zooming) {
                zooming = false;
                mc.gameSettings.fovSetting = oldFov;
                mc.gameSettings.mouseSensitivity = oldSensitivity;
            }
        }
    }
}
