package com.example.fireballpredictor.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FireballPredictorHooks {
    private static final int MARK_RADIUS = 2;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int EXPLOSION_MEMORY_TICKS = 80;
    private static final int GEN_SCAN_INTERVAL_TICKS = 5;
    private static final int INVIS_SCAN_INTERVAL_TICKS = 5;
    private static final int INVIS_WARNING_TICKS = 200;
    private static final int INVIS_ALERT_MAX_DISTANCE = 50;
    private static final int HOME_SCAN_INTERVAL_TICKS = 20;
    private static final int HOME_WARNING_TICKS = 100;
    private static final int HOME_WARNING_COOLDOWN_TICKS = 100;
    private static final int HOME_STAY_WARNING_TICKS = 100;
    private static final int HOME_STAY_TRIGGER_TICKS = 400;
    private static final int HOME_INVIS_MAX_HEIGHT_ABOVE_HOME = 30;
    private static final int HOME_ALERT_MAX_DISTANCE = 50;
    private static final int HOME_BED_SCAN_RADIUS = 64;
    private static final int HOME_BED_VERTICAL_RADIUS = 16;
    private static final int HYPIXEL_STATS_SCAN_INTERVAL_TICKS = 20;
    private static final int HYPIXEL_BEDWARS_EXP_PER_PRESTIGE = 487000;
    private static final int HYPIXEL_BEDWARS_EASY_EXP = 7000;
    private static final int HYPIXEL_4V4_EXPECTED_ENEMIES = 4;
    private static final double WEAK_ENEMY_MAX_AVERAGE_KD = 0.70D;
    private static final double WEAK_ENEMY_LOW_AVERAGE_STARS = 50.0D;
    private static final double WEAK_ENEMY_LOW_AVERAGE_KILLS_WITH_LOW_STARS = 400.0D;
    private static final double WEAK_ENEMY_MAX_AVERAGE_KILLS = 500.0D;
    private static final double TRACE_DISTANCE = 100.0D;
    private static final String DEFAULT_HYPIXEL_API_KEY = "322cd413-9d3b-486f-84de-9716cca33416";
    private static final String EXTRA_HYPIXEL_API_KEY = "429b2968-de16-4c23-84c4-c6856e8465f8";
    private static final String DEFAULT_HYPIXEL_API_KEYS = DEFAULT_HYPIXEL_API_KEY + "," + EXTRA_HYPIXEL_API_KEY;
    private static final String WARNING_TEXT = "\u6709\u706b\u7130\u5f39\u6765\u88ad";
    private static final String WEAK_ENEMY_WARNING_TEXT = "\u5bf9\u9762\u662f\u5c0f\u5446\u5446";
    private static final float INVIS_WARNING_SCALE = 2.0F;
    private static final int INVIS_WARNING_COLOR = 0x00BFFF;
    private static final float HOME_WARNING_SCALE = 2.0F;
    private static final float HOME_MULTI_WARNING_SCALE = 1.5F;
    private static final int HOME_WARNING_COLOR = 0x00FFFF;
    private static final float WARNING_SCALE = 3.25F;
    private static final int HUD_WHITE = 0xFFFFFF;
    private static final int HUD_DIAMOND = 0x00DBFF;
    private static final int HUD_EMERALD = 0x00FF51;

    private static final Map<Object, Object> REPLACED = new HashMap<Object, Object>();
    private static final Map<Object, Integer> RECENT_EXPLOSIONS = new HashMap<Object, Integer>();
    private static final List<GenInfo> DIAMOND_GENS = new ArrayList<GenInfo>();
    private static final List<GenInfo> EMERALD_GENS = new ArrayList<GenInfo>();
    private static final Set<String> INVIS_ACTIVE_TEAMS = new LinkedHashSet<String>();
    private static final Set<Object> HOME_ACTIVE_ENTITIES = new LinkedHashSet<Object>();
    private static final Map<Object, Integer> HOME_ENTITY_STAY_TICKS = new HashMap<Object, Integer>();
    private static final Set<Object> HOME_STAY_ALERTED_ENTITIES = new LinkedHashSet<Object>();
    private static final Map<String, Integer> HOME_WARNING_COOLDOWNS = new HashMap<String, Integer>();

    private static Reflection r;
    private static Object lastWorld;
    private static Object miningBlockPos;
    private static boolean incomingFireball;
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
    private static int tickCounter;
    private static int refreshCounter;
    private static int genCounter;
    private static int invisCounter;
    private static int homeCounter;
    private static int hypixelStatsCounter;
    private static boolean tickBusy;
    private static boolean renderBusy;
    private static boolean worldRenderBusy;
    private static boolean weakEnemyWarningActive;
    private static Object hypixelStatsWorld;
    private static String hypixelStatsGameKey;
    private static boolean hypixelStatsHeaderPosted;
    private static boolean hypixelStatsQueryStarted;
    private static int hypixelStatsDetectedTicks;
    private static int hypixelStatsApiKeyIndex;
    private static HypixelStatsConfig hypixelStatsConfig;
    private static boolean hypixelStatsCacheLoaded;
    private static final List<PlayerSnapshot> HYPIXEL_STATS_GAME_ENEMIES = new ArrayList<PlayerSnapshot>();
    private static final Map<String, HypixelStatsEntry> HYPIXEL_STATS_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, HypixelStatsEntry>());
    private static final Set<String> HYPIXEL_STATS_REQUESTED =
            Collections.synchronizedSet(new LinkedHashSet<String>());
    private static final ExecutorService HYPIXEL_STATS_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "fireballpredictor-hypixel-stats");
            thread.setDaemon(true);
            return thread;
        }
    });

    private FireballPredictorHooks() {
    }

    public static void onClientTick() {
        if (tickBusy) {
            return;
        }
        tickBusy = true;
        try {
            Reflection ref = reflection();
            Object mc = ref.getMinecraft();
            Object world = ref.mcWorld.get(mc);
            Object player = ref.mcPlayer.get(mc);
            tickRecentExplosions();
            tickHomeCooldowns();

            if (world == null || player == null) {
                incomingFireball = false;
                tickCounter = 0;
                refreshCounter = 0;
                genCounter = 0;
                invisCounter = 0;
                homeCounter = 0;
                hypixelStatsCounter = 0;
                myColor = -1;
                invisWarningTeam = null;
                invisWarningDistance = 0;
                invisWarningTicks = 0;
                clearHypixelStatsState();
                clearHomeState();
                DIAMOND_GENS.clear();
                EMERALD_GENS.clear();
                INVIS_ACTIVE_TEAMS.clear();
                HOME_ACTIVE_ENTITIES.clear();
                restoreAll(lastWorld);
                lastWorld = world;
                return;
            }

            if (lastWorld != null && lastWorld != world) {
                clearHomeState();
                clearHypixelStatsState();
                INVIS_ACTIVE_TEAMS.clear();
                HOME_ACTIVE_ENTITIES.clear();
            }
            lastWorld = world;
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
            int helmetColor = helmetTeamColor(ref, player);
            myColor = helmetColor == -1 ? playerColor(ref, player) : helmetColor;
            if (++genCounter >= GEN_SCAN_INTERVAL_TICKS) {
                genCounter = 0;
                scanGenerators(ref, world, player);
            }
            if (++invisCounter >= INVIS_SCAN_INTERVAL_TICKS) {
                invisCounter = 0;
                scanInvisibleEnemies(ref, world, player);
            }
            int homeScanInterval = homeSet ? 1 : HOME_SCAN_INTERVAL_TICKS;
            if (++homeCounter >= homeScanInterval) {
                homeCounter = 0;
                updateHomeAndScanEnemies(ref, world, player, helmetColor);
            }
            if (++refreshCounter >= REFRESH_INTERVAL_TICKS) {
                refreshCounter = 0;
                refreshMarkers(ref, world);
            }
            if (++hypixelStatsCounter >= HYPIXEL_STATS_SCAN_INTERVAL_TICKS) {
                hypixelStatsCounter = 0;
                scanHypixelStats(ref, mc, world, player);
            }
            updateMiningBlock(ref, mc, world);

            if (++tickCounter < 5) {
                return;
            }
            tickCounter = 0;

            incomingFireball = false;
            Set<Object> hits = new LinkedHashSet<Object>();
            collectExistingFireballs(ref, world, hits);
            collectHeldFireCharges(ref, world, hits);

            if (hits.isEmpty()) {
                restoreMarkers(ref, world);
                return;
            }

            restoreMarkers(ref, world);
            for (Object hit : hits) {
                markArea(ref, world, hit);
            }
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("tick hook error: " + t + " " + t.getMessage());
            System.err.println("[FireballPredictor] Tick hook error: " + t);
        } finally {
            tickBusy = false;
        }
    }

    public static void onExplosionPacket(Object packet) {
        try {
            Reflection ref = reflection();
            if (packet == null || !ref.explosionPacketClass.isInstance(packet)) {
                return;
            }

            List<?> positions = (List<?>) ref.explosionAffectedBlocks.invoke(packet);
            for (Object pos : positions) {
                RECENT_EXPLOSIONS.put(pos, EXPLOSION_MEMORY_TICKS);
            }
            FireballPredictorAgentLog.write("recorded explosion blocks: " + positions.size());
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("explosion hook error: " + t + " " + t.getMessage());
        }
    }

    public static void onRenderOverlay() {
        if (renderBusy) {
            return;
        }
        renderBusy = true;
        try {
            Reflection ref = reflection();
            Object mc = ref.getMinecraft();
            if (ref.mcWorld.get(mc) == null || ref.mcPlayer.get(mc) == null) {
                return;
            }

            Object font = ref.mcFontRenderer.get(mc);
            if (font == null) {
                return;
            }

            int y = 5;
            y = drawGenLine(ref, font, "\u94bb\u77f3\u8d44\u6e90\u70b9\uff1a ", DIAMOND_GENS, 5, y, HUD_DIAMOND);
            y = drawGenLine(ref, font, "\u7eff\u5b9d\u77f3\u8d44\u6e90\u70b9\uff1a ", EMERALD_GENS, 5, y, HUD_EMERALD);

            Object scaledResolution = null;
            int width = 0;
            int height = 0;
            if (invisWarningTeam != null && invisWarningTicks > 0) {
                scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                drawInvisWarning(ref, font, invisWarningTeam, invisWarningDistance, width, height / 2.0F - 72.0F);
            }

            if (!HOME_INVIS_WARNING_TEAMS.isEmpty() && HOME_NEARBY_TEAMS.size() > 1) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawHomeMultiWarning(ref, font, width, height / 2.0F + 4.0F);
            } else if (!HOME_INVIS_WARNING_TEAMS.isEmpty()) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawHomeInvisWarning(ref, font, HOME_INVIS_WARNING_TEAMS, homeInvisWarningDistance, width, height / 2.0F + 4.0F);
            } else if (!HOME_STAY_WARNING_TEAMS.isEmpty() && homeStayWarningTicks > 0) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawHomeStayWarning(ref, font, HOME_STAY_WARNING_TEAMS, homeStayWarningDistance, width, height / 2.0F + 36.0F);
            } else if (!HOME_WARNING_TEAMS.isEmpty() && homeWarningTicks > 0) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawHomeWarning(ref, font, HOME_WARNING_TEAMS, homeWarningDistance, width, height / 2.0F + 36.0F);
            }

            if (incomingFireball) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawCenteredScaled(ref, font, WARNING_TEXT, width, height / 2.0F - 36.0F,
                        WARNING_SCALE, 0xFF3333);
            }
            if (weakEnemyWarningActive) {
                if (scaledResolution == null) {
                    scaledResolution = ref.scaledResolutionCtor.newInstance(mc);
                    width = ((Integer) ref.scaledWidth.invoke(scaledResolution)).intValue();
                    height = ((Integer) ref.scaledHeight.invoke(scaledResolution)).intValue();
                }
                drawCenteredScaled(ref, font, WEAK_ENEMY_WARNING_TEXT, width,
                        incomingFireball ? height / 2.0F + 4.0F : height / 2.0F - 36.0F,
                        WARNING_SCALE, 0x00BFFF);
            }
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("overlay hook error: " + t + " " + t.getMessage());
            System.err.println("[FireballPredictor] Overlay hook error: " + t);
        } finally {
            renderBusy = false;
        }
    }

    public static void onRenderWorld(float partialTicks) {
        if (worldRenderBusy) {
            return;
        }
        worldRenderBusy = true;
        try {
            Reflection ref = reflection();
            Object mc = ref.getMinecraft();
            Object world = ref.mcWorld.get(mc);
            Object player = ref.mcPlayer.get(mc);
            if (world == null || player == null) {
                return;
            }

            Object camera = ref.mcRenderViewEntity.invoke(mc);
            if (camera == null) {
                camera = player;
            }
            double cameraX = interpolate(ref.entityLastTickPosX.getDouble(camera), ref.entityPosX.getDouble(camera), partialTicks);
            double cameraY = interpolate(ref.entityLastTickPosY.getDouble(camera), ref.entityPosY.getDouble(camera), partialTicks);
            double cameraZ = interpolate(ref.entityLastTickPosZ.getDouble(camera), ref.entityPosZ.getDouble(camera), partialTicks);

            ref.glPush.invoke(null);
            ref.glDisableTexture.invoke(null);
            ref.glEnableBlend.invoke(null);
            ref.glBlendFunc.invoke(null, 770, 771, 1, 0);
            ref.glDisableDepth.invoke(null);
            try {
                List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
                for (Object entity : entities) {
                    if (!ref.playerClass.isInstance(entity) || entity == player) {
                        continue;
                    }

                    int color = playerColor(ref, entity);
                    if (myColor == -1 || color == -1) {
                        ref.glColor.invoke(null, 1.0F, 1.0F, 1.0F, 1.0F);
                    } else if (color == myColor) {
                        ref.glColor.invoke(null, 0.0F, 1.0F, 0.0F, 1.0F);
                    } else {
                        ref.glColor.invoke(null, 1.0F, 0.0F, 0.0F, 1.0F);
                    }

                    Object box = ref.entityBoundingBox.invoke(entity);
                    Object shifted = ref.aabbOffset.invoke(box, -cameraX, -cameraY, -cameraZ);
                    ref.renderDrawSelection.invoke(null, shifted);
                }
            } finally {
                ref.glEnableDepth.invoke(null);
                ref.glDisableBlend.invoke(null);
                ref.glEnableTexture.invoke(null);
                ref.glPop.invoke(null);
            }
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("world render hook error: " + t + " " + t.getMessage());
            System.err.println("[FireballPredictor] World render hook error: " + t);
        } finally {
            worldRenderBusy = false;
        }
    }

    private static int drawGenLine(Reflection ref, Object font, String label, List<GenInfo> gens, int x, int y, int color) throws Exception {
        ref.fontDrawShadow.invoke(font, label, (float) x, (float) y, color);
        int labelWidth = ((Integer) ref.fontWidth.invoke(font, label)).intValue();
        ref.fontDrawShadow.invoke(font, genHudString(gens), (float) (x + labelWidth), (float) y, HUD_WHITE);
        return y + 10;
    }

    private static void drawCenteredScaled(Reflection ref, Object font, String text, int width, float y, float scale, int color) throws Exception {
        int textWidth = ((Integer) ref.fontWidth.invoke(font, text)).intValue();
        float x = (width - textWidth * scale) / 2.0F;
        ref.glPush.invoke(null);
        ref.glScale.invoke(null, scale, scale, 1.0F);
        ref.fontDrawShadow.invoke(font, text, x / scale, y / scale, color);
        ref.glPop.invoke(null);
    }

    private static void drawInvisWarning(Reflection ref, Object font, String team, int distance, int width, float y) throws Exception {
        String prefix = teamChineseName(team);
        String suffix = "\u6709\u9690\u8eab\uff0c\u8ddd\u79bb\u4f60" + distance + "\u683c";
        drawTeamSuffixScaled(ref, font, team, prefix, suffix, width, y, INVIS_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawHomeInvisWarning(Reflection ref, Object font, Set<String> teams, int distance, int width, float y) throws Exception {
        drawTeamListScaled(ref, font, teams, "\u9690\u8eab\u8ddd\u79bb" + distance + "\u683c",
                width, y, INVIS_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawHomeWarning(Reflection ref, Object font, Set<String> teams, int distance, int width, float y) throws Exception {
        drawTeamListScaled(ref, font, teams, "\u6b63\u5728\u9760\u8fd1\u5bb6,\u8ddd\u79bb:" + distance + "\u683c",
                width, y, HOME_WARNING_SCALE, HOME_WARNING_COLOR);
    }

    private static void drawHomeStayWarning(Reflection ref, Object font, Set<String> teams, int distance, int width, float y) throws Exception {
        drawTeamListScaled(ref, font, teams, "\u5728\u5bb6\u9644\u8fd1,\u8ddd\u79bb\u4f60" + distance + "\u683c",
                width, y, HOME_WARNING_SCALE, HOME_WARNING_COLOR);
    }

    private static void drawHomeMultiWarning(Reflection ref, Object font, int width, float y) throws Exception {
        float lineGap = 11.0F * HOME_MULTI_WARNING_SCALE;
        drawTeamListScaled(ref, font, HOME_NEARBY_TEAMS, "\u6b63\u5728\u9760\u8fd1\u5bb6,\u8ddd\u79bb:" + homeNearbyDistance + "\u683c",
                width, y, HOME_MULTI_WARNING_SCALE, INVIS_WARNING_COLOR);
        drawTeamListScaled(ref, font, HOME_INVIS_WARNING_TEAMS, "\u9690\u8eab\u8ddd\u79bb" + homeInvisWarningDistance + "\u683c",
                width, y + lineGap, HOME_MULTI_WARNING_SCALE, INVIS_WARNING_COLOR);
    }

    private static void drawTeamSuffixScaled(Reflection ref, Object font, String team, String prefix, String suffix,
                                             int width, float y, float scale, int suffixColor) throws Exception {
        int prefixWidth = ((Integer) ref.fontWidth.invoke(font, prefix)).intValue();
        int suffixWidth = ((Integer) ref.fontWidth.invoke(font, suffix)).intValue();
        float x = (width - (prefixWidth + suffixWidth) * scale) / 2.0F;
        ref.glPush.invoke(null);
        ref.glScale.invoke(null, scale, scale, 1.0F);
        float scaledX = x / scale;
        float scaledY = y / scale;
        ref.fontDrawShadow.invoke(font, prefix, scaledX, scaledY, teamDisplayColor(team));
        ref.fontDrawShadow.invoke(font, suffix, scaledX + prefixWidth, scaledY, suffixColor);
        ref.glPop.invoke(null);
    }

    private static void drawTeamListScaled(Reflection ref, Object font, Set<String> teams, String suffix,
                                           int width, float y, float scale, int suffixColor) throws Exception {
        String separator = ",";
        int totalWidth = 0;
        boolean first = true;
        for (String team : teams) {
            if (!first) {
                totalWidth += ((Integer) ref.fontWidth.invoke(font, separator)).intValue();
            }
            totalWidth += ((Integer) ref.fontWidth.invoke(font, teamChineseName(team))).intValue();
            first = false;
        }
        totalWidth += ((Integer) ref.fontWidth.invoke(font, suffix)).intValue();

        float x = (width - totalWidth * scale) / 2.0F;
        ref.glPush.invoke(null);
        ref.glScale.invoke(null, scale, scale, 1.0F);
        float cursor = x / scale;
        float scaledY = y / scale;
        first = true;
        for (String team : teams) {
            if (!first) {
                ref.fontDrawShadow.invoke(font, separator, cursor, scaledY, suffixColor);
                cursor += ((Integer) ref.fontWidth.invoke(font, separator)).intValue();
            }
            String name = teamChineseName(team);
            ref.fontDrawShadow.invoke(font, name, cursor, scaledY, teamDisplayColor(team));
            cursor += ((Integer) ref.fontWidth.invoke(font, name)).intValue();
            first = false;
        }
        ref.fontDrawShadow.invoke(font, suffix, cursor, scaledY, suffixColor);
        ref.glPop.invoke(null);
    }

    private static void scanGenerators(Reflection ref, Object world, Object player) throws Exception {
        DIAMOND_GENS.clear();
        EMERALD_GENS.clear();

        List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
        for (Object entity : entities) {
            if (!ref.entityItemClass.isInstance(entity)) {
                continue;
            }

            Object stack = ref.entityItemStack.invoke(entity);
            if (stack == null) {
                continue;
            }
            Object item = ref.itemStackItem.invoke(stack);
            List<GenInfo> targetList = null;
            Object expectedBlock = null;
            if (item == ref.diamondItem) {
                targetList = DIAMOND_GENS;
                expectedBlock = ref.diamondBlock;
            } else if (item == ref.emeraldItem) {
                targetList = EMERALD_GENS;
                expectedBlock = ref.emeraldBlock;
            }
            if (targetList == null) {
                continue;
            }

            Object blockBelowPos = ref.blockPosCtorDdd.newInstance(
                    ref.entityPosX.getDouble(entity),
                    ref.entityPosY.getDouble(entity) - 1.0D,
                    ref.entityPosZ.getDouble(entity));
            Object belowState = ref.worldBlockState.invoke(world, blockBelowPos);
            Object belowBlock = ref.blockFromState.invoke(belowState);
            if (belowBlock != expectedBlock) {
                continue;
            }

            int x = floor(ref.entityPosX.getDouble(entity));
            int y = floor(ref.entityPosY.getDouble(entity));
            int z = floor(ref.entityPosZ.getDouble(entity));
            int amount = ref.itemStackSize.getInt(stack);
            GenInfo existing = findGen(targetList, x, y, z);
            if (existing != null) {
                existing.amount += amount;
            } else {
                targetList.add(new GenInfo(
                        x,
                        y,
                        z,
                        amount,
                        distanceTo(ref, player, x + 0.5D, y + 0.5D, z + 0.5D),
                        isLookingAt(ref, player, x + 0.5D, y + 0.5D, z + 0.5D)));
            }
        }
    }

    private static void scanInvisibleEnemies(Reflection ref, Object world, Object player) throws Exception {
        if (myColor == -1) {
            return;
        }

        String nearestTeam = null;
        int nearestDistance = Integer.MAX_VALUE;
        boolean newWarning = false;
        Set<String> seenTeams = new LinkedHashSet<String>();
        List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
        for (Object entity : entities) {
            if (!ref.playerClass.isInstance(entity) || entity == player) {
                continue;
            }
            if (!((Boolean) ref.entityIsInvisible.invoke(entity)).booleanValue()) {
                continue;
            }
            if (invisibleTooHighForHome(ref, entity)) {
                continue;
            }

            int color = playerColor(ref, entity);
            if (color == -1 || color == myColor) {
                continue;
            }

            int distance = distanceBetween(ref, player, entity);
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

    private static void updateHomeAndScanEnemies(Reflection ref, Object world, Object player, int helmetColor) throws Exception {
        if (helmetColor != -1 && (!homeSet || homeTeamColor != helmetColor)) {
            BedPos bed = findNearestBed(ref, world, player);
            if (bed != null) {
                homeSet = true;
                homeX = bed.x;
                homeY = bed.y;
                homeZ = bed.z;
                homeTeamColor = helmetColor;
                FireballPredictorAgentLog.write("home bed recorded at " + homeX + "," + homeY + "," + homeZ);
                hypixelStatsCounter = HYPIXEL_STATS_SCAN_INTERVAL_TICKS;
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
        Set<String> seenVisibleHomeTeams = new LinkedHashSet<String>();
        Set<Object> seenVisibleHomeEntities = new LinkedHashSet<Object>();
        Set<String> seenAllHomeTeams = new LinkedHashSet<String>();
        int nearestHomeInvisDistance = Integer.MAX_VALUE;
        Set<String> seenHomeInvisTeams = new LinkedHashSet<String>();
        int nearestStayDistance = Integer.MAX_VALUE;
        boolean newStayWarning = false;
        Set<String> stayWarningTeams = new LinkedHashSet<String>();
        List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
        for (Object entity : entities) {
            if (!ref.playerClass.isInstance(entity) || entity == player) {
                continue;
            }

            int color = playerColor(ref, entity);
            if (color == -1 || color == myColor) {
                continue;
            }

            int distance = distanceTo(ref, entity, homeX + 0.5D, homeY + 0.5D, homeZ + 0.5D);
            if (distance > HOME_ALERT_MAX_DISTANCE) {
                continue;
            }

            String team = teamName(color);
            boolean invisible = ((Boolean) ref.entityIsInvisible.invoke(entity)).booleanValue();
            if (invisible && invisibleTooHighForHome(ref, entity)) {
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

            seenVisibleHomeTeams.add(team);
            seenVisibleHomeEntities.add(entity);
            int stayTicks = getHomeStayTicks(entity) + 1;
            HOME_ENTITY_STAY_TICKS.put(entity, Integer.valueOf(stayTicks));
            if (stayTicks >= HOME_STAY_TRIGGER_TICKS) {
                stayWarningTeams.add(team);
                int playerDistance = distanceBetween(ref, player, entity);
                if (playerDistance < nearestStayDistance) {
                    nearestStayDistance = playerDistance;
                }
                if (!HOME_STAY_ALERTED_ENTITIES.contains(entity)) {
                    HOME_STAY_ALERTED_ENTITIES.add(entity);
                    newStayWarning = true;
                }
            }

            if (!HOME_ACTIVE_ENTITIES.contains(entity)
                    && (homeWarningTicks > 0 || homeCooldownReady(HOME_WARNING_COOLDOWNS, team))) {
                HOME_ACTIVE_ENTITIES.add(entity);
                startHomeCooldown(HOME_WARNING_COOLDOWNS, team);
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

    private static BedPos findNearestBed(Reflection ref, Object world, Object player) throws Exception {
        int px = floor(ref.entityPosX.getDouble(player));
        int py = floor(ref.entityPosY.getDouble(player));
        int pz = floor(ref.entityPosZ.getDouble(player));
        Object center = ref.blockPosCtorDdd.newInstance((double) px, (double) py, (double) pz);
        BedPos nearest = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (int dx = -HOME_BED_SCAN_RADIUS; dx <= HOME_BED_SCAN_RADIUS; dx++) {
            for (int dy = -HOME_BED_VERTICAL_RADIUS; dy <= HOME_BED_VERTICAL_RADIUS; dy++) {
                for (int dz = -HOME_BED_SCAN_RADIUS; dz <= HOME_BED_SCAN_RADIUS; dz++) {
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq >= nearestDistanceSq) {
                        continue;
                    }

                    Object pos = ref.blockPosAdd.invoke(center, dx, dy, dz);
                    Object state = ref.worldBlockState.invoke(world, pos);
                    Object block = ref.blockFromState.invoke(state);
                    if (block != ref.bedBlock) {
                        continue;
                    }

                    nearestDistanceSq = distanceSq;
                    nearest = new BedPos(
                            ((Integer) ref.blockPosGetX.invoke(pos)).intValue(),
                            ((Integer) ref.blockPosGetY.invoke(pos)).intValue(),
                            ((Integer) ref.blockPosGetZ.invoke(pos)).intValue());
                }
            }
        }
        return nearest;
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
        homeCounter = 0;
        HOME_ACTIVE_ENTITIES.clear();
        HOME_ENTITY_STAY_TICKS.clear();
        HOME_STAY_ALERTED_ENTITIES.clear();
        homeStayWarningTicks = 0;
        homeStayWarningDistance = 0;
        HOME_STAY_WARNING_TEAMS.clear();
        HOME_WARNING_COOLDOWNS.clear();
    }

    private static int getHomeStayTicks(Object entity) {
        Integer ticks = HOME_ENTITY_STAY_TICKS.get(entity);
        return ticks == null ? 0 : ticks.intValue();
    }

    private static void retainHomeEntityMap(Map<Object, Integer> values, Set<Object> retained) {
        Iterator<Object> iterator = values.keySet().iterator();
        while (iterator.hasNext()) {
            if (!retained.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    private static boolean invisibleTooHighForHome(Reflection ref, Object entity) throws Exception {
        return homeSet && ref.entityPosY.getDouble(entity) > homeY + HOME_INVIS_MAX_HEIGHT_ABOVE_HOME;
    }

    private static boolean homeCooldownReady(Map<String, Integer> cooldowns, String team) {
        Integer ticks = cooldowns.get(team);
        return ticks == null || ticks.intValue() <= 0;
    }

    private static void startHomeCooldown(Map<String, Integer> cooldowns, String team) {
        cooldowns.put(team, Integer.valueOf(HOME_WARNING_COOLDOWN_TICKS));
    }

    private static String firstTeam(Set<String> teams) {
        Iterator<String> iterator = teams.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static int playerColor(Reflection ref, Object player) throws Exception {
        int helmetColor = helmetTeamColor(ref, player);
        if (helmetColor != -1) {
            return helmetColor;
        }

        int fromDisplayName = displayNameTeamColor(ref, player);
        return fromDisplayName == -1 ? scoreboardTeamColor(ref, player) : fromDisplayName;
    }

    private static int helmetTeamColor(Reflection ref, Object player) throws Exception {
        Object helmet = ref.playerCurrentArmor.invoke(player, 3);
        if (helmet != null && ((Boolean) ref.itemStackHasTag.invoke(helmet)).booleanValue()) {
            Object display = ref.itemStackSubCompound.invoke(helmet, "display", false);
            if (display != null && ((Boolean) ref.nbtHasKey.invoke(display, "color")).booleanValue()) {
                return teamColor(teamName(((Integer) ref.nbtGetInteger.invoke(display, "color")).intValue()));
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
        if ("red".equals(team)) {
            return "\u7ea2\u961f";
        }
        return "\u7ea2\u961f";
    }

    private static int displayNameTeamColor(Reflection ref, Object player) {
        try {
            if (ref.entityDisplayName == null) {
                return -1;
            }
            Object component = ref.entityDisplayName.invoke(player);
            if (component == null) {
                return -1;
            }
            String text;
            if (component instanceof String) {
                text = (String) component;
            } else {
                Method formatted = ref.chatFormattedText;
                if (formatted == null || !formatted.getDeclaringClass().isInstance(component)) {
                    formatted = Reflection.optionalMethod(component.getClass(), new String[]{"getFormattedText", "func_150254_d", "e"});
                }
                text = formatted == null ? String.valueOf(component) : String.valueOf(formatted.invoke(component));
            }
            return colorFromFormattedText(text);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int scoreboardTeamColor(Reflection ref, Object player) {
        try {
            if (ref.playerTeam == null) {
                return -1;
            }
            Object team = ref.playerTeam.invoke(player);
            if (team == null) {
                return -1;
            }
            Method colorPrefix = Reflection.optionalMethod(team.getClass(), new String[]{"getColorPrefix", "func_96668_e", "e"});
            if (colorPrefix != null) {
                int fromPrefix = colorFromFormattedText(String.valueOf(colorPrefix.invoke(team)));
                if (fromPrefix != -1) {
                    return fromPrefix;
                }
            }
            Method chatFormat = Reflection.optionalMethod(team.getClass(), new String[]{"getChatFormat", "func_178775_l", "l"});
            return chatFormat == null ? -1 : colorFromFormattedText(String.valueOf(chatFormat.invoke(team)));
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

    private static int distanceBetween(Reflection ref, Object a, Object b) throws Exception {
        double dx = ref.entityPosX.getDouble(a) - ref.entityPosX.getDouble(b);
        double dy = ref.entityPosY.getDouble(a) - ref.entityPosY.getDouble(b);
        double dz = ref.entityPosZ.getDouble(a) - ref.entityPosZ.getDouble(b);
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static String genHudString(List<GenInfo> gens) {
        StringBuilder builder = new StringBuilder();
        for (GenInfo info : gens) {
            builder.append(info.amount).append(" r=").append(info.distance);
            if (info.looking) {
                builder.append("(looking)");
            }
            builder.append("// ");
        }
        return builder.toString();
    }

    private static GenInfo findGen(List<GenInfo> gens, int x, int y, int z) {
        for (GenInfo info : gens) {
            if (info.x == x && info.y == y && info.z == z) {
                return info;
            }
        }
        return null;
    }

    private static int distanceTo(Reflection ref, Object entity, double x, double y, double z) throws Exception {
        double dx = ref.entityPosX.getDouble(entity) - x;
        double dy = ref.entityPosY.getDouble(entity) - y;
        double dz = ref.entityPosZ.getDouble(entity) - z;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static boolean isLookingAt(Reflection ref, Object player, double x, double y, double z) throws Exception {
        Object look = ref.entityLook.invoke(player, 1.0F);
        double px = ref.entityPosX.getDouble(player);
        double py = ref.entityPosY.getDouble(player) + ((Float) ref.entityEyeHeight.invoke(player)).floatValue();
        double pz = ref.entityPosZ.getDouble(player);
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001D) {
            return false;
        }
        double dot = ref.vecX.getDouble(look) * (dx / len)
                + ref.vecY.getDouble(look) * (dy / len)
                + ref.vecZ.getDouble(look) * (dz / len);
        return dot > 0.97D;
    }

    private static double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static void collectExistingFireballs(Reflection ref, Object world, Set<Object> hits) throws Exception {
        List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
        for (Object entity : entities) {
            if (!ref.fireballClass.isInstance(entity)) {
                continue;
            }

            Vec direction = directionOf(ref, entity);
            if (direction.length() < 0.0001D) {
                continue;
            }

            incomingFireball = true;
            Vec dir = direction.normalize();
            Object start = ref.vec3Ctor.newInstance(
                    ref.entityPosX.getDouble(entity),
                    ref.entityPosY.getDouble(entity),
                    ref.entityPosZ.getDouble(entity));
            Object end = ref.vec3Add.invoke(start,
                    dir.x * TRACE_DISTANCE,
                    dir.y * TRACE_DISTANCE,
                    dir.z * TRACE_DISTANCE);
            Object hit = rayTraceBlock(ref, world, start, end);
            if (hit != null) {
                hits.add(hit);
            }
        }
    }

    private static void collectHeldFireCharges(Reflection ref, Object world, Set<Object> hits) throws Exception {
        List<?> players = (List<?>) ref.worldPlayerEntities.get(world);
        for (Object player : players) {
            Object held = ref.playerHeldItem.invoke(player);
            if (held == null || ref.itemStackItem.invoke(held) != ref.fireChargeItem) {
                continue;
            }

            Object start = ref.entityPositionEyes.invoke(player, 1.0F);
            Object look = ref.entityLook.invoke(player, 1.0F);
            Object end = ref.vec3Add.invoke(start,
                    ref.vecX.getDouble(look) * TRACE_DISTANCE,
                    ref.vecY.getDouble(look) * TRACE_DISTANCE,
                    ref.vecZ.getDouble(look) * TRACE_DISTANCE);
            Object hit = rayTraceBlock(ref, world, start, end);
            if (hit != null) {
                hits.add(hit);
            }
        }
    }

    private static Object rayTraceBlock(Reflection ref, Object world, Object start, Object end) throws Exception {
        Object hit = ref.worldRayTrace.invoke(world, start, end, false, true, false);
        if (hit == null) {
            return null;
        }
        return ref.movingObjectBlockPos.invoke(hit);
    }

    private static Vec directionOf(Reflection ref, Object fireball) throws Exception {
        Vec motion = new Vec(
                ref.entityMotionX.getDouble(fireball),
                ref.entityMotionY.getDouble(fireball),
                ref.entityMotionZ.getDouble(fireball));
        if (motion.length() >= 0.0001D) {
            return motion;
        }
        return new Vec(
                ref.fireballAccelX.getDouble(fireball),
                ref.fireballAccelY.getDouble(fireball),
                ref.fireballAccelZ.getDouble(fireball));
    }

    private static void markArea(Reflection ref, Object world, Object hitPos) throws Exception {
        for (int dx = -MARK_RADIUS; dx <= MARK_RADIUS; dx++) {
            for (int dy = -MARK_RADIUS; dy <= MARK_RADIUS; dy++) {
                for (int dz = -MARK_RADIUS; dz <= MARK_RADIUS; dz++) {
                    Object pos = ref.blockPosAdd.invoke(hitPos, dx, dy, dz);
                    Object current = ref.worldBlockState.invoke(world, pos);
                    Object block = ref.blockFromState.invoke(current);

                    if (pos.equals(miningBlockPos)) {
                        continue;
                    }
                    if (block == ref.stainedGlassBlock && REPLACED.containsKey(pos)) {
                        continue;
                    }
                    if (block == ref.airBlock || block == ref.stainedGlassBlock
                            || !((Boolean) ref.blockIsFullCube.invoke(block)).booleanValue()) {
                        continue;
                    }

                    if (!REPLACED.containsKey(pos)) {
                        REPLACED.put(pos, current);
                    }
                    ref.worldSetBlockState.invoke(world, pos, ref.redGlassState, 3);
                    refreshRenderArea(ref, world, pos);
                }
            }
        }
    }

    private static void updateMiningBlock(Reflection ref, Object mc, Object world) throws Exception {
        miningBlockPos = null;
        Object gameSettings = ref.mcGameSettings.get(mc);
        Object mouseOver = ref.mcObjectMouseOver.get(mc);
        if (gameSettings == null || mouseOver == null) {
            return;
        }

        Object attackKey = ref.gameSettingsAttackKey.get(gameSettings);
        if (attackKey == null || !((Boolean) ref.keyBindingIsDown.invoke(attackKey)).booleanValue()) {
            return;
        }

        Object pos = ref.movingObjectBlockPos.invoke(mouseOver);
        if (pos != null) {
            miningBlockPos = pos;
            restoreMarker(ref, world, pos);
        }
    }

    private static void restoreMarker(Reflection ref, Object world, Object pos) throws Exception {
        if (world == null || pos == null) {
            return;
        }

        Object original = REPLACED.remove(pos);
        if (original == null) {
            return;
        }

        Object current = ref.worldBlockState.invoke(world, pos);
        Object block = ref.blockFromState.invoke(current);
        if (block == ref.stainedGlassBlock) {
            Object restored = wasRecentlyExploded(pos) ? ref.airState : original;
            ref.worldSetBlockState.invoke(world, pos, restored, 2);
            refreshRenderArea(ref, world, pos);
        }
    }

    private static void refreshMarkers(Reflection ref, Object world) throws Exception {
        Iterator<Map.Entry<Object, Object>> iterator = REPLACED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            Object pos = entry.getKey();
            Object current = ref.worldBlockState.invoke(world, entry.getKey());
            Object block = ref.blockFromState.invoke(current);
            if (block == ref.stainedGlassBlock) {
                refreshRenderArea(ref, world, pos);
            } else {
                iterator.remove();
                refreshRenderArea(ref, world, pos);
            }
        }
    }

    private static void restoreMarkers(Reflection ref, Object world) throws Exception {
        if (world != null) {
            for (Map.Entry<Object, Object> entry : REPLACED.entrySet()) {
                Object pos = entry.getKey();
                Object current = ref.worldBlockState.invoke(world, pos);
                Object block = ref.blockFromState.invoke(current);
                if (block == ref.stainedGlassBlock) {
                    Object restored = wasRecentlyExploded(pos) ? ref.airState : entry.getValue();
                    ref.worldSetBlockState.invoke(world, pos, restored, 2);
                }
                refreshRenderArea(ref, world, pos);
            }
        }
        REPLACED.clear();
    }

    private static void restoreAll(Object world) {
        if (world != null) {
            try {
                Reflection ref = reflection();
                for (Map.Entry<Object, Object> entry : REPLACED.entrySet()) {
                    Object current = ref.worldBlockState.invoke(world, entry.getKey());
                    Object block = ref.blockFromState.invoke(current);
                    if (block == ref.stainedGlassBlock) {
                        Object restored = wasRecentlyExploded(entry.getKey()) ? ref.airState : entry.getValue();
                        ref.worldSetBlockState.invoke(world, entry.getKey(), restored, 2);
                    }
                    refreshRenderArea(ref, world, entry.getKey());
                }
            } catch (Throwable t) {
                System.err.println("[FireballPredictor] Restore error: " + t);
            }
        }
        REPLACED.clear();
    }

    private static boolean wasRecentlyExploded(Object pos) {
        return RECENT_EXPLOSIONS.containsKey(pos);
    }

    private static void tickRecentExplosions() {
        Iterator<Map.Entry<Object, Integer>> iterator = RECENT_EXPLOSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue().intValue() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(Integer.valueOf(ticksLeft));
            }
        }
    }

    private static void tickHomeCooldowns() {
        tickCooldownMap(HOME_WARNING_COOLDOWNS);
    }

    private static void tickCooldownMap(Map<String, Integer> cooldowns) {
        Iterator<Map.Entry<String, Integer>> iterator = cooldowns.entrySet().iterator();
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

    private static void refreshRenderArea(Reflection ref, Object world, Object pos) throws Exception {
        ref.worldMarkRenderUpdate.invoke(world, pos, pos);
        Object mc = ref.getMinecraft();
        Object renderGlobal = ref.mcRenderGlobal.get(mc);
        if (renderGlobal == null || ref.renderMarkRange == null) {
            return;
        }

        int x = ((Integer) ref.blockPosGetX.invoke(pos)).intValue();
        int y = ((Integer) ref.blockPosGetY.invoke(pos)).intValue();
        int z = ((Integer) ref.blockPosGetZ.invoke(pos)).intValue();
        ref.renderMarkRange.invoke(renderGlobal,
                x - 16, y - 16, z - 16,
                x + 16, y + 16, z + 16);
    }

    private static void scanHypixelStats(Reflection ref, Object mc, Object world, Object player) {
        try {
            HypixelStatsConfig config = hypixelStatsConfig();
            if (!config.enabled || config.apiKey.length() == 0 || myColor == -1 || !homeSet) {
                weakEnemyWarningActive = false;
                return;
            }
            if (hypixelStatsWorld != world) {
                clearHypixelStatsState();
                hypixelStatsWorld = world;
            }

            List<String> screenText = collectScoreboardText(world);
            if (!isBedwars4v4(ref, mc, world, player, screenText)) {
                weakEnemyWarningActive = false;
                hypixelStatsDetectedTicks = 0;
                return;
            }
            List<PlayerSnapshot> enemies = collectEnemySnapshots(ref, mc, world, player);
            if (enemies.isEmpty()) {
                weakEnemyWarningActive = false;
                return;
            }
            hypixelStatsDetectedTicks += HYPIXEL_STATS_SCAN_INTERVAL_TICKS;
            if (!hypixelStatsQueryStarted) {
                hypixelStatsQueryStarted = true;
                hypixelStatsGameKey = gameKey(enemies);
                HYPIXEL_STATS_GAME_ENEMIES.clear();
            }
            if (HYPIXEL_STATS_GAME_ENEMIES.size() < HYPIXEL_4V4_EXPECTED_ENEMIES) {
                mergePlayerSnapshots(HYPIXEL_STATS_GAME_ENEMIES, enemies, HYPIXEL_4V4_EXPECTED_ENEMIES);
            }
            updateWeakEnemyWarning(config, HYPIXEL_STATS_GAME_ENEMIES, System.currentTimeMillis());

            List<PlayerSnapshot> toQuery = new ArrayList<PlayerSnapshot>();
            long now = System.currentTimeMillis();
            for (PlayerSnapshot enemy : HYPIXEL_STATS_GAME_ENEMIES) {
                if (enemy.uuid == null || enemy.uuid.length() == 0 || enemy.name == null || enemy.name.length() == 0) {
                    continue;
                }
                synchronized (HYPIXEL_STATS_REQUESTED) {
                    if (HYPIXEL_STATS_REQUESTED.contains(enemy.uuid)) {
                        continue;
                    }
                }
                HypixelStatsEntry cached = getCachedHypixelStats(config, enemy.uuid, now);
                if (cached != null) {
                    postHypixelStatsLine(ref.loader, cached.withName(enemy.name));
                    markHypixelStatsRequested(enemy.uuid);
                    continue;
                }
                synchronized (HYPIXEL_STATS_REQUESTED) {
                    if (!HYPIXEL_STATS_REQUESTED.contains(enemy.uuid)) {
                        HYPIXEL_STATS_REQUESTED.add(enemy.uuid);
                        toQuery.add(enemy);
                    }
                }
            }

            if (toQuery.isEmpty()) {
                return;
            }
            if (!hypixelStatsHeaderPosted) {
                hypixelStatsHeaderPosted = true;
                postLocalChat(ref.loader, "\u00a7b[Stats] \u00a7f\u6b63\u5728\u67e5\u8be24V4\u5bf9\u9762\u73a9\u5bb6...");
            }
            for (PlayerSnapshot enemy : toQuery) {
                HYPIXEL_STATS_EXECUTOR.submit(new HypixelStatsTask(ref.loader, config, enemy));
            }
            updateWeakEnemyWarning(config, HYPIXEL_STATS_GAME_ENEMIES, System.currentTimeMillis());
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("hypixel stats scan error: " + t + " " + t.getMessage());
        }
    }

    private static void updateWeakEnemyWarning(HypixelStatsConfig config, List<PlayerSnapshot> enemies, long now) {
        int count = 0;
        int totalStars = 0;
        long totalKills = 0L;
        double totalKd = 0.0D;
        for (PlayerSnapshot enemy : enemies) {
            HypixelStatsEntry entry = getCachedHypixelStats(config, enemy.uuid, now);
            if (entry == null) {
                weakEnemyWarningActive = false;
                return;
            }
            count++;
            totalStars += entry.stars;
            totalKills += entry.kills;
            totalKd += entry.kd;
        }
        if (count <= 0) {
            weakEnemyWarningActive = false;
            return;
        }
        double averageKd = totalKd / count;
        double averageStars = (double) totalStars / (double) count;
        double averageKills = (double) totalKills / (double) count;
        weakEnemyWarningActive = averageKd < WEAK_ENEMY_MAX_AVERAGE_KD
                || (averageStars < WEAK_ENEMY_LOW_AVERAGE_STARS
                && averageKills < WEAK_ENEMY_LOW_AVERAGE_KILLS_WITH_LOW_STARS)
                || averageKills < WEAK_ENEMY_MAX_AVERAGE_KILLS;
    }

    private static boolean isBedwars4v4(Reflection ref, Object mc, Object world, Object player, List<String> screenText) {
        boolean sawBedwars = false;
        for (String line : screenText) {
            String lower = stripFormatting(line).toLowerCase();
            if (lower.contains("bed wars") || lower.contains("bedwars") || lower.contains("\u8d77\u5e8a")) {
                sawBedwars = true;
            }
            if (lower.contains("4v4") || lower.contains("4 v 4")) {
                return true;
            }
        }

        try {
            List<PlayerSnapshot> players = collectTeamSnapshots(ref, mc, world, player);
            Set<String> teams = new LinkedHashSet<String>();
            Map<String, Integer> counts = new HashMap<String, Integer>();
            for (PlayerSnapshot snapshot : players) {
                if (snapshot.color == -1) {
                    continue;
                }
                String team = teamName(snapshot.color);
                teams.add(team);
                Integer count = counts.get(team);
                counts.put(team, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
            if (teams.size() != 2 || players.size() > 8 || players.size() < 2) {
                return false;
            }
            for (Integer count : counts.values()) {
                if (count.intValue() > 4) {
                    return false;
                }
            }
            return sawBedwars;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<PlayerSnapshot> collectEnemySnapshots(Reflection ref, Object mc, Object world, Object self) throws Exception {
        List<PlayerSnapshot> all = collectTeamSnapshots(ref, mc, world, self);
        List<PlayerSnapshot> enemies = new ArrayList<PlayerSnapshot>();
        String selfUuid = uuidWithoutDashes(getUuid(self));
        for (PlayerSnapshot snapshot : all) {
            if (snapshot.color == -1 || snapshot.color == myColor) {
                continue;
            }
            if (selfUuid != null && selfUuid.equals(snapshot.uuid)) {
                continue;
            }
            enemies.add(snapshot);
        }
        return enemies;
    }

    private static List<PlayerSnapshot> collectTeamSnapshots(Reflection ref, Object mc, Object world, Object self) throws Exception {
        List<PlayerSnapshot> snapshots = new ArrayList<PlayerSnapshot>();
        mergePlayerSnapshots(snapshots, collectTabSnapshots(ref, mc), Integer.MAX_VALUE);

        List<?> players = (List<?>) ref.worldPlayerEntities.get(world);
        String selfUuid = uuidWithoutDashes(getUuid(self));
        for (Object entity : players) {
            if (!ref.playerClass.isInstance(entity)) {
                continue;
            }
            String uuid = uuidWithoutDashes(getUuid(entity));
            if (uuid == null || uuid.equals(selfUuid)) {
                continue;
            }
            mergePlayerSnapshot(snapshots, new PlayerSnapshot(uuid, getPlayerName(entity), playerColor(ref, entity)), Integer.MAX_VALUE);
        }
        return snapshots;
    }

    private static void mergePlayerSnapshots(List<PlayerSnapshot> target, List<PlayerSnapshot> source, int maxSize) {
        for (PlayerSnapshot snapshot : source) {
            mergePlayerSnapshot(target, snapshot, maxSize);
            if (target.size() >= maxSize) {
                return;
            }
        }
    }

    private static void mergePlayerSnapshot(List<PlayerSnapshot> target, PlayerSnapshot snapshot, int maxSize) {
        if (snapshot == null || snapshot.uuid == null || snapshot.uuid.length() == 0) {
            return;
        }
        for (int i = 0; i < target.size(); i++) {
            PlayerSnapshot existing = target.get(i);
            if (!snapshot.uuid.equals(existing.uuid)) {
                continue;
            }
            if ((existing.color == -1 && snapshot.color != -1)
                    || (existing.name == null || existing.name.length() == 0)) {
                target.set(i, snapshot);
            }
            return;
        }
        if (target.size() < maxSize) {
            target.add(snapshot);
        }
    }

    private static List<PlayerSnapshot> collectTabSnapshots(Reflection ref, Object mc) {
        List<PlayerSnapshot> snapshots = new ArrayList<PlayerSnapshot>();
        try {
            Object handler = invokeFirstOptional(mc, new String[]{"getNetHandler", "func_147114_u", "u"});
            if (handler == null) {
                return snapshots;
            }
            Object infosObject = invokeFirstOptional(handler, new String[]{"getPlayerInfoMap", "func_175106_d", "e"});
            if (!(infosObject instanceof Collection)) {
                return snapshots;
            }
            Collection<?> infos = (Collection<?>) infosObject;
            for (Object info : infos) {
                Object profile = invokeFirstOptional(info, new String[]{"getGameProfile", "func_178845_a", "a"});
                if (profile == null) {
                    profile = invokeNoArgByReturnName(info, "GameProfile");
                }
                String uuid = uuidWithoutDashes(getUuid(profile));
                String name = getPlayerName(profile);
                Object display = invokeFirstOptional(info, new String[]{"getDisplayName", "func_178854_k", "g"});
                String formatted = formattedText(display);
                int color = colorFromFormattedText(formatted);
                if (color == -1) {
                    Object team = invokeFirstOptional(info, new String[]{"getPlayerTeam", "func_178850_i", "d"});
                    if (team == null) {
                        team = invokeNoArgByReturnName(info, "ScorePlayerTeam");
                    }
                    color = colorFromTeamObject(team);
                }
                if (uuid != null && name != null && color != -1) {
                    snapshots.add(new PlayerSnapshot(uuid, name, color));
                }
            }
        } catch (Throwable ignored) {
        }
        return snapshots;
    }

    private static int colorFromTeamObject(Object team) {
        if (team == null) {
            return -1;
        }
        try {
            Method colorPrefix = Reflection.optionalMethod(team.getClass(), new String[]{"getColorPrefix", "func_96668_e", "e"});
            if (colorPrefix != null) {
                int fromPrefix = colorFromFormattedText(String.valueOf(colorPrefix.invoke(team)));
                if (fromPrefix != -1) {
                    return fromPrefix;
                }
            }
            Method chatFormat = Reflection.optionalMethod(team.getClass(), new String[]{"getChatFormat", "func_178775_l", "l"});
            return chatFormat == null ? -1 : colorFromFormattedText(String.valueOf(chatFormat.invoke(team)));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String gameKey(List<PlayerSnapshot> enemies) {
        StringBuilder builder = new StringBuilder();
        for (PlayerSnapshot enemy : enemies) {
            builder.append(enemy.uuid).append(',');
        }
        return builder.toString();
    }

    private static List<String> collectScoreboardText(Object world) {
        List<String> lines = new ArrayList<String>();
        try {
            Object scoreboard = invokeFirstOptional(world, new String[]{"getScoreboard", "func_96441_U", "ae"});
            if (scoreboard == null) {
                return lines;
            }
            Object objective = invokeFirstOptional(scoreboard, new String[]{"getObjectiveInDisplaySlot", "func_96539_a", "a"}, Integer.valueOf(1));
            if (objective == null) {
                return lines;
            }
            lines.add(String.valueOf(invokeFirstOptional(objective, new String[]{"getDisplayName", "func_96678_d", "d"})));
            Object scoresObject = invokeFirstOptional(scoreboard, new String[]{"getSortedScores", "func_96534_i", "i"}, objective);
            if (scoresObject instanceof Collection) {
                for (Object score : (Collection<?>) scoresObject) {
                    Object name = invokeFirstOptional(score, new String[]{"getPlayerName", "func_96653_e", "e"});
                    if (name != null) {
                        lines.add(String.valueOf(name));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return lines;
    }

    private static HypixelStatsEntry getCachedHypixelStats(HypixelStatsConfig config, String uuid, long now) {
        loadHypixelStatsCache(config);
        HypixelStatsEntry entry = HYPIXEL_STATS_CACHE.get(uuid);
        if (entry == null) {
            return null;
        }
        if (now - entry.timestamp > config.cacheMillis) {
            return null;
        }
        return entry;
    }

    private static void markHypixelStatsRequested(String uuid) {
        synchronized (HYPIXEL_STATS_REQUESTED) {
            HYPIXEL_STATS_REQUESTED.add(uuid);
        }
    }

    private static void postHypixelStatsLine(ClassLoader loader, HypixelStatsEntry entry) {
        try {
            postLocalChat(loader, "\u00a7b[Stats] \u00a7f" + entry.name
                    + " \u00a77\u661f\u6570:\u00a7e" + entry.stars
                    + " \u00a77\u603b\u51fb\u6740:\u00a7e" + entry.kills
                    + " \u00a77\u603bKD:\u00a7e" + formatKd(entry.kd));
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("hypixel stats chat error: " + t);
        }
    }

    private static String formatKd(double kd) {
        return String.format(java.util.Locale.US, "%.2f", Double.valueOf(kd));
    }

    private static HypixelStatsConfig hypixelStatsConfig() {
        if (hypixelStatsConfig != null) {
            return hypixelStatsConfig;
        }

        Properties properties = new Properties();
        File file = hypixelStatsConfigFile();
        if (file.isFile()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                properties.load(input);
            } catch (Throwable t) {
                FireballPredictorAgentLog.write("hypixel stats config load failed: " + t);
            } finally {
                closeQuietly(input);
            }
        }
        String apiKeyText = properties.getProperty("api-key", "").trim();
        String apiKeysText = properties.getProperty("api-keys", "").trim();
        apiKeysText = apiKeyText + "," + apiKeysText + "," + DEFAULT_HYPIXEL_API_KEYS;
        String[] apiKeys = parseHypixelApiKeys(apiKeysText);
        if (apiKeys.length == 0) {
            apiKeys = parseHypixelApiKeys(DEFAULT_HYPIXEL_API_KEYS);
        }
        String apiKey = apiKeys.length == 0 ? "" : apiKeys[0];
        hypixelStatsConfig = new HypixelStatsConfig(
                Boolean.parseBoolean(properties.getProperty("enabled", "true")),
                apiKey,
                apiKeys,
                Long.parseLong(properties.getProperty("cache-minutes", "360")) * 60L * 1000L,
                new File(file.getParentFile(), "fireballpredictor-hypixel-stats-cache-v2.properties"));
        return hypixelStatsConfig;
    }

    private static String[] parseHypixelApiKeys(String text) {
        if (text == null) {
            return new String[0];
        }
        String[] parts = text.split("[,;\\s]+");
        List<String> keys = new ArrayList<String>();
        for (String part : parts) {
            String key = part == null ? "" : part.trim();
            if (key.length() > 0 && !keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys.toArray(new String[keys.size()]);
    }

    private static String nextHypixelApiKey(HypixelStatsConfig config) {
        if (config.apiKeys.length == 0) {
            return config.apiKey;
        }
        synchronized (FireballPredictorHooks.class) {
            if (hypixelStatsApiKeyIndex < 0) {
                hypixelStatsApiKeyIndex = 0;
            }
            String key = config.apiKeys[hypixelStatsApiKeyIndex % config.apiKeys.length];
            hypixelStatsApiKeyIndex++;
            return key;
        }
    }

    private static File hypixelStatsConfigFile() {
        File mc = new File(System.getProperty("user.home"), "AppData\\Roaming\\.minecraft");
        File config = new File(mc, "config");
        return new File(config, "fireballpredictor-hypixel-stats.properties");
    }

    private static void loadHypixelStatsCache(HypixelStatsConfig config) {
        if (hypixelStatsCacheLoaded) {
            return;
        }
        hypixelStatsCacheLoaded = true;
        if (!config.cacheFile.isFile()) {
            return;
        }
        Properties properties = new Properties();
        FileInputStream input = null;
        try {
            input = new FileInputStream(config.cacheFile);
            properties.load(input);
            for (String uuid : properties.stringPropertyNames()) {
                HypixelStatsEntry entry = HypixelStatsEntry.fromCache(uuid, properties.getProperty(uuid));
                if (entry != null) {
                    HYPIXEL_STATS_CACHE.put(uuid, entry);
                }
            }
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("hypixel stats cache load failed: " + t);
        } finally {
            closeQuietly(input);
        }
    }

    private static void saveHypixelStatsCache(HypixelStatsConfig config) {
        Properties properties = new Properties();
        synchronized (HYPIXEL_STATS_CACHE) {
            for (Map.Entry<String, HypixelStatsEntry> entry : HYPIXEL_STATS_CACHE.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue().toCache());
            }
        }
        File parent = config.cacheFile.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(config.cacheFile);
            properties.store(output, "FireballPredictor Hypixel stats cache");
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("hypixel stats cache save failed: " + t);
        } finally {
            closeQuietly(output);
        }
    }

    private static HypixelStatsEntry fetchHypixelStats(HypixelStatsConfig config, PlayerSnapshot player) throws Exception {
        URL url = new URL("https://api.hypixel.net/player?uuid=" + player.uuid);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Api-Key", nextHypixelApiKey(config));
        connection.setRequestProperty("User-Agent", "FireballPredictorAgent/1.8.9");

        int code = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"));
        StringBuilder body = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } finally {
            closeQuietly(reader);
            connection.disconnect();
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Hypixel HTTP " + code + ": " + body);
        }

        String json = body.toString();
        String bedwars = readJsonObject(json, "Bedwars");
        if (bedwars.length() == 0) {
            bedwars = json;
        }
        double experience = readJsonNumber(bedwars, "Experience", 0.0D);
        long kills = (long) readJsonNumber(bedwars, "kills_bedwars", 0.0D);
        long deaths = (long) readJsonNumber(bedwars, "deaths_bedwars", 0.0D);
        double kd = deaths <= 0L ? (double) kills : (double) kills / (double) deaths;
        return new HypixelStatsEntry(player.uuid, player.name, System.currentTimeMillis(),
                bedwarsStars(experience), kills, kd);
    }

    private static int bedwarsStars(double experience) {
        int prestiges = (int) (experience / HYPIXEL_BEDWARS_EXP_PER_PRESTIGE);
        double exp = experience - prestiges * HYPIXEL_BEDWARS_EXP_PER_PRESTIGE;
        int level = 0;
        if (exp >= 500.0D) {
            level++;
            exp -= 500.0D;
        }
        if (exp >= 1000.0D) {
            level++;
            exp -= 1000.0D;
        }
        if (exp >= 2000.0D) {
            level++;
            exp -= 2000.0D;
        }
        if (exp >= 3500.0D) {
            level++;
            exp -= 3500.0D;
        }
        if (level >= 4 && exp > 0.0D) {
            level += (int) (exp / 5000.0D);
        }
        if (level > 99) {
            level = 99;
        }
        return prestiges * 100 + level;
    }

    private static double readJsonNumber(String json, String field, double fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String readJsonObject(String json, String field) {
        String needle = "\"" + field + "\"";
        int fieldIndex = json.indexOf(needle);
        if (fieldIndex < 0) {
            return "";
        }
        int braceStart = json.indexOf('{', fieldIndex + needle.length());
        if (braceStart < 0) {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = braceStart; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        return "";
    }

    private static void clearHypixelStatsState() {
        hypixelStatsWorld = null;
        hypixelStatsGameKey = null;
        hypixelStatsHeaderPosted = false;
        hypixelStatsQueryStarted = false;
        hypixelStatsDetectedTicks = 0;
        weakEnemyWarningActive = false;
        HYPIXEL_STATS_GAME_ENEMIES.clear();
        synchronized (HYPIXEL_STATS_REQUESTED) {
            HYPIXEL_STATS_REQUESTED.clear();
        }
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)[\\u00a7&][0-9a-fk-or]", "");
    }

    private static String getUuid(Object target) {
        if (target == null) {
            return null;
        }
        Object uuid = invokeFirstOptional(target, new String[]{"getUniqueID", "func_110124_au", "aJ", "getId"});
        if (uuid == null) {
            uuid = invokeNoArgByReturnType(target, UUID.class);
        }
        return uuid == null ? null : String.valueOf(uuid);
    }

    private static String uuidWithoutDashes(String uuid) {
        return uuid == null ? null : uuid.replace("-", "");
    }

    private static String getPlayerName(Object target) {
        if (target == null) {
            return null;
        }
        Object name = invokeFirstOptional(target, new String[]{"getName", "func_70005_c_", "e_", "getProfileName"});
        return name == null ? null : stripFormatting(String.valueOf(name));
    }

    private static String formattedText(Object component) {
        if (component == null) {
            return "";
        }
        if (component instanceof String) {
            return (String) component;
        }
        Object formatted = invokeFirstOptional(component, new String[]{"getFormattedText", "func_150254_d", "e"});
        return formatted == null ? String.valueOf(component) : String.valueOf(formatted);
    }

    private static Object invokeFirstOptional(Object target, String[] names, Object... args) {
        if (target == null) {
            return null;
        }
        Class<?> type = target instanceof Class ? (Class<?>) target : target.getClass();
        for (String name : names) {
            Method method = findNoStrictMethod(type, name, args);
            if (method == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method.invoke(target instanceof Class ? null : target, args);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findNoStrictMethod(Class<?> type, String name, Object[] args) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (args[i] != null && !wrap(parameterTypes[i]).isInstance(args[i])) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == Integer.TYPE) {
            return Integer.class;
        }
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }
        if (type == Double.TYPE) {
            return Double.class;
        }
        if (type == Long.TYPE) {
            return Long.class;
        }
        return type;
    }

    private static Object invokeNoArgByReturnType(Object target, Class<?> returnType) {
        if (target == null) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 0 && returnType.isAssignableFrom(method.getReturnType())
                        && !Modifier.isStatic(method.getModifiers())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static Object invokeNoArgByReturnName(Object target, String returnName) {
        if (target == null) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 0
                        && method.getReturnType().getName().contains(returnName)
                        && !Modifier.isStatic(method.getModifiers())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static void postLocalChat(final ClassLoader loader, final String text) throws Exception {
        final Class<?> minecraftClass = loadFirst(loader, "net.minecraft.client.Minecraft", "ave");
        Object minecraft = invokeFirstOptional(minecraftClass, new String[]{"getMinecraft", "func_71410_x", "A"});
        if (minecraft == null) {
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Object mc = invokeFirstOptional(minecraftClass, new String[]{"getMinecraft", "func_71410_x", "A"});
                    Object ingame = fieldFirst(mc, new String[]{"ingameGUI", "q"});
                    Object chatGui = invokeFirstOptional(ingame, new String[]{"getChatGUI", "d"});
                    Object component = newTextComponentWithLoader(loader, text);
                    invokeFirstOptional(chatGui, new String[]{"printChatMessage", "a"}, component);
                } catch (Throwable t) {
                    FireballPredictorAgentLog.write("postLocalChat task failed: " + t);
                }
            }
        };
        invokeFirstOptional(minecraft, new String[]{"addScheduledTask", "func_152344_a", "a"}, task);
    }

    private static Object newTextComponentWithLoader(ClassLoader loader, String text) throws Exception {
        Class<?> textComponent = loadFirst(loader, "net.minecraft.util.ChatComponentText", "fa");
        return textComponent.getConstructor(String.class).newInstance(text);
    }

    private static Class<?> loadFirst(ClassLoader loader, String primary, String fallback) throws ClassNotFoundException {
        try {
            return Class.forName(primary, false, loader);
        } catch (ClassNotFoundException ignored) {
            return Class.forName(fallback, false, loader);
        }
    }

    private static Object fieldFirst(Object target, String[] names) throws Exception {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                Field field = type.getField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
            }
        }
        for (String name : names) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        throw new NoSuchFieldException(java.util.Arrays.toString(names));
    }

    private static void closeQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.getClass().getMethod("close").invoke(closeable);
        } catch (Throwable ignored) {
        }
    }

    private static Reflection reflection() throws Exception {
        if (r == null) {
            r = new Reflection();
        }
        return r;
    }

    private static final class HypixelStatsTask implements Runnable {
        private final ClassLoader loader;
        private final HypixelStatsConfig config;
        private final PlayerSnapshot player;

        HypixelStatsTask(ClassLoader loader, HypixelStatsConfig config, PlayerSnapshot player) {
            this.loader = loader;
            this.config = config;
            this.player = player;
        }

        @Override
        public void run() {
            try {
                HypixelStatsEntry entry = fetchHypixelStats(config, player);
                HYPIXEL_STATS_CACHE.put(player.uuid, entry);
                saveHypixelStatsCache(config);
                postHypixelStatsLine(loader, entry);
            } catch (Throwable t) {
                FireballPredictorAgentLog.write("hypixel stats fetch failed for " + player.name + ": " + t + " " + t.getMessage());
                try {
                    postLocalChat(loader, "\u00a7c[Stats] \u00a7f" + player.name + " \u67e5\u8be2\u5931\u8d25");
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static final class HypixelStatsConfig {
        final boolean enabled;
        final String apiKey;
        final String[] apiKeys;
        final long cacheMillis;
        final File cacheFile;

        HypixelStatsConfig(boolean enabled, String apiKey, String[] apiKeys, long cacheMillis, File cacheFile) {
            this.enabled = enabled;
            this.apiKey = apiKey;
            this.apiKeys = apiKeys == null ? new String[0] : apiKeys;
            this.cacheMillis = cacheMillis;
            this.cacheFile = cacheFile;
        }
    }

    private static final class PlayerSnapshot {
        final String uuid;
        final String name;
        final int color;

        PlayerSnapshot(String uuid, String name, int color) {
            this.uuid = uuid;
            this.name = name;
            this.color = color;
        }
    }

    private static final class HypixelStatsEntry {
        final String uuid;
        final String name;
        final long timestamp;
        final int stars;
        final long kills;
        final double kd;

        HypixelStatsEntry(String uuid, String name, long timestamp, int stars, long kills, double kd) {
            this.uuid = uuid;
            this.name = name;
            this.timestamp = timestamp;
            this.stars = stars;
            this.kills = kills;
            this.kd = kd;
        }

        HypixelStatsEntry withName(String newName) {
            return new HypixelStatsEntry(uuid, newName, timestamp, stars, kills, kd);
        }

        String toCache() {
            return timestamp + "|" + name + "|" + stars + "|" + kills + "|" + formatKd(kd);
        }

        static HypixelStatsEntry fromCache(String uuid, String value) {
            if (value == null) {
                return null;
            }
            String[] parts = value.split("\\|");
            if (parts.length < 5) {
                return null;
            }
            try {
                return new HypixelStatsEntry(uuid, parts[1], Long.parseLong(parts[0]),
                        Integer.parseInt(parts[2]), Long.parseLong(parts[3]), Double.parseDouble(parts[4]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private static final class GenInfo {
        final int x;
        final int y;
        final int z;
        int amount;
        final int distance;
        final boolean looking;

        GenInfo(int x, int y, int z, int amount, int distance, boolean looking) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.amount = amount;
            this.distance = distance;
            this.looking = looking;
        }
    }

    private static final class BedPos {
        final int x;
        final int y;
        final int z;

        BedPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class Vec {
        final double x;
        final double y;
        final double z;

        Vec(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        Vec normalize() {
            double length = length();
            if (length < 0.0001D) {
                return new Vec(0.0D, 0.0D, 0.0D);
            }
            return new Vec(x / length, y / length, z / length);
        }
    }

    private static final class Reflection {
        final ClassLoader loader;
        final Class<?> minecraftClass;
        final Class<?> worldClass;
        final Class<?> entityClass;
        final Class<?> playerClass;
        final Class<?> fireballClass;
        final Class<?> entityItemClass;
        final Class<?> gameSettingsClass;
        final Class<?> keyBindingClass;
        final Class<?> vec3Class;
        final Class<?> blockPosClass;
        final Class<?> aabbClass;
        final Class<?> blockClass;
        final Class<?> itemClass;
        final Class<?> itemStackClass;
        final Class<?> nbtClass;
        final Class<?> stateClass;
        final Class<?> movingObjectClass;
        final Class<?> explosionPacketClass;
        final Class<?> scaledResolutionClass;
        final Class<?> renderGlobalClass;
        final Class<?> glStateManagerClass;

        final Method minecraftInstance;
        final Method mcRenderViewEntity;
        final Field mcWorld;
        final Field mcPlayer;
        final Field mcFontRenderer;
        final Field mcRenderGlobal;
        final Field mcGameSettings;
        final Field mcObjectMouseOver;

        final Field worldLoadedEntityList;
        final Field worldPlayerEntities;
        final Method worldBlockState;
        final Method worldSetBlockState;
        final Method worldMarkRenderUpdate;
        final Method worldRayTrace;

        final Field entityPosX;
        final Field entityPosY;
        final Field entityPosZ;
        final Field entityLastTickPosX;
        final Field entityLastTickPosY;
        final Field entityLastTickPosZ;
        final Field entityMotionX;
        final Field entityMotionY;
        final Field entityMotionZ;
        final Method entityLook;
        final Method entityPositionEyes;
        final Method entityEyeHeight;
        final Method entityBoundingBox;
        final Method entityIsInvisible;
        final Method entityDisplayName;
        final Method chatFormattedText;

        final Field fireballAccelX;
        final Field fireballAccelY;
        final Field fireballAccelZ;

        final Method playerHeldItem;
        final Method playerCurrentArmor;
        final Method playerTeam;
        final Method itemStackItem;
        final Method entityItemStack;
        final Field itemStackSize;
        final Method itemStackHasTag;
        final Method itemStackSubCompound;
        final Object fireChargeItem;
        final Object diamondItem;
        final Object emeraldItem;

        final Field gameSettingsAttackKey;
        final Method keyBindingIsDown;

        final Constructor<?> vec3Ctor;
        final Constructor<?> blockPosCtorDdd;
        final Field vecX;
        final Field vecY;
        final Field vecZ;
        final Method vec3Add;

        final Method blockPosAdd;
        final Method blockPosGetX;
        final Method blockPosGetY;
        final Method blockPosGetZ;
        final Method movingObjectBlockPos;

        final Method blockFromName;
        final Object airBlock;
        final Object airState;
        final Object stainedGlassBlock;
        final Object bedBlock;
        final Method blockStateFromMeta;
        final Object redGlassState;
        final Object diamondBlock;
        final Object emeraldBlock;
        final Method blockIsFullCube;
        final Method blockFromState;
        final Method renderMarkRange;
        final Method renderDrawSelection;
        final Method explosionAffectedBlocks;
        final Method aabbOffset;
        final Method nbtHasKey;
        final Method nbtGetInteger;

        final Constructor<?> scaledResolutionCtor;
        final Method scaledWidth;
        final Method scaledHeight;
        final Method fontWidth;
        final Method fontDrawShadow;
        final Method glPush;
        final Method glScale;
        final Method glPop;
        final Method glDisableTexture;
        final Method glEnableTexture;
        final Method glEnableBlend;
        final Method glDisableBlend;
        final Method glDisableDepth;
        final Method glEnableDepth;
        final Method glBlendFunc;
        final Method glColor;

        Reflection() throws Exception {
            loader = Thread.currentThread().getContextClassLoader();
            minecraftClass = loadFirst("net.minecraft.client.Minecraft", "ave");
            worldClass = loadFirst("net.minecraft.world.World", "adm");
            entityClass = loadFirst("net.minecraft.entity.Entity", "pk");
            playerClass = loadFirst("net.minecraft.entity.player.EntityPlayer", "wn");
            fireballClass = loadFirst("net.minecraft.entity.projectile.EntityFireball", "ws");
            entityItemClass = loadFirst("net.minecraft.entity.item.EntityItem", "uz");
            gameSettingsClass = loadFirst("net.minecraft.client.settings.GameSettings", "avh");
            keyBindingClass = loadFirst("net.minecraft.client.settings.KeyBinding", "avb");
            vec3Class = loadFirst("net.minecraft.util.Vec3", "aui");
            blockPosClass = loadFirst("net.minecraft.util.BlockPos", "cj");
            aabbClass = loadFirst("net.minecraft.util.AxisAlignedBB", "aug");
            blockClass = loadFirst("net.minecraft.block.Block", "afh");
            itemClass = loadFirst("net.minecraft.item.Item", "zw");
            itemStackClass = loadFirst("net.minecraft.item.ItemStack", "zx");
            nbtClass = loadFirst("net.minecraft.nbt.NBTTagCompound", "dn");
            stateClass = loadFirst("net.minecraft.block.state.IBlockState", "alz");
            movingObjectClass = loadFirst("net.minecraft.util.MovingObjectPosition", "auh");
            explosionPacketClass = loadFirst("net.minecraft.network.play.server.S27PacketExplosion", "gk");
            scaledResolutionClass = loadFirst("net.minecraft.client.gui.ScaledResolution", "avr");
            renderGlobalClass = loadFirst("net.minecraft.client.renderer.RenderGlobal", "bfr");
            glStateManagerClass = loadFirst("net.minecraft.client.renderer.GlStateManager", "bfl");
            Class<?> chatComponentClass = tryLoadFirst("net.minecraft.util.IChatComponent", "eu");

            minecraftInstance = method(minecraftClass, new String[]{"getMinecraft", "A"});
            mcRenderViewEntity = method(minecraftClass, new String[]{"getRenderViewEntity", "ac"});
            mcWorld = field(minecraftClass, "theWorld", "f");
            mcPlayer = field(minecraftClass, "thePlayer", "h");
            mcFontRenderer = field(minecraftClass, "fontRendererObj", "k");
            mcRenderGlobal = field(minecraftClass, "renderGlobal", "g");
            mcGameSettings = field(minecraftClass, "gameSettings", "t");
            mcObjectMouseOver = field(minecraftClass, "objectMouseOver", "s");

            worldLoadedEntityList = field(worldClass, "loadedEntityList", "f");
            worldPlayerEntities = field(worldClass, "playerEntities", "j");
            worldBlockState = method(worldClass, new String[]{"getBlockState", "p"}, blockPosClass);
            worldSetBlockState = method(worldClass, new String[]{"setBlockState", "a"}, blockPosClass, stateClass, Integer.TYPE);
            worldMarkRenderUpdate = method(worldClass, new String[]{"markBlockRangeForRenderUpdate", "b"}, blockPosClass, blockPosClass);
            worldRayTrace = method(worldClass, new String[]{"rayTraceBlocks", "a"}, vec3Class, vec3Class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);

            entityPosX = field(entityClass, "posX", "s");
            entityPosY = field(entityClass, "posY", "t");
            entityPosZ = field(entityClass, "posZ", "u");
            entityLastTickPosX = field(entityClass, "lastTickPosX", "P");
            entityLastTickPosY = field(entityClass, "lastTickPosY", "Q");
            entityLastTickPosZ = field(entityClass, "lastTickPosZ", "R");
            entityMotionX = field(entityClass, "motionX", "v");
            entityMotionY = field(entityClass, "motionY", "w");
            entityMotionZ = field(entityClass, "motionZ", "x");
            entityLook = method(entityClass, new String[]{"getLook", "d"}, Float.TYPE);
            entityPositionEyes = method(entityClass, new String[]{"getPositionEyes", "e"}, Float.TYPE);
            entityEyeHeight = method(entityClass, new String[]{"getEyeHeight", "aS"});
            entityBoundingBox = method(entityClass, new String[]{"getEntityBoundingBox", "aR"});
            entityIsInvisible = method(entityClass, new String[]{"isInvisible", "ax"});
            entityDisplayName = optionalMethod(entityClass, new String[]{"getDisplayName", "func_145748_c_", "e_", "f_"});
            chatFormattedText = chatComponentClass == null
                    ? null
                    : optionalMethod(chatComponentClass, new String[]{"getFormattedText", "func_150254_d", "e"});

            fireballAccelX = field(fireballClass, "accelerationX", "b");
            fireballAccelY = field(fireballClass, "accelerationY", "c");
            fireballAccelZ = field(fireballClass, "accelerationZ", "d");

            playerHeldItem = method(playerClass, new String[]{"getHeldItem", "bZ"});
            playerCurrentArmor = method(playerClass, new String[]{"getCurrentArmor", "q"}, Integer.TYPE);
            playerTeam = optionalMethod(playerClass, new String[]{"getTeam", "func_96124_cp", "aO"});
            itemStackItem = method(itemStackClass, new String[]{"getItem", "b"});
            entityItemStack = method(entityItemClass, new String[]{"getEntityItem", "l"});
            itemStackSize = field(itemStackClass, "stackSize", "b");
            itemStackHasTag = method(itemStackClass, new String[]{"hasTagCompound", "n"});
            itemStackSubCompound = method(itemStackClass, new String[]{"getSubCompound", "a"}, String.class, Boolean.TYPE);
            fireChargeItem = method(itemClass, new String[]{"getByNameOrId", "d"}, String.class).invoke(null, "fire_charge");
            diamondItem = method(itemClass, new String[]{"getByNameOrId", "d"}, String.class).invoke(null, "diamond");
            emeraldItem = method(itemClass, new String[]{"getByNameOrId", "d"}, String.class).invoke(null, "emerald");

            gameSettingsAttackKey = field(gameSettingsClass, "keyBindAttack", "ai");
            keyBindingIsDown = method(keyBindingClass, new String[]{"isKeyDown", "d"});

            vec3Ctor = vec3Class.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
            blockPosCtorDdd = blockPosClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
            vecX = field(vec3Class, "xCoord", "a");
            vecY = field(vec3Class, "yCoord", "b");
            vecZ = field(vec3Class, "zCoord", "c");
            vec3Add = method(vec3Class, new String[]{"addVector", "b"}, Double.TYPE, Double.TYPE, Double.TYPE);

            blockPosAdd = method(blockPosClass, new String[]{"add", "a"}, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            blockPosGetX = method(blockPosClass, new String[]{"getX", "n"});
            blockPosGetY = method(blockPosClass, new String[]{"getY", "o"});
            blockPosGetZ = method(blockPosClass, new String[]{"getZ", "p"});
            movingObjectBlockPos = method(movingObjectClass, new String[]{"getBlockPos", "a"});

            blockFromName = method(blockClass, new String[]{"getBlockFromName", "b"}, String.class);
            airBlock = blockFromName.invoke(null, "air");
            stainedGlassBlock = blockFromName.invoke(null, "stained_glass");
            bedBlock = blockFromName.invoke(null, "bed");
            diamondBlock = blockFromName.invoke(null, "diamond_block");
            emeraldBlock = blockFromName.invoke(null, "emerald_block");
            blockStateFromMeta = method(blockClass, new String[]{"getStateFromMeta", "a"}, Integer.TYPE);
            airState = blockStateFromMeta.invoke(airBlock, 0);
            redGlassState = blockStateFromMeta.invoke(stainedGlassBlock, 14);
            blockIsFullCube = method(blockClass, new String[]{"isFullCube", "d"});
            blockFromState = method(stateClass, new String[]{"getBlock", "c"});
            renderMarkRange = method(renderGlobalClass, new String[]{"markBlockRangeForRenderUpdate", "a"},
                    Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            renderDrawSelection = method(renderGlobalClass, new String[]{"drawSelectionBoundingBox", "a"}, aabbClass);
            explosionAffectedBlocks = method(explosionPacketClass, new String[]{"getAffectedBlockPositions", "h"});
            aabbOffset = method(aabbClass, new String[]{"offset", "c"}, Double.TYPE, Double.TYPE, Double.TYPE);
            nbtHasKey = method(nbtClass, new String[]{"hasKey", "c"}, String.class);
            nbtGetInteger = method(nbtClass, new String[]{"getInteger", "f"}, String.class);

            scaledResolutionCtor = scaledResolutionClass.getConstructor(minecraftClass);
            scaledWidth = method(scaledResolutionClass, new String[]{"getScaledWidth", "a"});
            scaledHeight = method(scaledResolutionClass, new String[]{"getScaledHeight", "b"});
            Class<?> fontRendererClass = loadFirst("net.minecraft.client.gui.FontRenderer", "avn");
            fontWidth = method(fontRendererClass, new String[]{"getStringWidth", "a"}, String.class);
            fontDrawShadow = method(fontRendererClass, new String[]{"drawStringWithShadow", "a"}, String.class, Float.TYPE, Float.TYPE, Integer.TYPE);
            glPush = method(glStateManagerClass, new String[]{"pushMatrix", "E"});
            glScale = method(glStateManagerClass, new String[]{"scale", "a"}, Float.TYPE, Float.TYPE, Float.TYPE);
            glPop = method(glStateManagerClass, new String[]{"popMatrix", "F"});
            glDisableTexture = method(glStateManagerClass, new String[]{"disableTexture2D", "x"});
            glEnableTexture = method(glStateManagerClass, new String[]{"enableTexture2D", "w"});
            glEnableBlend = method(glStateManagerClass, new String[]{"enableBlend", "l"});
            glDisableBlend = method(glStateManagerClass, new String[]{"disableBlend", "k"});
            glDisableDepth = method(glStateManagerClass, new String[]{"disableDepth", "i"});
            glEnableDepth = method(glStateManagerClass, new String[]{"enableDepth", "j"});
            glBlendFunc = method(glStateManagerClass, new String[]{"tryBlendFuncSeparate", "a"}, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            glColor = method(glStateManagerClass, new String[]{"color", "c"}, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        }

        Object getMinecraft() throws Exception {
            return minecraftInstance.invoke(null);
        }

        private static Field field(Class<?> owner, String... names) throws Exception {
            NoSuchFieldException last = null;
            for (String name : names) {
                try {
                    Field field = owner.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    last = e;
                }
            }
            throw last;
        }

        private static Method method(Class<?> owner, String[] names, Class<?>... parameterTypes) throws Exception {
            NoSuchMethodException last = null;
            for (String name : names) {
                try {
                    Method method = owner.getMethod(name, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    last = e;
                }
            }
            throw last;
        }

        private static Method optionalMethod(Class<?> owner, String[] names, Class<?>... parameterTypes) {
            if (owner == null) {
                return null;
            }
            for (String name : names) {
                try {
                    Method method = owner.getMethod(name, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    try {
                        Method method = owner.getDeclaredMethod(name, parameterTypes);
                        method.setAccessible(true);
                        return method;
                    } catch (NoSuchMethodException ignoredAgain) {
                        // Try the next candidate name.
                    }
                }
            }
            return null;
        }

        private Class<?> loadFirst(String primary, String fallback) throws ClassNotFoundException {
            try {
                return Class.forName(primary, false, loader);
            } catch (ClassNotFoundException ignored) {
                return Class.forName(fallback, false, loader);
            }
        }

        private Class<?> tryLoadFirst(String primary, String fallback) {
            try {
                return loadFirst(primary, fallback);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }

    }
}
