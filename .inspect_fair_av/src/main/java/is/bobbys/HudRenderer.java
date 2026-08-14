package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import static is.bobbys.Bridge.bridgeKey;

public class HudRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String FIREBALL_WARNING = "\u6709\u706b\u7130\u5f39\u6765\u88ad";
    private static final float FIREBALL_WARNING_SCALE = 3.25F;

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
            float warningX = (sr.getScaledWidth() - fr.getStringWidth(FIREBALL_WARNING) * FIREBALL_WARNING_SCALE) / 2.0F;
            float warningY = sr.getScaledHeight() / 2.0F - 36.0F;
            GlStateManager.pushMatrix();
            GlStateManager.scale(FIREBALL_WARNING_SCALE, FIREBALL_WARNING_SCALE, 1.0F);
            fr.drawStringWithShadow(FIREBALL_WARNING, warningX / FIREBALL_WARNING_SCALE, warningY / FIREBALL_WARNING_SCALE, 0xFF3333);
            GlStateManager.popMatrix();
        }
    }
}
