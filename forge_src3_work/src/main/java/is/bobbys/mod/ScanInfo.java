package is.bobbys.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class ScanInfo {
    int num;
    long r;
    boolean looking;
    BlockPos pos;

    public ScanInfo(EntityItem entityItem) {
        this.num = entityItem.getEntityItem().stackSize;
        EntityPlayer p=Minecraft.getMinecraft().thePlayer;
        this.r = Math.round(Math.sqrt(entityItem.getPosition().distanceSq(p.posX,p.posY,p.posZ)));
        this.looking=isLookingAt(Minecraft.getMinecraft().thePlayer,entityItem.getPosition());
        pos=entityItem.getPosition();
    }
    public boolean samePos(EntityItem item){
        BlockPos pos1=item.getPosition();
        return pos.getX()==pos1.getX()&&pos1.getY()==pos.getY()&&pos1.getZ()==pos.getZ();
    }
    public static boolean isLookingAt(EntityPlayer player, BlockPos target) {

        Vec3 look = player.getLookVec(); // 视线方向（单位向量）

        Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 targetPos = new Vec3(
                target.getX() + 0.5,
                target.getY() + 0.5,
                target.getZ() + 0.5
        );
        return look.dotProduct(targetPos.subtract(playerPos).normalize()) > 0.97;
    }
}
