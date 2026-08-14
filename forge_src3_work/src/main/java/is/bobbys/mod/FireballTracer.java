package is.bobbys.mod;

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

import java.util.*;

public final class FireballTracer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MARK_RADIUS = 2;
    private static final int TRACE_INTERVAL_TICKS = 5;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int EXPLOSION_MEMORY_TICKS = 80;
    private static final double TRACE_DISTANCE = 80.0D;
    private static final double MINSQ_DISTANCE = (8.0D)*8;
    private static final IBlockState RED_GLASS = Blocks.stained_glass.getDefaultState()
            .withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED);

    public static boolean on = true;

    private static final Map<BlockPos, IBlockState> replaced = new HashMap<>();
    private static final Map<BlockPos, Integer> recentExplosions = new HashMap<>();
    private static final LinkedList<BlockPos> hits=new LinkedList<>();
    private static boolean incomingFireball=false;
    private static int tickCounter=0;
    private static int refreshCounter=0;
    public static final HashSet<BlockPos> breaking=new HashSet<>();

    private FireballTracer() {
    }

    public static void tick() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        World world = mc.theWorld;
        tickRecentExplosions();

        if (!on) {
            incomingFireball = false;
            restoreAll(world);
            return;
        }

        if (++refreshCounter >= REFRESH_INTERVAL_TICKS) {
            refreshCounter = 0;
            refreshMarkers(world);
        }

        if (++tickCounter < TRACE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        incomingFireball = false;
        hits.clear();
        traceExistingFireballs(world);
        traceHeldFireCharges(world);
        restoreAll(world);

        for (BlockPos h:hits){
            markArea(world, h);
        }
    }

    public static void restoreAll(World world) {
        Iterator<Map.Entry<BlockPos, IBlockState>> iterator = replaced.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, IBlockState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            IBlockState current = world.getBlockState(pos);
            if (current.getBlock() == Blocks.stained_glass) {
                world.setBlockState(pos, replacementStateFor(pos, entry.getValue()), 2);
            }
            world.markBlockRangeForRenderUpdate(pos, pos);
            iterator.remove();
        }
    }

    public static void onExplosion(List<BlockPos> affectedBlocks) {
        if (affectedBlocks == null || affectedBlocks.isEmpty()) {
            return;
        }
        World world = mc.theWorld;
        for (BlockPos pos : affectedBlocks) {
            recentExplosions.put(pos, EXPLOSION_MEMORY_TICKS);
            if (world != null) {
                restoreMarker(world, pos);
            }
        }
    }

    public static boolean hasIncomingFireball() {
        return incomingFireball;
    }

    private static void traceExistingFireballs(World world) {
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
            MovingObjectPosition hit = world.rayTraceBlocks(start, end, true, true, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK&&hit.getBlockPos().distanceSq(fireball.getPosition())>MINSQ_DISTANCE) {
                hits.add(hit.getBlockPos());
            }
        }
    }

    private static void traceHeldFireCharges(World world) {
        for (EntityPlayer player : world.playerEntities) {
            ItemStack held = player.getHeldItem();
            if (held == null || held.getItem() != Items.fire_charge) {
                continue;
            }

            Vec3 start = player.getPositionEyes(1.0F);
            Vec3 look = player.getLook(1.0F);
            Vec3 end = start.addVector(look.xCoord * TRACE_DISTANCE, look.yCoord * TRACE_DISTANCE, look.zCoord * TRACE_DISTANCE);
            MovingObjectPosition hit = world.rayTraceBlocks(start, end, true, true, false);
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
        BlockPos pos;
        IBlockState current;
        Block block;
        int dy,dz;
        for (int dx = -MARK_RADIUS; dx <= MARK_RADIUS; dx++) {
            for (dy = -MARK_RADIUS; dy <= MARK_RADIUS; dy++) {
                for (dz = -MARK_RADIUS; dz <= MARK_RADIUS; dz++) {
                    pos = hitPos.add(dx, dy, dz);
                    current = world.getBlockState(pos);
                    block = current.getBlock();
                    if (breaking.contains(pos)){
                        restoreMarker(world, pos);
                        continue;
                    }
                    if (replaced.containsKey(pos)) {
                        continue;
                    }
                    if (block == Blocks.air || block == Blocks.stained_glass || !block.isFullCube()) {
                        continue;
                    }
                    replaced.put(pos, current);
                    world.setBlockState(pos, RED_GLASS, 3);
                    world.markBlockRangeForRenderUpdate(pos, pos);
                }
            }
        }
        world.markBlockRangeForRenderUpdate(hitPos.add(-MARK_RADIUS, -MARK_RADIUS, -MARK_RADIUS), hitPos.add(MARK_RADIUS, MARK_RADIUS, MARK_RADIUS));
    }

    private static void restoreMarker(World world, BlockPos pos) {
        IBlockState original = replaced.remove(pos);
        if (original == null) {
            return;
        }
        if (world.getBlockState(pos).getBlock() == Blocks.stained_glass) {
            world.setBlockState(pos, replacementStateFor(pos, original), 2);
        }
        world.markBlockRangeForRenderUpdate(pos, pos);
    }

    private static IBlockState replacementStateFor(BlockPos pos, IBlockState original) {
        if (recentExplosions.containsKey(pos)) {
            return Blocks.air.getDefaultState();
        }
        return original;
    }

    private static void refreshMarkers(World world) {
        Iterator<BlockPos> iterator = replaced.keySet().iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (world.getBlockState(pos).getBlock() != Blocks.stained_glass) {
                iterator.remove();
            }
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    private static void tickRecentExplosions() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = recentExplosions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }
}
