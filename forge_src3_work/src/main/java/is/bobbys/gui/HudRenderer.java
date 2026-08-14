package is.bobbys.gui;

import is.bobbys.mod.FireballTracer;
import is.bobbys.mod.GenDetect;
import is.bobbys.mod.HomeAlert;
import is.bobbys.mod.ProDetect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import static is.bobbys.mod.Bridge.bridgeKey;

public class HudRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String FIREBALL_WARNING = "\u6709\u706b\u5f39";

    public static void render() {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) {
            return;
        }
        int x = 5;
        int y = 5;

        if (GenDetect.on) {
            RenderUtil.drawDia(x, y);
            y += 10;
            RenderUtil.drawEmer(x, y);
            y += 10;
        }
        if (ProDetect.on) {
            RenderUtil.drawPro(x, y);
            y += 10;
        }
        if (bridgeKey.isKeyDown()) {
            RenderUtil.drawLine("Bridging", x, y);
            y += 10;
        }
        if (FireballTracer.hasIncomingFireball()) {
            ScaledResolution sr = new ScaledResolution(mc);
            int warningX = (sr.getScaledWidth() - fr.getStringWidth(FIREBALL_WARNING)) / 2;
            int warningY = sr.getScaledHeight() / 2 - 24;
            fr.drawStringWithShadow(FIREBALL_WARNING, warningX, warningY, 0xFF3333);
        }
        HomeAlert.render();
    }
}
