package is.bobbys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    /**
     * 绘制普通文字
     */
    public static void drawLine(String text, int x, int y) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                text,
                x,
                y,
                0xFFFFFF
        );
    }
    public static void drawDia(int x,int y){
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                "DiamondGen: ",
                x,
                y,
                0x00DBFF
        );
        fr.drawStringWithShadow(
                GenDetect.hudString(GenDetect.dias),
                x+getStringWidth("DiamondGen: "),
                y,
                0xFFFFFF
        );
    }
    public static void drawEmer(int x,int y){
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                "EmeraldGen: ",
                x,
                y,
                0x00FF51
        );
        fr.drawStringWithShadow(
                GenDetect.hudString(GenDetect.ems),
                x+getStringWidth("EmeraldGen: "),
                y,
                0xFFFFFF
        );
    }

    public static void drawPro(int x,int y){
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        for (int color :
                ProDetect.map.keySet()) {
            fr.drawStringWithShadow(
                    String.valueOf(ProDetect.map.get(color)), x, y,
                    color
            );
            x+=7;
        }
    }

    /**
     * 绘制标题
     */
    public static void drawTitle(String text, int x, int y) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                text,
                x,
                y,
                0x55FFFF
        );
    }

    /**
     * 绘制指定颜色文字
     */
    public static void drawColored(String text, int x, int y, int color) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                text,
                x,
                y,
                color
        );
    }

    /**
     * 获取文字宽度
     */
    public static int getStringWidth(String text) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return 0;
        return fr.getStringWidth(text);
    }

    /**
     * 获取字体高度
     */
    public static int getFontHeight() {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return 9;
        return fr.FONT_HEIGHT;
    }
}