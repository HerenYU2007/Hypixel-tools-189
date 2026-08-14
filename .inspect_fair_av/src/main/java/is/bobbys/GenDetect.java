package is.bobbys;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.util.LinkedList;

public class GenDetect {
    public static boolean on=true;
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static int timer=0;
    public static final LinkedList<ScanInfo> dias=new LinkedList<>();
    public static final LinkedList<ScanInfo> ems=new LinkedList<>();
    public static void tick(){
        if (!on) return;
        if (++timer!=5) return;//every 5 tick
        timer=0;

        if (mc.theWorld == null) {
            return;
        }
        dias.clear();
        ems.clear();
        EntityItem entityItem;
        ItemStack stack;
        Item item;
        LoopA:
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityItem)) {
                continue;
            }
            entityItem = (EntityItem) obj;
            stack = entityItem.getEntityItem();
            if (stack == null) {
                continue;
            }

            item = stack.getItem();
            if (item.getRegistryName().equals("minecraft:diamond")&&bUnderItem(entityItem).getRegistryName().equals("minecraft:diamond_block")){
                for (ScanInfo info:dias){
                    if (info.samePos(entityItem)){
                        info.num+=stack.stackSize;
                        continue LoopA;
                    }
                }
                dias.add(new ScanInfo(entityItem));
                continue;
            }
            if (item.getRegistryName().equals("minecraft:emerald")&&bUnderItem(entityItem).getRegistryName().equals("minecraft:emerald_block")){
                for (ScanInfo info:ems){
                    if (info.samePos(entityItem)){
                        info.num+=stack.stackSize;
                        continue LoopA;
                    }
                }
                ems.add(new ScanInfo(entityItem));
                continue;
            }
        }
    }
    private static Block bUnderItem(EntityItem entityItem){
        return mc.theWorld.getBlockState(new BlockPos(entityItem.posX,entityItem.posY-1,entityItem.posZ)).getBlock();
    }
    public static String hudString(LinkedList<ScanInfo> list){
        StringBuilder builder=new StringBuilder();
        for (ScanInfo info:list){
            builder.append(info.num).append(" r=").append(info.r);
            if (info.looking) builder.append("(looking)");
            builder.append("// ");
        }
        return builder.toString();
    }
}
