package is.bobbys.mod;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class HomeAlert {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int INVIS_WARNING_TICKS = 200;
    private static final int INVIS_ALERT_MAX_DISTANCE = 50;
    private static final int HOME_SCAN_INTERVAL_TICKS = 20;
    private static final int HOME_WARNING_TICKS = 100;
    private static final int HOME_WARNING_COOLDOWN_TICKS = 100;
    private static final int HOME_STAY_WARNING_TICKS = 100;
    private static final int HOME_STAY_TRIGGER_TICKS = 400;
    private static final int HOME_ALERT_MAX_DISTANCE = 50;
    private static final int HOME_BED_SCAN_RADIUS = 64;
    private static final int HOME_BED_VERTICAL_RADIUS = 16;
    private static final int HOME_INVIS_MAX_HEIGHT_ABOVE_HOME = 30;

    private static final float INVIS_WARNING_SCALE = 2.0F;
    private static final int INVIS_WARNING_COLOR = 0x00BFFF;
    private static final float HOME_WARNING_SCALE = 2.0F;
    private static final float HOME_MULTI_WARNING_SCALE = 1.5F;
    private static final int HOME_WARNING_COLOR = 0x00FFFF;

    private static final Set<String> INVIS_ACTIVE_TEAMS = new LinkedHashSet<String>();
    private static final Set<EntityPlayer> HOME_ACTIVE_ENTITIES = new LinkedHashSet<EntityPlayer>();
    private static final Map<EntityPlayer, Integer> HOME_ENTITY_STAY_TICKS = new HashMap<EntityPlayer, Integer>();
    private static final Set<EntityPlayer> HOME_STAY_ALERTED_ENTITIES = new LinkedHashSet<EntityPlayer>();
    private static final Map<String, Integer> HOME_WARNING_COOLDOWNS = new HashMap<String, Integer>();

    private static World lastWorld;
    private static int myColor = -1;
    private static String invisWarningTeam;
    private static int invisWarningDistance;
    private static int invisWarningTicks;
    private static boolean homeSet;
    private static int homeX;
    private static int homeY;
    private static int homeZ;
    private static int homeTeamColor = -1;
    private static String homeWarningTeam;
    private static int homeWarningTicks;
    private static int homeWarningDistance;
    private static final Set<String> HOME_WARNING_TEAMS = new LinkedHashSet<String>();
    private static final Set<String> HOME_NEARBY_TEAMS = new LinkedHashSet<String>();
    private static final Set<String> HOME_INVIS_WARNING_TEAMS = new LinkedHashSet<String>();
    private static int homeNearbyDistance;
    private static int homeInvisWarningDistance;
    private static int homeStayWarningTicks;
    private static int homeStayWarningDistance;
    private static final Set<String> HOME_STAY_WARNING_TEAMS = new LinkedHashSet<String>();
    private static int invisCounter;
    private static int homeCounter;

    private HomeAlert() {
    }

    public static void tick() {
        World world = mc.theWorld;
        EntityPlayer player = mc.thePlayer;
        tickHomeCooldowns();

        if (world == null || player == null) {
            clearAll();
            lastWorld = world;
            return;
        }

        if (lastWorld != null && lastWorld != world) {
            clearAll();
        }
        lastWorld = world;

        tickWarnings();

        int helmetColor = helmetTeamColor(player);
        myColor = helmetColor == -1 ? playerColor(player) : helmetColor;

        if (++invisCounter >= 5) {
            invisCounter = 0;
            scanInvisibleEnemies(world, player);
        }

        int homeScanInterval = homeSet ? 1 : HOME_SCAN_INTERVAL_TICKS;
        if (++homeCounter >= homeScanInterval) {
            homeCounter = 0;
            updateHomeAndScanEnemies(world, player, helmetColor);
        }
    }

    public static void render() {
        FontRenderer font = mc.fontRendererObj;
        if (font == null || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        if (invisWarningTeam != null && invisWarningTicks > 0) {
            drawInvisWarning(font, invisWarningTeam, invisWarningDistance, width, height / 2.0F - 72.0F);
        }

        if (!HOME_INVIS_WARNING_TEAMS.isEmpty() && HOME_NEARBY_TEAMS.size() > 1) {
            drawHomeMultiWarning(font, width, height / 2.0F + 4.0F);
        } else if (!HOME_INVIS_WARNING_TEAMS.isEmpty()) {
            drawHomeInvisWarning(font, HOME_INVIS_WARNING_TEAMS, homeInvisWarningDistance, width, height / 2.0F + 4.0F);
        } else if (!HOME_STAY_WARNING_TEAMS.isEmpty() && homeStayWarningTicks > 0) {
            drawHomeStayWarning(font, HOME_STAY_WARNING_TEAMS, homeStayWarningDistance, width, height / 2.0F + 36.0F);
        } else if (!HOME_WARNING_TEAMS.isEmpty() && homeWarningTicks > 0) {
            drawHomeWarning(font, HOME_WARNING_TEAMS, homeWarningDistance, width, height / 2.0F + 36.0F);
        }
    }

    private static void tickWarnings() {
        if (invisWarningTicks > 0) {
            invisWarningTicks--;
            if (invisWarningTicks == 0) {
                invisWarningTeam = null;
                invisWarningDistance = 0;
            }
        }
        if (homeWarningTicks > 0) {
            homeWarningTicks--;
            if (homeWarningTicks == 0) {
                homeWarningTeam = null;
                HOME_WARNING_TEAMS.clear();
                homeWarningDistance = 0;
            }
        }
        if (homeStayWarningTicks > 0) {
            homeStayWarningTicks--;
            if (homeStayWarningTicks == 0) {
                HOME_STAY_WARNING_TEAMS.clear();
                homeStayWarningDistance = 0;
            }
        }
    }

    private static void scanInvisibleEnemies(World world, EntityPlayer player) {
        if (myColor == -1) {
            return;
        }

        String nearestTeam = null;
        int nearestDistance = Integer.MAX_VALUE;
        boolean newWarning = false;
        Set<String> seenTeams = new LinkedHashSet<String>();
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityPlayer) || object == player) {
                continue;
            }
            EntityPlayer entity = (EntityPlayer) object;
            if (!entity.isInvisible() || invisibleTooHighForHome(entity)) {
                continue;
            }

            int color = playerColor(entity);
            if (color == -1 || color == myColor) {
                continue;
            }

            int distance = distanceBetween(player, entity);
            if (distance > INVIS_ALERT_MAX_DISTANCE) {
                continue;
            }

            String team = teamName(color);
            seenTeams.add(team);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTeam = team;
            }

            if (!INVIS_ACTIVE_TEAMS.contains(team)) {
                INVIS_ACTIVE_TEAMS.add(team);
                newWarning = true;
            }
        }
        INVIS_ACTIVE_TEAMS.retainAll(seenTeams);

        if (nearestTeam != null) {
            if (invisWarningTicks > 0 || newWarning) {
                invisWarningTeam = nearestTeam;
                invisWarningDistance = nearestDistance;
            }
            if (newWarning && invisWarningTicks <= 0) {
                invisWarningTicks = INVIS_WARNING_TICKS;
            }
        }
    }

    private static void updateHomeAndScanEnemies(World world, EntityPlayer player, int helmetColor) {
        if (helmetColor != -1 && (!homeSet || homeTeamColor != helmetColor)) {
            BlockPos bed = findNearestBed(world, player);
            if (bed != null) {
                homeSet = true;
                homeX = bed.getX();
                homeY = bed.getY();
                homeZ = bed.getZ();
                homeTeamColor = helmetColor;
            }
        }

        if (!homeSet || myColor == -1) {
            HOME_NEARBY_TEAMS.clear();
            HOME_INVIS_WARNING_TEAMS.clear();
            HOME_STAY_WARNING_TEAMS.clear();
            homeNearbyDistance = 0;
            homeInvisWarningDistance = 0;
            homeStayWarningDistance = 0;
            return;
        }

        int nearestHomeDistance = Integer.MAX_VALUE;
        boolean newHomeWarning = false;
        Set<EntityPlayer> seenVisibleHomeEntities = new LinkedHashSet<EntityPlayer>();
        Set<String> seenAllHomeTeams = new LinkedHashSet<String>();
        int nearestHomeInvisDistance = Integer.MAX_VALUE;
        Set<String> seenHomeInvisTeams = new LinkedHashSet<String>();
        int nearestStayDistance = Integer.MAX_VALUE;
        boolean newStayWarning = false;
        Set<String> stayWarningTeams = new LinkedHashSet<String>();

        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityPlayer) || object == player) {
                continue;
            }
            EntityPlayer entity = (EntityPlayer) object;

            int color = playerColor(entity);
            if (color == -1 || color == myColor) {
                continue;
            }

            int distance = distanceTo(entity, homeX + 0.5D, homeY + 0.5D, homeZ + 0.5D);
            if (distance > HOME_ALERT_MAX_DISTANCE) {
                continue;
            }

            String team = teamName(color);
            boolean invisible = entity.isInvisible();
            if (invisible && invisibleTooHighForHome(entity)) {
                continue;
            }

            seenAllHomeTeams.add(team);
            if (distance < nearestHomeDistance) {
                nearestHomeDistance = distance;
            }
            if (invisible) {
                seenHomeInvisTeams.add(team);
                if (distance < nearestHomeInvisDistance) {
                    nearestHomeInvisDistance = distance;
                }
                continue;
            }

            seenVisibleHomeEntities.add(entity);
            int stayTicks = getHomeStayTicks(entity) + 1;
            HOME_ENTITY_STAY_TICKS.put(entity, Integer.valueOf(stayTicks));
            if (stayTicks >= HOME_STAY_TRIGGER_TICKS) {
                stayWarningTeams.add(team);
                int playerDistance = distanceBetween(player, entity);
                if (playerDistance < nearestStayDistance) {
                    nearestStayDistance = playerDistance;
                }
                if (!HOME_STAY_ALERTED_ENTITIES.contains(entity)) {
                    HOME_STAY_ALERTED_ENTITIES.add(entity);
                    newStayWarning = true;
                }
            }

            if (!HOME_ACTIVE_ENTITIES.contains(entity)
                    && (homeWarningTicks > 0 || homeCooldownReady(team))) {
                HOME_ACTIVE_ENTITIES.add(entity);
                startHomeCooldown(team);
                newHomeWarning = true;
            }
        }

        HOME_ACTIVE_ENTITIES.retainAll(seenVisibleHomeEntities);
        retainHomeEntityMap(HOME_ENTITY_STAY_TICKS, seenVisibleHomeEntities);
        HOME_STAY_ALERTED_ENTITIES.retainAll(seenVisibleHomeEntities);

        HOME_NEARBY_TEAMS.clear();
        HOME_NEARBY_TEAMS.addAll(seenAllHomeTeams);
        homeNearbyDistance = nearestHomeDistance == Integer.MAX_VALUE ? 0 : nearestHomeDistance;

        HOME_INVIS_WARNING_TEAMS.clear();
        HOME_INVIS_WARNING_TEAMS.addAll(seenHomeInvisTeams);
        homeInvisWarningDistance = nearestHomeInvisDistance == Integer.MAX_VALUE ? 0 : nearestHomeInvisDistance;

        if (homeStayWarningTicks > 0 && !stayWarningTeams.isEmpty()) {
            HOME_STAY_WARNING_TEAMS.clear();
            HOME_STAY_WARNING_TEAMS.addAll(stayWarningTeams);
            homeStayWarningDistance = nearestStayDistance == Integer.MAX_VALUE ? 0 : nearestStayDistance;
        } else if (homeStayWarningTicks > 0) {
            HOME_STAY_WARNING_TEAMS.clear();
            homeStayWarningDistance = 0;
            homeStayWarningTicks = 0;
        }

        if (newStayWarning && !stayWarningTeams.isEmpty()) {
            HOME_STAY_WARNING_TEAMS.clear();
            HOME_STAY_WARNING_TEAMS.addAll(stayWarningTeams);
            homeStayWarningDistance = nearestStayDistance == Integer.MAX_VALUE ? 0 : nearestStayDistance;
            homeStayWarningTicks = HOME_STAY_WARNING_TICKS;
        }

        if (homeWarningTicks > 0 && !seenAllHomeTeams.isEmpty()) {
            HOME_WARNING_TEAMS.clear();
            HOME_WARNING_TEAMS.addAll(seenAllHomeTeams);
            homeWarningDistance = homeNearbyDistance;
            homeWarningTeam = firstTeam(seenAllHomeTeams);
        }

        if (newHomeWarning && !seenAllHomeTeams.isEmpty()) {
            HOME_WARNING_TEAMS.clear();
            HOME_WARNING_TEAMS.addAll(seenAllHomeTeams);
            homeWarningDistance = homeNearbyDistance;
            homeWarningTeam = firstTeam(seenAllHomeTeams);
            homeWarningTicks = HOME_WARNING_TICKS;
        }
    }

    private static BlockPos findNearestBed(World world, EntityPlayer player) {
        BlockPos center = player.getPosition();
        BlockPos nearest = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (int dx = -HOME_BED_SCAN_RADIUS; dx <= HOME_BED_SCAN_RADIUS; dx++) {
            for (int dy = -HOME_BED_VERTICAL_RADIUS; dy <= HOME_BED_VERTICAL_RADIUS; dy++) {
                for (int dz = -HOME_BED_SCAN_RADIUS; dz <= HOME_BED_SCAN_RADIUS; dz++) {
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq >= nearestDistanceSq) {
                        continue;
                    }

                    BlockPos pos = center.add(dx, dy, dz);
                    Block block = world.getBlockState(pos).getBlock();
                    if (block != Blocks.bed) {
                        continue;
                    }

                    nearestDistanceSq = distanceSq;
                    nearest = pos;
                }
            }
        }
        return nearest;
    }

    private static void drawInvisWarning(FontRenderer font, String team, int distance, int width, float y) {
        drawTeamSuffixScaled(font, team, teamChineseName(team), "\u6709\u9690\u8eab\uff0c\u8ddd\u79bb\u4f60" + distance + "\u683c",
                width, y, INVIS_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawHomeInvisWarning(FontRenderer font, Set<String> teams, int distance, int width, float y) {
        drawTeamListScaled(font, teams, "\u9690\u8eab\u8ddd\u79bb" + distance + "\u683c",
                width, y, INVIS_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawHomeWarning(FontRenderer font, Set<String> teams, int distance, int width, float y) {
        drawTeamListScaled(font, teams, "\u6b63\u5728\u9760\u8fd1\u5bb6,\u8ddd\u79bb:" + distance + "\u683c",
                width, y, HOME_WARNING_SCALE, HOME_WARNING_COLOR);
    }

    private static void drawHomeStayWarning(FontRenderer font, Set<String> teams, int distance, int width, float y) {
        drawTeamListScaled(font, teams, "\u5728\u5bb6\u9644\u8fd1,\u8ddd\u79bb\u4f60" + distance + "\u683c",
                width, y, HOME_WARNING_SCALE, HOME_WARNING_COLOR);
    }

    private static void drawHomeMultiWarning(FontRenderer font, int width, float y) {
        float lineGap = 11.0F * HOME_MULTI_WARNING_SCALE;
        drawTeamListScaled(font, HOME_NEARBY_TEAMS, "\u6b63\u5728\u9760\u8fd1\u5bb6,\u8ddd\u79bb:" + homeNearbyDistance + "\u683c",
                width, y, HOME_MULTI_WARNING_SCALE, INVIS_WARNING_COLOR);
        drawTeamListScaled(font, HOME_INVIS_WARNING_TEAMS, "\u9690\u8eab\u8ddd\u79bb" + homeInvisWarningDistance + "\u683c",
                width, y + lineGap, HOME_MULTI_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawTeamSuffixScaled(FontRenderer font, String team, String prefix, String suffix,
                                             int width, float y, float scale, int suffixColor) {
        int prefixWidth = font.getStringWidth(prefix);
        int suffixWidth = font.getStringWidth(suffix);
        float x = (width - (prefixWidth + suffixWidth) * scale) / 2.0F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        float scaledX = x / scale;
        float scaledY = y / scale;
        font.drawStringWithShadow(prefix, scaledX, scaledY, teamDisplayColor(team));
        font.drawStringWithShadow(suffix, scaledX + prefixWidth, scaledY, suffixColor);
        GlStateManager.popMatrix();
    }

    private static void drawTeamListScaled(FontRenderer font, Set<String> teams, String suffix,
                                           int width, float y, float scale, int suffixColor) {
        String separator = ",";
        int totalWidth = 0;
        boolean first = true;
        for (String team : teams) {
            if (!first) {
                totalWidth += font.getStringWidth(separator);
            }
            totalWidth += font.getStringWidth(teamChineseName(team));
            first = false;
        }
        totalWidth += font.getStringWidth(suffix);

        float x = (width - totalWidth * scale) / 2.0F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        float cursor = x / scale;
        float scaledY = y / scale;
        first = true;
        for (String team : teams) {
            if (!first) {
                font.drawStringWithShadow(separator, cursor, scaledY, suffixColor);
                cursor += font.getStringWidth(separator);
            }
            String name = teamChineseName(team);
            font.drawStringWithShadow(name, cursor, scaledY, teamDisplayColor(team));
            cursor += font.getStringWidth(name);
            first = false;
        }
        font.drawStringWithShadow(suffix, cursor, scaledY, suffixColor);
        GlStateManager.popMatrix();
    }

    private static int playerColor(EntityPlayer player) {
        int helmetColor = helmetTeamColor(player);
        if (helmetColor != -1) {
            return helmetColor;
        }
        int fromDisplayName = displayNameTeamColor(player);
        return fromDisplayName == -1 ? scoreboardTeamColor(player) : fromDisplayName;
    }

    private static int helmetTeamColor(EntityPlayer player) {
        ItemStack helmet = player.getCurrentArmor(3);
        if (helmet != null && helmet.hasTagCompound()) {
            NBTTagCompound display = helmet.getSubCompound("display", false);
            if (display != null && display.hasKey("color")) {
                return teamColor(teamName(display.getInteger("color")));
            }
        }
        return -1;
    }

    private static int displayNameTeamColor(EntityPlayer player) {
        try {
            if (player.getDisplayName() == null) {
                return -1;
            }
            return colorFromFormattedText(player.getDisplayName().getFormattedText());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int scoreboardTeamColor(EntityPlayer player) {
        try {
            Team team = player.getTeam();
            if (team == null) {
                return -1;
            }
            return colorFromFormattedText(team.getColorPrefix());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int colorFromFormattedText(String text) {
        if (text == null) {
            return -1;
        }
        for (int i = 0; i + 1 < text.length(); i++) {
            char marker = text.charAt(i);
            if (marker != '\u00a7' && marker != '&') {
                continue;
            }
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'c' || code == '4') {
                return teamColor("red");
            }
            if (code == '9' || code == '1' || code == 'b') {
                return teamColor("blue");
            }
            if (code == 'e' || code == '6') {
                return teamColor("yellow");
            }
            if (code == 'a' || code == '2') {
                return teamColor("green");
            }
        }
        return -1;
    }

    private static String teamName(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        if (r > 180 && g > 140 && b < 120) {
            return "yellow";
        }
        if (b >= r && b >= g) {
            return "blue";
        }
        if (g >= r && g >= b) {
            return "green";
        }
        return "red";
    }

    private static int teamColor(String team) {
        if ("blue".equals(team)) {
            return 0x5555FF;
        }
        if ("yellow".equals(team)) {
            return 0xFFFF55;
        }
        if ("green".equals(team)) {
            return 0x55FF55;
        }
        return 0xFF5555;
    }

    private static int teamDisplayColor(String team) {
        return teamColor(team);
    }

    private static String teamChineseName(String team) {
        if ("blue".equals(team)) {
            return "\u84dd\u961f";
        }
        if ("yellow".equals(team)) {
            return "\u9ec4\u961f";
        }
        if ("green".equals(team)) {
            return "\u7eff\u961f";
        }
        return "\u7ea2\u961f";
    }

    private static boolean invisibleTooHighForHome(EntityPlayer entity) {
        return homeSet && entity.posY > homeY + HOME_INVIS_MAX_HEIGHT_ABOVE_HOME;
    }

    private static int distanceBetween(EntityPlayer a, EntityPlayer b) {
        return (int) Math.round(a.getDistanceToEntity(b));
    }

    private static int distanceTo(EntityPlayer entity, double x, double y, double z) {
        double dx = entity.posX - x;
        double dy = entity.posY - y;
        double dz = entity.posZ - z;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static int getHomeStayTicks(EntityPlayer entity) {
        Integer ticks = HOME_ENTITY_STAY_TICKS.get(entity);
        return ticks == null ? 0 : ticks.intValue();
    }

    private static void retainHomeEntityMap(Map<EntityPlayer, Integer> values, Set<EntityPlayer> retained) {
        Iterator<EntityPlayer> iterator = values.keySet().iterator();
        while (iterator.hasNext()) {
            if (!retained.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    private static boolean homeCooldownReady(String team) {
        Integer ticks = HOME_WARNING_COOLDOWNS.get(team);
        return ticks == null || ticks.intValue() <= 0;
    }

    private static void startHomeCooldown(String team) {
        HOME_WARNING_COOLDOWNS.put(team, Integer.valueOf(HOME_WARNING_COOLDOWN_TICKS));
    }

    private static String firstTeam(Set<String> teams) {
        Iterator<String> iterator = teams.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static void tickHomeCooldowns() {
        Iterator<Map.Entry<String, Integer>> iterator = HOME_WARNING_COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue().intValue() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(Integer.valueOf(ticksLeft));
            }
        }
    }

    private static void clearAll() {
        myColor = -1;
        invisWarningTeam = null;
        invisWarningDistance = 0;
        invisWarningTicks = 0;
        INVIS_ACTIVE_TEAMS.clear();
        clearHomeState();
    }

    private static void clearHomeState() {
        homeSet = false;
        homeX = 0;
        homeY = 0;
        homeZ = 0;
        homeTeamColor = -1;
        homeWarningTeam = null;
        homeWarningTicks = 0;
        homeWarningDistance = 0;
        HOME_WARNING_TEAMS.clear();
        HOME_NEARBY_TEAMS.clear();
        HOME_INVIS_WARNING_TEAMS.clear();
        homeNearbyDistance = 0;
        homeInvisWarningDistance = 0;
        homeStayWarningTicks = 0;
        homeStayWarningDistance = 0;
        HOME_STAY_WARNING_TEAMS.clear();
        homeCounter = 0;
        HOME_ACTIVE_ENTITIES.clear();
        HOME_ENTITY_STAY_TICKS.clear();
        HOME_STAY_ALERTED_ENTITIES.clear();
        HOME_WARNING_COOLDOWNS.clear();
    }
}
