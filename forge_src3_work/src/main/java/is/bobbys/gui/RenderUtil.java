package is.bobbys.gui;

import is.bobbys.mod.GenDetect;
import is.bobbys.mod.ProDetect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.Map;

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
                "\u94bb\u77f3\u8d44\u6e90\u70b9\uff1a ",
                x,
                y,
                0x00DBFF
        );
        fr.drawStringWithShadow(
                GenDetect.hudString(GenDetect.dias),
                x+getStringWidth("\u94bb\u77f3\u8d44\u6e90\u70b9\uff1a "),
                y,
                0xFFFFFF
        );
    }
    public static void drawEmer(int x,int y){
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        fr.drawStringWithShadow(
                "\u7eff\u5b9d\u77f3\u8d44\u6e90\u70b9\uff1a ",
                x,
                y,
                0x00FF51
        );
        fr.drawStringWithShadow(
                GenDetect.hudString(GenDetect.ems),
                x+getStringWidth("\u7eff\u5b9d\u77f3\u8d44\u6e90\u70b9\uff1a "),
                y,
                0xFFFFFF
        );
    }

    public static void drawPro(int x,int y){
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;
        for (Map.Entry<Integer,Integer> e:
                ProDetect.map.entrySet()) {
            fr.drawStringWithShadow(
                    String.valueOf(e.getValue()), x, y,
                    e.getKey()
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
