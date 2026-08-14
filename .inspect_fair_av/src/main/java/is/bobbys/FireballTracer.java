package is.bobbys;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class FireballTracer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MARK_RADIUS = 2;
    private static final double TRACE_DISTANCE = 100.0D;
    private static final IBlockState RED_GLASS = Blocks.stained_glass.getDefaultState()
            .withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED);

    public static boolean on = true;

    private static final Map<BlockPos, IBlockState> replaced = new HashMap<BlockPos, IBlockState>();
    private static boolean incomingFireball;
    private static int tickCounter;

    private FireballTracer() {
    }

    public static void tick() {
        if (!on || mc.theWorld == null || mc.thePlayer == null) {
            incomingFireball = false;
            tickCounter = 0;
            restoreAll(mc.theWorld);
            return;
        }

        if (++tickCounter < 5) {
            return;
        }
        tickCounter = 0;

        World world = mc.theWorld;
        incomingFireball = false;

        Set<BlockPos> hits = new LinkedHashSet<BlockPos>();
        collectExistingFireballs(world, hits);
        collectHeldFireCharges(world, hits);
        restoreAll(world);

        if (hits.isEmpty()) {
            return;
        }

        for (BlockPos hit : hits) {
            markArea(world, hit);
        }
    }

    public static void restoreAll(World world) {
        if (world != null) {
            for (Map.Entry<BlockPos, IBlockState> entry : replaced.entrySet()) {
                world.setBlockState(entry.getKey(), entry.getValue(), 2);
            }
        }
        replaced.clear();
    }

    public static boolean hasIncomingFireball() {
        return incomingFireball;
    }

    private static void collectExistingFireballs(World world, Set<BlockPos> hits) {
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityFireball)) {
                continue;
            }

            EntityFireball fireball = (EntityFireball) object;
            Vec3 direction = directionOf(fireball);
            if (direction.lengthVector() < 0.0001D) {
                continue;
            }

            incomingFireball = true;
            Vec3 dir = direction.normalize();
            Vec3 start = new Vec3(fireball.posX, fireball.posY, fireball.posZ);
            Vec3 end = start.addVector(dir.xCoord * TRACE_DISTANCE, dir.yCoord * TRACE_DISTANCE, dir.zCoord * TRACE_DISTANCE);
            MovingObjectPosition hit = world.rayTraceBlocks(start, end, false, true, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                hits.add(hit.getBlockPos());
            }
        }
    }

    private static void collectHeldFireCharges(World world, Set<BlockPos> hits) {
        for (Object object : world.playerEntities) {
            if (!(object instanceof EntityPlayer)) {
                continue;
            }

            EntityPlayer player = (EntityPlayer) object;
            ItemStack held = player.getHeldItem();
            if (held == null || held.getItem() != Items.fire_charge) {
                continue;
            }

            Vec3 start = player.getPositionEyes(1.0F);
            Vec3 look = player.getLook(1.0F);
            Vec3 end = start.addVector(look.xCoord * TRACE_DISTANCE, look.yCoord * TRACE_DISTANCE, look.zCoord * TRACE_DISTANCE);
            MovingObjectPosition hit = world.rayTraceBlocks(start, end, false, true, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                hits.add(hit.getBlockPos());
            }
        }
    }

    private static Vec3 directionOf(EntityFireball fireball) {
        Vec3 motion = new Vec3(fireball.motionX, fireball.motionY, fireball.motionZ);
        if (motion.lengthVector() >= 0.0001D) {
            return motion;
        }
        return new Vec3(fireball.accelerationX, fireball.accelerationY, fireball.accelerationZ);
    }

    private static void markArea(World world, BlockPos hitPos) {
        for (int dx = -MARK_RADIUS; dx <= MARK_RADIUS; dx++) {
            for (int dy = -MARK_RADIUS; dy <= MARK_RADIUS; dy++) {
                for (int dz = -MARK_RADIUS; dz <= MARK_RADIUS; dz++) {
                    BlockPos pos = hitPos.add(dx, dy, dz);
                    IBlockState current = world.getBlockState(pos);
                    Block block = current.getBlock();

                    if (block == Blocks.stained_glass && replaced.containsKey(pos)) {
                        continue;
                    }
                    if (block == Blocks.air || block == Blocks.stained_glass || !block.isFullCube()) {
                        continue;
                    }

                    if (!replaced.containsKey(pos)) {
                        replaced.put(pos, current);
                    }
                    world.setBlockState(pos, RED_GLASS, 3);
                    world.markBlockRangeForRenderUpdate(pos, pos);
                }
            }
        }
    }

    private static void pruneMarkers(World world) {
        Iterator<Map.Entry<BlockPos, IBlockState>> iterator = replaced.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, IBlockState> entry = iterator.next();
            if (world.getBlockState(entry.getKey()).getBlock() != Blocks.stained_glass) {
                iterator.remove();
            }
        }
    }
}
