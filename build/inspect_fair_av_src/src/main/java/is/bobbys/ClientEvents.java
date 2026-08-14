package is.bobbys;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ClientEvents {

    private final Minecraft mc = Minecraft.getMinecraft();
    public static boolean espOn=false;

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            FireballTracer.restoreAll(event.world);
        }
    }

    /**
     * 每 Tick 扫描一次世界中的掉落物
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // 只在 Tick 结束时执行一次
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // 未进入世界
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        ModU.myColor=ModU.getPlayerColor(mc.thePlayer);
        GenDetect.tick();
        ProDetect.tick();
        FireballTracer.tick();
        Zoom.tick();
//        Bridge.tick();
        AutoClicker.updateLeftClick();

    }

    /**
     * 绘制 HUD
     */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        HudRenderer.render();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChat &&
                !(event.gui instanceof MyGuiChat)) {
            event.gui = new MyGuiChat();
        }
    }
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // 游戏未加载
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        // 已经打开GUI
        if (mc.currentScreen != null) {
            return;
        }
        // '.' 键（主键盘）
        if (Keyboard.getEventKey() == Keyboard.KEY_PERIOD
                && Keyboard.getEventKeyState()) {
            mc.displayGuiScreen(new MyGuiChat("."));
        }
    }
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        Entity player = mc.getRenderViewEntity();
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        if (espOn) {
            AxisAlignedBB box;
            EntityPlayer entityPlayer;
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableDepth();
            for (Entity e : mc.theWorld.loadedEntityList) {
                if ((e instanceof EntityPlayer) && (e != mc.thePlayer)) {
                    box = e.getEntityBoundingBox().offset(-x, -y, -z);
                    entityPlayer=(EntityPlayer) e;
                    if (ModU.myColor == -1) GlStateManager.color(1, 1, 1, 1);
                    else {
                        int color2 = ModU.getPlayerColor(entityPlayer);
                        if (color2 == -1) GlStateManager.color(1, 1, 1, 1);
                        else {
                            if (color2 == ModU.myColor) GlStateManager.color(0, 1, 0, 1);
                            else GlStateManager.color(1, 0, 0, 1);
                        }
                    }
                    RenderGlobal.drawSelectionBoundingBox(box);
                }
            }
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        BlockPos pos = mop.getBlockPos();
        World world = mc.theWorld;

        Block block = world.getBlockState(pos).getBlock();
        if (block == null || block.getMaterial() == Material.air) return;

        AxisAlignedBB aabb = block.getSelectedBoundingBox(world, pos);
        if (aabb == null) return;

        drawHighlightBox(aabb, x,y,z);
    }

    private void drawHighlightBox(AxisAlignedBB aabb, double x,double y,double z) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GL11.glDepthMask(true);
        GlStateManager.translate(-x, -y, -z);
        GlStateManager.color(0.7F, 1, 0, 1);
        RenderGlobal.drawSelectionBoundingBox(aabb);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
