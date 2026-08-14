package com.example.fireballpredictor.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private static final int PROTECTION_SCAN_INTERVAL_TICKS = 10;
    private static final int PROTECTION_DEBUG_INTERVAL_TICKS = 100;
    private static final int PROTECTION_ZERO_LOCK_TICKS_AFTER_BED = 1800;
    private static final int PROTECTION_THREE_ECONOMY_TICKS_AFTER_BED = 15000;
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
    private static final int DAMAGE_PROBE_TICKS = 20;
    private static final int MAX_BEDWARS_ENEMIES = 16;
    private static final int PROTECTION_SAMPLE_LIMIT = 6;
    private static final int PROTECTION_SAMPLE_COOLDOWN_TICKS = 10;
    private static final int PROTECTION_SHARE_SYNC_INTERVAL_TICKS = 20;
    private static final int PROTECTION_SHARE_CONNECT_TIMEOUT_MILLIS = 2500;
    private static final int PROTECTION_SHARE_READ_TIMEOUT_MILLIS = 3500;
    private static final boolean DAMAGE_DEBUG_CHAT = false;
    private static final double DAMAGE_EPSILON = 0.01D;
    private static final double WEAK_ENEMY_MAX_AVERAGE_KD = 0.70D;
    private static final double WEAK_ENEMY_LOW_AVERAGE_STARS = 50.0D;
    private static final double WEAK_ENEMY_LOW_AVERAGE_KILLS_WITH_LOW_STARS = 400.0D;
    private static final double WEAK_ENEMY_MAX_AVERAGE_KILLS = 500.0D;
    private static final double TRACE_DISTANCE = 100.0D;
    private static final String DEFAULT_HYPIXEL_API_KEY = "0094afab-949d-4e06-b7dc-4d3db1282489";
    private static final String EXTRA_HYPIXEL_API_KEY = "63298aad-ad18-4305-b2e5-76c22f2b8514";
    private static final String DEFAULT_HYPIXEL_API_KEYS = DEFAULT_HYPIXEL_API_KEY + "," + EXTRA_HYPIXEL_API_KEY;
    private static final String PROTECTION_SHARE_HOST = "3722d01e5a6f.ofalias.com";
    private static final int PROTECTION_SHARE_PORT = 48820;
    private static final String PROTECTION_SHARE_ROOM = "default";
    private static final String PROTECTION_SHARE_SENDER_ID = "mc-" + UUID.randomUUID().toString().replace("-", "");
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
    private static final Map<String, Integer> PROTECTION_BY_TEAM = new HashMap<String, Integer>();
    private static final Map<String, List<ProtectionSample>> PROTECTION_SAMPLES_BY_TEAM =
            new HashMap<String, List<ProtectionSample>>();
    private static final Map<String, ProtectionProbability> PROTECTION_PROBABILITY_BY_TEAM =
            new HashMap<String, ProtectionProbability>();
    private static final Set<String> PROTECTION_VISIBLE_ENEMY_TEAMS = new LinkedHashSet<String>();

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
    private static int homeRecordedTicks;
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
    private static int protectionCounter;
    private static int protectionDebugCounter;
    private static int protectionShareCounter;
    private static int homeCounter;
    private static int hypixelStatsCounter;
    private static int lastHelmetTeamColor = -1;
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
    private static boolean damageAttackWasDown;
    private static DamageProbe damageProbe;
    private static String lastAimUuid;
    private static String lastAimName;
    private static int lastAimColor = -1;
    private static double lastAimHealth = -1.0D;
    private static int protectionSampleCooldownTicks;
    private static volatile boolean protectionShareBusy;
    private static volatile long protectionShareSince;
    private static final List<RemoteProtectionSample> REMOTE_PROTECTION_PENDING = new ArrayList<RemoteProtectionSample>();
    private static Method enchantmentHelperGetEnchantments;
    private static boolean enchantmentHelperLookupDone;
    private static final ExecutorService HYPIXEL_STATS_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "fireballpredictor-hypixel-stats");
            thread.setDaemon(true);
            return thread;
        }
    });
    private static final ExecutorService PROTECTION_SHARE_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "fireballpredictor-protection-share");
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
                protectionCounter = 0;
                protectionDebugCounter = 0;
                protectionShareCounter = 0;
                homeCounter = 0;
                hypixelStatsCounter = 0;
                myColor = -1;
                lastHelmetTeamColor = -1;
                invisWarningTeam = null;
                invisWarningDistance = 0;
                invisWarningTicks = 0;
                clearHypixelStatsState();
                clearDamageProbeState();
                clearProtectionShareState();
                clearHomeState();
                DIAMOND_GENS.clear();
                EMERALD_GENS.clear();
                INVIS_ACTIVE_TEAMS.clear();
                PROTECTION_BY_TEAM.clear();
                PROTECTION_SAMPLES_BY_TEAM.clear();
                PROTECTION_PROBABILITY_BY_TEAM.clear();
                PROTECTION_VISIBLE_ENEMY_TEAMS.clear();
                HOME_ACTIVE_ENTITIES.clear();
                restoreAll(lastWorld);
                lastWorld = world;
                return;
            }

            if (lastWorld != null && lastWorld != world) {
                clearHomeState();
                clearHypixelStatsState();
                clearDamageProbeState();
                clearProtectionShareState();
                INVIS_ACTIVE_TEAMS.clear();
                PROTECTION_BY_TEAM.clear();
                PROTECTION_SAMPLES_BY_TEAM.clear();
                PROTECTION_PROBABILITY_BY_TEAM.clear();
                PROTECTION_VISIBLE_ENEMY_TEAMS.clear();
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
            lastHelmetTeamColor = helmetColor;
            myColor = helmetColor == -1 ? playerColor(ref, player) : helmetColor;
            if (++genCounter >= GEN_SCAN_INTERVAL_TICKS) {
                genCounter = 0;
                scanGenerators(ref, world, player);
            }
            if (++invisCounter >= INVIS_SCAN_INTERVAL_TICKS) {
                invisCounter = 0;
                scanInvisibleEnemies(ref, world, player);
            }
            if (++protectionCounter >= PROTECTION_SCAN_INTERVAL_TICKS) {
                protectionCounter = 0;
                scanProtectionLevels(ref, world, player);
            }
            int homeScanInterval = homeSet ? 1 : HOME_SCAN_INTERVAL_TICKS;
            if (++homeCounter >= homeScanInterval) {
                homeCounter = 0;
                updateHomeAndScanEnemies(ref, world, player, helmetColor);
            }
            if (homeSet && homeRecordedTicks < PROTECTION_ZERO_LOCK_TICKS_AFTER_BED) {
                homeRecordedTicks++;
            }
            if (++refreshCounter >= REFRESH_INTERVAL_TICKS) {
                refreshCounter = 0;
                refreshMarkers(ref, world);
            }
            if (++hypixelStatsCounter >= HYPIXEL_STATS_SCAN_INTERVAL_TICKS) {
                hypixelStatsCounter = 0;
                scanHypixelStats(ref, mc, world, player);
            }
            trackAttackDamage(ref, mc, world, player);
            tickProtectionShare(ref);
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
            y = drawProtectionLine(ref, font, 5, y);

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

    private static int drawProtectionLine(Reflection ref, Object font, int x, int y) throws Exception {
        if (myColor == -1 || (PROTECTION_VISIBLE_ENEMY_TEAMS.isEmpty() && PROTECTION_BY_TEAM.isEmpty())) {
            return y;
        }

        String label = "\u4fdd\u62a4\uff1a ";
        ref.fontDrawShadow.invoke(font, label, (float) x, (float) y, 0xFFAA00);
        int cursor = x + ((Integer) ref.fontWidth.invoke(font, label)).intValue();
        boolean drew = false;
        String[] order = new String[]{"red", "blue", "yellow", "green"};
        for (String team : order) {
            if (teamColor(team) == myColor) {
                continue;
            }
            if (!PROTECTION_VISIBLE_ENEMY_TEAMS.contains(team) && !PROTECTION_BY_TEAM.containsKey(team)) {
                continue;
            }
            if (drew) {
                ref.fontDrawShadow.invoke(font, " ", (float) cursor, (float) y, HUD_WHITE);
                cursor += ((Integer) ref.fontWidth.invoke(font, " ")).intValue();
            }
            String name = teamChineseName(team) + ": ";
            ref.fontDrawShadow.invoke(font, name, (float) cursor, (float) y, teamDisplayColor(team));
            cursor += ((Integer) ref.fontWidth.invoke(font, name)).intValue();

            Integer level = PROTECTION_BY_TEAM.get(team);
            String value = level == null ? "\u672a\u77e5" : String.valueOf(level.intValue());
            value += protectionProbabilityText(team);
            ref.fontDrawShadow.invoke(font, value, (float) cursor, (float) y, HUD_WHITE);
            cursor += ((Integer) ref.fontWidth.invoke(font, value)).intValue();
            drew = true;
        }
        return drew ? y + 10 : y;
    }

    private static String protectionProbabilityText(String team) {
        ProtectionProbability probability = PROTECTION_PROBABILITY_BY_TEAM.get(team);
        if (probability == null || probability.firstLevel < 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(' ')
                .append(probability.firstLevel)
                .append('(')
                .append(probability.firstPercent)
                .append("%)");
        if (probability.secondLevel >= 0 && probability.secondPercent > 0) {
            builder.append(' ')
                    .append(probability.secondLevel)
                    .append('(')
                    .append(probability.secondPercent)
                    .append("%)");
        }
        return builder.toString();
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

    private static void scanProtectionLevels(Reflection ref, Object world, Object player) throws Exception {
        PROTECTION_VISIBLE_ENEMY_TEAMS.clear();
        if (myColor == -1) {
            return;
        }

        protectionDebugCounter += PROTECTION_SCAN_INTERVAL_TICKS;
        boolean debug = false;
        if (protectionDebugCounter >= PROTECTION_DEBUG_INTERVAL_TICKS) {
            protectionDebugCounter = 0;
            debug = true;
        }
        int visibleEnemies = 0;
        List<?> entities = (List<?>) ref.worldLoadedEntityList.get(world);
        for (Object entity : entities) {
            if (!ref.playerClass.isInstance(entity) || entity == player) {
                continue;
            }

            int color = playerColor(ref, entity);
            if (color == -1 || color == myColor) {
                continue;
            }

            String team = teamName(color);
            PROTECTION_VISIBLE_ENEMY_TEAMS.add(team);
            if (isProtectionZeroLocked()) {
                setProtectionZeroForTeam(team);
            }
            visibleEnemies++;
            int level = playerProtectionLevel(ref, entity);
            if (debug) {
                FireballPredictorAgentLog.write("protection scan: name=" + getPlayerName(entity)
                        + " team=" + team + " level=" + level + " armor=" + armorProtectionDebug(ref, entity));
            }
            if (level < 0) {
                continue;
            }
            FireballPredictorAgentLog.write("protection direct read ignored: " + team + "=" + level);
        }
        if (debug && visibleEnemies == 0) {
            FireballPredictorAgentLog.write("protection scan: no visible enemy players; myColor=" + myColor);
        }
    }

    private static int playerProtectionLevel(Reflection ref, Object player) throws Exception {
        int best = -1;
        for (int slot = 0; slot < 4; slot++) {
            Object armor = ref.playerCurrentArmor.invoke(player, slot);
            int level = itemProtectionLevel(armor);
            if (level > best) {
                best = level;
            }
        }
        return best;
    }

    private static int itemProtectionLevel(Object stack) {
        return itemEnchantmentLevel(stack, 0);
    }

    private static int itemEnchantmentLevel(Object stack, int enchantmentId) {
        if (stack == null) {
            return -1;
        }
        int fromHelper = enchantmentHelperLevel(stack, enchantmentId);
        if (fromHelper >= 0) {
            return fromHelper;
        }
        Object enchants = invokeFirstOptional(stack, new String[]{
                "getEnchantmentTagList", "func_77986_q", "q"});
        if (enchants == null) {
            Object tag = invokeFirstOptional(stack, new String[]{
                    "getTagCompound", "func_77978_p", "p"});
            if (tag != null) {
                enchants = invokeFirstOptional(tag, new String[]{
                        "getTagList", "func_150295_c", "c"}, "ench", Integer.valueOf(10));
            }
        }
        return enchantmentLevel(enchants, enchantmentId);
    }

    private static String armorProtectionDebug(Reflection ref, Object player) {
        StringBuilder builder = new StringBuilder();
        for (int slot = 0; slot < 4; slot++) {
            if (slot > 0) {
                builder.append("; ");
            }
            builder.append(slot).append('=');
            try {
                Object armor = ref.playerCurrentArmor.invoke(player, slot);
                if (armor == null) {
                    builder.append("null");
                    continue;
                }
                Object enchants = invokeFirstOptional(armor, new String[]{
                        "getEnchantmentTagList", "func_77986_q", "q"});
                int count = enchants == null ? -1 : intValue(invokeFirstOptional(enchants, new String[]{
                        "tagCount", "func_74745_c", "c"}), -1);
                builder.append(armor.getClass().getName())
                        .append(" prot=").append(itemProtectionLevel(armor))
                        .append(" enchCount=").append(count);
            } catch (Throwable t) {
                builder.append("error:").append(t.getClass().getSimpleName()).append(':').append(t.getMessage());
            }
        }
        return builder.toString();
    }

    private static int enchantmentHelperLevel(Object stack, int enchantmentId) {
        try {
            Method method = enchantmentHelperMethod(stack.getClass());
            if (method == null) {
                return -1;
            }
            Object result = method.invoke(null, stack);
            if (!(result instanceof Map)) {
                return -1;
            }
            Map<?, ?> map = (Map<?, ?>) result;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                int id = intValue(entry.getKey(), Integer.MIN_VALUE);
                int level = intValue(entry.getValue(), -1);
                if (id == enchantmentId) {
                    return level;
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static Method enchantmentHelperMethod(Class<?> itemStackClass) {
        if (enchantmentHelperLookupDone) {
            return enchantmentHelperGetEnchantments;
        }
        enchantmentHelperLookupDone = true;
        try {
            ClassLoader loader = itemStackClass.getClassLoader();
            Class<?> helper = Class.forName("net.minecraft.enchantment.EnchantmentHelper", false, loader);
            for (Method method : helper.getMethods()) {
                if (Map.class.isAssignableFrom(method.getReturnType())
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isAssignableFrom(itemStackClass)) {
                    method.setAccessible(true);
                    enchantmentHelperGetEnchantments = method;
                    return method;
                }
            }
            for (Method method : helper.getDeclaredMethods()) {
                if (Map.class.isAssignableFrom(method.getReturnType())
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isAssignableFrom(itemStackClass)) {
                    method.setAccessible(true);
                    enchantmentHelperGetEnchantments = method;
                    return method;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int enchantmentLevel(Object enchants, int enchantmentId) {
        if (enchants == null) {
            return -1;
        }
        int count = intValue(invokeFirstOptional(enchants, new String[]{
                "tagCount", "func_74745_c", "c"}), 0);
        int best = -1;
        for (int i = 0; i < count; i++) {
            Object entry = invokeFirstOptional(enchants, new String[]{
                    "getCompoundTagAt", "func_150305_b", "b"}, Integer.valueOf(i));
            if (entry == null) {
                entry = invokeFirstOptional(enchants, new String[]{
                        "get", "func_150305_b", "b"}, Integer.valueOf(i));
            }
            if (entry == null) {
                continue;
            }
            int id = nbtNumber(entry, "id");
            int level = nbtNumber(entry, "lvl");
            if (id == enchantmentId && level > best) {
                best = level;
            }
            int fromEntryText = enchantmentLevelFromText(String.valueOf(entry), enchantmentId);
            if (fromEntryText > best) {
                best = fromEntryText;
            }
        }
        int fromText = enchantmentLevelFromText(String.valueOf(enchants), enchantmentId);
        return fromText > best ? fromText : best;
    }

    private static int enchantmentLevelFromText(String text, int enchantmentId) {
        if (text == null || text.length() == 0) {
            return -1;
        }
        String id = String.valueOf(enchantmentId);
        Matcher matcher = Pattern.compile("id\\s*[:=]\\s*" + id + "\\D+lvl\\s*[:=]\\s*(\\d+)").matcher(text);
        if (!matcher.find()) {
            matcher = Pattern.compile("lvl\\s*[:=]\\s*(\\d+)\\D+id\\s*[:=]\\s*" + id).matcher(text);
            if (!matcher.find()) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int nbtNumber(Object compound, String key) {
        Object value = invokeFirstOptional(compound, new String[]{
                "getInteger", "func_74762_e", "f"}, key);
        int number = intValue(value, Integer.MIN_VALUE);
        if (number != Integer.MIN_VALUE) {
            return number;
        }
        value = invokeFirstOptional(compound, new String[]{
                "getShort", "func_74765_d", "e"}, key);
        return intValue(value, 0);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
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
                homeRecordedTicks = 0;
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

    private static boolean isHomeBedPresent(Reflection ref, Object world) {
        if (!homeSet || world == null) {
            return false;
        }
        try {
            Object pos = ref.blockPosCtorDdd.newInstance((double) homeX, (double) homeY, (double) homeZ);
            Object state = ref.worldBlockState.invoke(world, pos);
            Object block = ref.blockFromState.invoke(state);
            return block == ref.bedBlock;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void clearHomeState() {
        homeSet = false;
        homeX = 0;
        homeY = 0;
        homeZ = 0;
        homeTeamColor = -1;
        homeRecordedTicks = 0;
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

    private static String chatTeamColorCode(int color) {
        if (color == -1) {
            return "\u00a7f";
        }
        String team = teamName(color);
        if ("blue".equals(team)) {
            return "\u00a79";
        }
        if ("yellow".equals(team)) {
            return "\u00a7e";
        }
        if ("green".equals(team)) {
            return "\u00a7a";
        }
        return "\u00a7c";
    }

    private static String teamDebugName(int color) {
        return color == -1 ? "unknown" : teamName(color);
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

    private static void trackAttackDamage(Reflection ref, Object mc, Object world, Object self) {
        try {
            if (protectionSampleCooldownTicks > 0) {
                protectionSampleCooldownTicks--;
            }
            Object target = mouseOverPlayer(ref, mc, self);
            if (target == null) {
                target = aimedPlayerFallback(ref, world, self);
            }
            String targetUuid = target == null ? null : uuidWithoutDashes(getUuid(target));
            String targetName = target == null ? null : getPlayerName(target);
            int targetColor = target == null ? -1 : playerColor(ref, target);
            double targetHealth = target == null ? -1.0D : entityHealth(target);

            Object gameSettings = ref.mcGameSettings.get(mc);
            Object attackKey = gameSettings == null ? null : ref.gameSettingsAttackKey.get(gameSettings);
            boolean attackDown = attackKey != null && ((Boolean) ref.keyBindingIsDown.invoke(attackKey)).booleanValue();
            boolean critical = isLikelyCriticalHit(ref, self);
            if (attackDown && !damageAttackWasDown && target == null) {
                DamageProbe missed = new DamageProbe(null, "\u672a\u9501\u5b9a\u76ee\u6807", -1, null, -1,
                        new ArmorProfile("unknown", 7), swordProfile(ref, self), critical);
                postDamageDebugChat(ref, missed, -1, -1, null,
                        new ProtectionUpdate(false, "\u5ffd\u7565:\u51c6\u661f\u65e0\u76ee\u6807"));
                FireballPredictorAgentLog.write("damage probe miss: no target under crosshair");
            }
            if (attackDown && !damageAttackWasDown && target != null
                    && (damageProbe != null || protectionSampleCooldownTicks > 0)) {
                ArmorProfile armor = armorProfile(ref, target);
                SwordProfile sword = swordProfile(ref, self);
                DamageProbe skipped = new DamageProbe(targetUuid, targetName, targetColor, target, targetHealth, armor, sword, critical);
                postDamageDebugChat(ref, skipped, -1, targetHealth, null,
                        new ProtectionUpdate(false, damageProbe != null ? "\u5ffd\u7565:\u7b49\u5f85\u4e0a\u6b21" : "\u5ffd\u7565:\u51b7\u5374"));
            }
            if (attackDown && !damageAttackWasDown && target != null
                    && damageProbe == null && protectionSampleCooldownTicks <= 0) {
                double before = sameUuid(targetUuid, lastAimUuid) && lastAimHealth >= 0.0D ? lastAimHealth : targetHealth;
                ArmorProfile armor = armorProfile(ref, target);
                SwordProfile sword = swordProfile(ref, self);
                damageProbe = new DamageProbe(targetUuid, targetName, targetColor, target, before, armor, sword, critical);
                postDamageDebugChat(ref, damageProbe, -1, before, null,
                        new ProtectionUpdate(false, "\u5f00\u59cb:\u7b49\u5f85\u6263\u8840"));
                FireballPredictorAgentLog.write("damage probe start: target=" + targetName
                        + " team=" + teamDebugName(targetColor) + " entityHealthBefore=" + before
                        + " currentEntityHealth=" + targetHealth
                        + " armor=" + armor.label + " armorPoints=" + armor.points
                        + " sword=" + sword.label + " sharp=" + sword.sharpnessLevel
                        + " critical=" + critical
                        + " rawDamage=" + formatOneDecimal(effectiveAttackDamage(damageProbe)));
            }
            damageAttackWasDown = attackDown;

            if (damageProbe != null) {
                damageProbe.ticks++;
                double current = entityHealth(damageProbe.target);
                if (current < 0.0D) {
                    current = entityHealth(playerByUuid(ref, world, damageProbe.uuid));
                }
                if (current >= 0.0D && damageProbe.beforeHealth >= 0.0D && current < damageProbe.beforeHealth - DAMAGE_EPSILON) {
                    double damage = damageProbe.beforeHealth - current;
                    ProtectionGuess guess = inferProtectionLevel(damageProbe, damage);
                    ProtectionUpdate update = updateInferredProtection(damageProbe, damage, current, guess);
                    postDamageDebugChat(ref, damageProbe, damage, current, guess, update);
                    FireballPredictorAgentLog.write("damage probe result: target=" + damageProbe.name
                            + " team=" + teamDebugName(damageProbe.color)
                            + " before=" + formatOneDecimal(damageProbe.beforeHealth) + " after=" + formatOneDecimal(current)
                            + " damage=" + formatOneDecimal(damage)
                            + " armor=" + damageProbe.armor.label
                            + " armorPoints=" + damageProbe.armor.points
                            + " sword=" + damageProbe.sword.label
                            + " sharp=" + damageProbe.sword.sharpnessLevel
                            + " inferredProtection=" + (guess == null ? -1 : guess.level)
                            + " predictedDamage=" + (guess == null ? "unknown" : formatOneDecimal(guess.predictedDamage))
                            + " error=" + (guess == null ? "unknown" : formatOneDecimal(guess.error)));
                    damageProbe = null;
                    protectionSampleCooldownTicks = PROTECTION_SAMPLE_COOLDOWN_TICKS;
                } else if (damageProbe.ticks >= DAMAGE_PROBE_TICKS) {
                    postDamageDebugChat(ref, damageProbe, -1, current, null,
                            new ProtectionUpdate(false, current < 0.0D ? "\u8d85\u65f6:\u8840\u91cf\u6d88\u5931" : "\u8d85\u65f6:\u672a\u6263\u8840"));
                    FireballPredictorAgentLog.write("damage probe timeout: target=" + damageProbe.name
                            + " team=" + teamDebugName(damageProbe.color)
                            + " before=" + formatOneDecimal(damageProbe.beforeHealth) + " after=" + formatOneDecimal(current));
                    damageProbe = null;
                    protectionSampleCooldownTicks = PROTECTION_SAMPLE_COOLDOWN_TICKS;
                }
            }

            lastAimUuid = targetUuid;
            lastAimName = targetName;
            lastAimColor = targetColor;
            lastAimHealth = targetHealth;
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("damage probe error: " + t + " " + t.getMessage());
        }
    }

    private static void postDamageDebugChat(Reflection ref, DamageProbe probe, double observedDamage, double currentHealth,
                                            ProtectionGuess guess, ProtectionUpdate update) {
        if (!DAMAGE_DEBUG_CHAT) {
            return;
        }
        try {
            postLocalChat(ref.loader, "\u00a7b[Damage] \u00a7f" + probe.name
                    + " \u00a77\u4f24\u5bb3:\u00a7e" + debugNumber(observedDamage)
                    + " \u00a77\u8840\u91cf:\u00a7e" + debugNumber(probe.beforeHealth) + "\u00a77->\u00a7e" + debugNumber(currentHealth)
                    + " \u00a77\u5957\u88c5:\u00a7e" + armorChineseName(probe.armor.label)
                    + " \u00a77\u5251:\u00a7e" + swordChineseName(probe.sword.label)
                    + " \u00a77\u950b\u5229:\u00a7e" + (probe.sword.sharpnessLevel > 0 ? "\u6709" : "\u65e0")
                    + " \u00a77\u66b4\u51fb:\u00a7e" + (probe.critical ? "\u662f" : "\u5426")
                    + " \u00a77\u63a8\u7b97:\u00a7e" + (guess == null ? "?" : String.valueOf(guess.level))
                    + " \u00a77\u6837\u672c:\u00a7e" + (update == null ? "?" : update.message));
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("damage debug chat error: " + t + " " + t.getMessage());
        }
    }

    private static String debugNumber(double value) {
        return value < 0.0D ? "?" : formatOneDecimal(value);
    }

    private static String armorChineseName(String label) {
        if ("diamond".equals(label)) {
            return "\u94bb\u5957";
        }
        if ("iron".equals(label)) {
            return "\u94c1\u5957";
        }
        if ("leather".equals(label)) {
            return "\u76ae\u9769\u5957";
        }
        return "\u672a\u77e5";
    }

    private static String swordChineseName(String label) {
        if ("diamond_sword".equals(label)) {
            return "\u94bb\u77f3\u5251";
        }
        if ("iron_sword".equals(label)) {
            return "\u94c1\u5251";
        }
        if ("stone_sword".equals(label)) {
            return "\u77f3\u5251";
        }
        if ("wood_sword".equals(label)) {
            return "\u6728\u5251";
        }
        if ("gold_sword".equals(label)) {
            return "\u91d1\u5251";
        }
        if ("hand".equals(label)) {
            return "\u7a7a\u624b";
        }
        return "\u672a\u77e5";
    }

    private static Object mouseOverPlayer(Reflection ref, Object mc, Object self) throws Exception {
        if (ref.movingObjectEntityHit == null) {
            return null;
        }
        Object mouseOver = ref.mcObjectMouseOver.get(mc);
        if (mouseOver == null) {
            return null;
        }
        Object entity = ref.movingObjectEntityHit.get(mouseOver);
        if (entity == null || !ref.playerClass.isInstance(entity)) {
            return null;
        }
        String selfUuid = uuidWithoutDashes(getUuid(self));
        String entityUuid = uuidWithoutDashes(getUuid(entity));
        if (selfUuid != null && selfUuid.equals(entityUuid)) {
            return null;
        }
        return entity;
    }

    private static Object aimedPlayerFallback(Reflection ref, Object world, Object self) throws Exception {
        Object best = null;
        double bestScore = 0.985D;
        String selfUuid = uuidWithoutDashes(getUuid(self));
        List<?> players = (List<?>) ref.worldPlayerEntities.get(world);
        for (Object entity : players) {
            if (!ref.playerClass.isInstance(entity)) {
                continue;
            }
            String uuid = uuidWithoutDashes(getUuid(entity));
            if (uuid != null && uuid.equals(selfUuid)) {
                continue;
            }
            if (distanceBetween(ref, self, entity) > 6) {
                continue;
            }
            double score = aimDot(ref, self, entity);
            if (score > bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        if (best != null) {
            FireballPredictorAgentLog.write("damage probe fallback target: " + getPlayerName(best)
                    + " dot=" + formatOneDecimal(bestScore));
        }
        return best;
    }

    private static double aimDot(Reflection ref, Object self, Object target) throws Exception {
        Object look = ref.entityLook.invoke(self, 1.0F);
        double px = ref.entityPosX.getDouble(self);
        double py = ref.entityPosY.getDouble(self) + ((Float) ref.entityEyeHeight.invoke(self)).floatValue();
        double pz = ref.entityPosZ.getDouble(self);
        double tx = ref.entityPosX.getDouble(target);
        double ty = ref.entityPosY.getDouble(target) + ((Float) ref.entityEyeHeight.invoke(target)).floatValue() * 0.75D;
        double tz = ref.entityPosZ.getDouble(target);
        double dx = tx - px;
        double dy = ty - py;
        double dz = tz - pz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001D) {
            return -1.0D;
        }
        return ref.vecX.getDouble(look) * (dx / len)
                + ref.vecY.getDouble(look) * (dy / len)
                + ref.vecZ.getDouble(look) * (dz / len);
    }

    private static int scoreboardHealth(Reflection ref, Object world, String playerName) {
        if (playerName == null || playerName.length() == 0) {
            return -1;
        }
        try {
            Object scoreboard = invokeFirstOptional(world, new String[]{"getScoreboard", "func_96441_U", "ae"});
            if (scoreboard == null) {
                return -1;
            }
            int[] slots = new int[]{0, 2, 1};
            for (int slot : slots) {
                Object objective = invokeFirstOptional(scoreboard, new String[]{"getObjectiveInDisplaySlot", "func_96539_a", "a"}, Integer.valueOf(slot));
                if (objective == null) {
                    continue;
                }
                Object score = invokeFirstOptional(scoreboard, new String[]{"getValueFromObjective", "func_96529_a", "a"}, playerName, objective);
                if (score == null) {
                    continue;
                }
                int value = intValue(invokeFirstOptional(score, new String[]{"getScorePoints", "func_96652_c", "c"}), Integer.MIN_VALUE);
                if (value != Integer.MIN_VALUE) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static double entityHealth(Object entity) {
        if (entity == null) {
            return -1.0D;
        }
        Object value = invokeFirstOptional(entity, new String[]{"getHealth", "func_110143_aJ", "bn", "br"});
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return -1.0D;
    }

    private static Object playerByUuid(Reflection ref, Object world, String uuid) {
        if (uuid == null || uuid.length() == 0) {
            return null;
        }
        try {
            List<?> players = (List<?>) ref.worldPlayerEntities.get(world);
            if (players == null) {
                return null;
            }
            for (Object player : players) {
                String playerUuid = uuidWithoutDashes(getUuid(player));
                if (uuid.equals(playerUuid)) {
                    return player;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ArmorProfile armorProfile(Reflection ref, Object player) {
        try {
            Object boots = ref.playerCurrentArmor.invoke(player, 0);
            Object leggings = ref.playerCurrentArmor.invoke(player, 1);
            int bootsPoints = armorPiecePoints(ref, boots, 1);
            int leggingsPoints = armorPiecePoints(ref, leggings, 2);
            String bootsType = armorPieceType(ref, boots, "leather");
            String leggingsType = armorPieceType(ref, leggings, "leather");
            String label;
            if (bootsType.startsWith("diamond") || leggingsType.startsWith("diamond")) {
                label = "diamond";
            } else if (bootsType.startsWith("iron") || leggingsType.startsWith("iron")) {
                label = "iron";
            } else {
                label = "leather";
            }
            return new ArmorProfile(label, 4 + bootsPoints + leggingsPoints);
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("armor profile error: " + t + " " + t.getMessage());
            return new ArmorProfile("unknown", 7);
        }
    }

    private static int armorPiecePoints(Reflection ref, Object stack, int leatherFallback) {
        String type = armorPieceType(ref, stack, "leather");
        if ("diamond_boots".equals(type)) {
            return 3;
        }
        if ("diamond_leggings".equals(type)) {
            return 6;
        }
        if ("iron_boots".equals(type)) {
            return 2;
        }
        if ("iron_leggings".equals(type)) {
            return 5;
        }
        if ("leather_boots".equals(type)) {
            return 1;
        }
        if ("leather_leggings".equals(type)) {
            return 2;
        }
        return leatherFallback;
    }

    private static String armorPieceType(Reflection ref, Object stack, String fallback) {
        String text = itemIdentity(ref, stack);
        if (text.indexOf("diamond_boots") >= 0 || text.indexOf("bootsdiamond") >= 0) {
            return "diamond_boots";
        }
        if (text.indexOf("diamond_leggings") >= 0 || text.indexOf("leggingsdiamond") >= 0) {
            return "diamond_leggings";
        }
        if (text.indexOf("iron_boots") >= 0 || text.indexOf("bootsiron") >= 0) {
            return "iron_boots";
        }
        if (text.indexOf("iron_leggings") >= 0 || text.indexOf("leggingsiron") >= 0) {
            return "iron_leggings";
        }
        if (text.indexOf("leather_boots") >= 0 || text.indexOf("bootscloth") >= 0 || text.indexOf("bootsleather") >= 0) {
            return "leather_boots";
        }
        if (text.indexOf("leather_leggings") >= 0 || text.indexOf("leggingscloth") >= 0 || text.indexOf("leggingsleather") >= 0) {
            return "leather_leggings";
        }
        return fallback;
    }

    private static SwordProfile swordProfile(Reflection ref, Object self) {
        try {
            Object held = ref.playerHeldItem.invoke(self);
            String text = itemIdentity(ref, held);
            double damage = 1.0D;
            String label = "hand";
            if (text.indexOf("diamond_sword") >= 0 || text.indexOf("sworddiamond") >= 0) {
                damage = 7.0D;
                label = "diamond_sword";
            } else if (text.indexOf("iron_sword") >= 0 || text.indexOf("swordiron") >= 0) {
                damage = 6.0D;
                label = "iron_sword";
            } else if (text.indexOf("stone_sword") >= 0 || text.indexOf("swordstone") >= 0) {
                damage = 5.0D;
                label = "stone_sword";
            } else if (text.indexOf("wooden_sword") >= 0 || text.indexOf("wood_sword") >= 0
                    || text.indexOf("swordwood") >= 0 || text.indexOf("golden_sword") >= 0
                    || text.indexOf("gold_sword") >= 0 || text.indexOf("swordgold") >= 0) {
                damage = 4.0D;
                label = text.indexOf("gold") >= 0 ? "gold_sword" : "wood_sword";
            }
            int sharpness = Math.min(1, Math.max(0, itemEnchantmentLevel(held, 16)));
            if (sharpness > 0) {
                damage += 1.25D;
            }
            return new SwordProfile(label, damage, sharpness);
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("sword profile error: " + t + " " + t.getMessage());
            return new SwordProfile("unknown", 4.0D, 0);
        }
    }

    private static boolean isLikelyCriticalHit(Reflection ref, Object self) {
        try {
            float fallDistance = ref.entityFallDistance == null ? 0.0F : ref.entityFallDistance.getFloat(self);
            boolean onGround = ref.entityOnGround != null && ref.entityOnGround.getBoolean(self);
            double motionY = ref.entityMotionY.getDouble(self);
            return fallDistance > 0.0F && !onGround && motionY < 0.0D;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static double effectiveAttackDamage(DamageProbe probe) {
        return probe.sword.damage * (probe.critical ? 1.5D : 1.0D);
    }

    private static ProtectionGuess inferProtectionLevel(DamageProbe probe, double observedDamage) {
        if (probe.critical) {
            return null;
        }
        ProtectionGuess ruleGuess = inferProtectionFromRuleTable(probe, observedDamage);
        if (ruleGuess != null) {
            return ruleGuess;
        }
        return inferProtectionLevel(effectiveAttackDamage(probe), probe.armor.points, observedDamage);
    }

    private static ProtectionGuess inferProtectionFromRuleTable(DamageProbe probe, double observedDamage) {
        if (observedDamage <= 0.0D) {
            return null;
        }
        ProtectionGuess leatherDecimalGuess = inferLeatherArmorDecimalRule(probe, observedDamage);
        if (leatherDecimalGuess != null) {
            return leatherDecimalGuess;
        }
        ProtectionGuess ironDecimalGuess = inferIronArmorDecimalRule(probe, observedDamage);
        if (ironDecimalGuess != null) {
            return ironDecimalGuess;
        }
        ProtectionGuess measuredGuess = inferMeasuredProtectionZeroOrOne(probe, observedDamage);
        if (measuredGuess != null && measuredGuess.error <= 0.65D) {
            return measuredGuess;
        }
        ProtectionGuess minimumGuess = inferProtectionFromMinimumDamage(probe, observedDamage);
        if (minimumGuess != null && minimumGuess.error <= 0.75D) {
            return minimumGuess;
        }
        ProtectionGuess best = null;
        for (int level = 0; level <= 4; level++) {
            int[] damages = protectionRuleDamages(probe.armor.label, probe.sword.label,
                    probe.sword.sharpnessLevel, probe.critical, level);
            if (damages == null || damages.length == 0) {
                continue;
            }
            double error = damageRuleError(damages, observedDamage);
            if (level >= 2) {
                error += 0.35D + (level - 2) * 0.20D;
            }
            ProtectionGuess candidate = new ProtectionGuess(level, observedDamage, error);
            if (best == null || candidate.error < best.error
                    || (Math.abs(candidate.error - best.error) < 0.0001D && candidate.level < best.level)) {
                best = candidate;
            }
        }
        return best;
    }

    private static ProtectionGuess inferLeatherArmorDecimalRule(DamageProbe probe, double observedDamage) {
        if (!"leather".equals(probe.armor.label) || probe.sword.sharpnessLevel != 0 || probe.critical) {
            return null;
        }
        String sword = probe.sword.label;
        if ("gold_sword".equals(sword)) {
            sword = "wood_sword";
        }
        if ("wood_sword".equals(sword)) {
            if (observedDamage <= 3.35D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.75D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("stone_sword".equals(sword)) {
            if (observedDamage <= 4.10D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 4.50D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("iron_sword".equals(sword)) {
            if (observedDamage <= 4.70D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 5.20D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("diamond_sword".equals(sword)) {
            if (observedDamage <= 5.40D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 6.00D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        }
        return null;
    }

    private static ProtectionGuess inferIronArmorDecimalRule(DamageProbe probe, double observedDamage) {
        if (!"iron".equals(probe.armor.label) || probe.sword.sharpnessLevel != 0 || probe.critical) {
            return null;
        }
        String sword = probe.sword.label;
        if ("gold_sword".equals(sword)) {
            sword = "wood_sword";
        }
        if ("wood_sword".equals(sword)) {
            if (observedDamage <= 1.05D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.30D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.60D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
        } else if ("stone_sword".equals(sword)) {
            if (observedDamage <= 1.25D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.85D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.30D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
        } else if ("iron_sword".equals(sword)) {
            if (observedDamage <= 1.55D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.35D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.80D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
        } else if ("diamond_sword".equals(sword)) {
            if (observedDamage <= 2.55D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.60D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 4.25D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
        }
        return null;
    }

    private static ProtectionGuess inferMeasuredProtectionZeroOrOne(DamageProbe probe, double observedDamage) {
        ProtectionGuess best = null;
        for (int level = 0; level <= 1; level++) {
            int[] damages = protectionRuleDamages(probe.armor.label, probe.sword.label,
                    probe.sword.sharpnessLevel, probe.critical, level);
            if (damages == null || damages.length == 0) {
                continue;
            }
            double error = damageRuleError(damages, observedDamage);
            ProtectionGuess candidate = new ProtectionGuess(level, observedDamage, error);
            if (best == null || candidate.error < best.error
                    || (Math.abs(candidate.error - best.error) < 0.0001D && candidate.level < best.level)) {
                best = candidate;
            }
        }
        return best;
    }

    private static ProtectionGuess inferProtectionFromMinimumDamage(DamageProbe probe, double observedDamage) {
        ProtectionGuess best = null;
        for (int level = 0; level <= 4; level++) {
            int[] damages = protectionRuleDamages(probe.armor.label, probe.sword.label,
                    probe.sword.sharpnessLevel, probe.critical, level);
            if (damages == null || damages.length == 0) {
                continue;
            }
            double minimum = minDamage(damages);
            double error = Math.abs(observedDamage - minimum);
            if (observedDamage > minimum) {
                error += (observedDamage - minimum) * 0.35D;
            }
            if (level >= 2) {
                error += 0.20D + (level - 2) * 0.15D;
            }
            ProtectionGuess candidate = new ProtectionGuess(level, minimum, error);
            if (best == null || candidate.error < best.error
                    || (Math.abs(candidate.error - best.error) < 0.0001D && candidate.level < best.level)) {
                best = candidate;
            }
        }
        return best;
    }

    private static double minDamage(int[] damages) {
        int min = damages[0];
        for (int damage : damages) {
            if (damage < min) {
                min = damage;
            }
        }
        return (double) min;
    }

    private static double damageRuleError(int[] damages, double observedDamage) {
        double bestDistance = Double.MAX_VALUE;
        for (int damage : damages) {
            double distance = Math.abs((double) damage - observedDamage);
            if (distance < bestDistance) {
                bestDistance = distance;
            }
        }
        return bestDistance;
    }

    private static int[] protectionRuleDamages(String armor, String sword, int sharpness, boolean critical, int level) {
        if (sharpness != 0 || critical) {
            return null;
        }
        if ("gold_sword".equals(sword)) {
            sword = "wood_sword";
        }
        if (level == 0) {
            return protectionZeroDamages(armor, sword, sharpness, critical);
        }
        if (level == 1) {
            return protectionOneDamages(armor, sword, sharpness, critical);
        }
        int[] baseline = protectionZeroDamages(armor, sword, sharpness, critical);
        if (baseline == null) {
            return null;
        }
        return scaledProtectionDamages(baseline, level);
    }

    private static int[] protectionZeroDamages(String armor, String sword, int sharpness, boolean critical) {
        if (sharpness != 0 || critical) {
            return null;
        }
        if ("gold_sword".equals(sword)) {
            sword = "wood_sword";
        }
        if ("leather".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{4, 7, 8, 10, 11};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{5, 8, 9, 13};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{5, 6, 10};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{5, 6, 11, 12};
            }
        }
        if ("iron".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{1, 3, 4, 6};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{4, 6, 7, 10};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{3, 4, 7, 8};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{3, 5, 9, 13};
            }
        }
        if ("diamond".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{2, 3, 4, 5, 6};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{2, 5, 6};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{3, 4, 6, 7, 9, 10};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{4, 7, 8};
            }
        }
        return null;
    }

    private static int[] protectionOneDamages(String armor, String sword, int sharpness, boolean critical) {
        if (sharpness != 0 || critical) {
            return null;
        }
        if ("gold_sword".equals(sword)) {
            sword = "wood_sword";
        }
        if ("leather".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{2, 3, 4, 5, 6, 7, 10};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{4, 7, 8};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{4, 5, 9};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{4, 5, 6, 10, 11};
            }
        }
        if ("iron".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{2, 3, 4, 5, 6};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{3, 4, 5, 6, 8};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{3, 4, 6, 7, 11};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{1, 4, 7, 8, 12};
            }
        }
        if ("diamond".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new int[]{1, 2, 3, 4, 5};
            }
            if ("stone_sword".equals(sword)) {
                return new int[]{2, 4, 5, 8};
            }
            if ("iron_sword".equals(sword)) {
                return new int[]{2, 3, 6};
            }
            if ("diamond_sword".equals(sword)) {
                return new int[]{3, 4, 6, 7, 9};
            }
        }
        return null;
    }

    private static int[] scaledProtectionDamages(int[] baseline, int level) {
        double lowScale;
        double highScale;
        if (level == 2) {
            lowScale = 0.64D;
            highScale = 0.80D;
        } else if (level == 3) {
            lowScale = 0.40D;
            highScale = 0.68D;
        } else {
            lowScale = 0.20D;
            highScale = 0.60D;
        }
        boolean[] seen = new boolean[32];
        int count = 0;
        for (int value : baseline) {
            int low = Math.max(1, (int) Math.round(value * lowScale));
            int high = Math.max(low, (int) Math.round(value * highScale));
            for (int damage = low; damage <= high && damage < seen.length; damage++) {
                if (!seen[damage]) {
                    seen[damage] = true;
                    count++;
                }
            }
        }
        int[] result = new int[count];
        int index = 0;
        for (int damage = 1; damage < seen.length; damage++) {
            if (seen[damage]) {
                result[index++] = damage;
            }
        }
        return result;
    }

    private static ProtectionGuess inferProtectionLevel(double rawDamage, int armorPoints, double observedDamage) {
        if (rawDamage <= 0.0D || observedDamage <= 0.0D || armorPoints < 0) {
            return null;
        }
        ProtectionGuess best = null;
        for (int level = 0; level <= 4; level++) {
            DamageRange range = predictedDamageRange(rawDamage, armorPoints, level);
            double predicted = (range.low + range.high) / 2.0D;
            double error = rangeError(observedDamage, range.low, range.high);
            if (best == null || error < best.error
                    || (Math.abs(error - best.error) < 0.0001D && level > best.level)) {
                best = new ProtectionGuess(level, predicted, error);
            }
        }
        return best;
    }

    private static ProtectionUpdate updateInferredProtection(DamageProbe probe, double observedDamage, double currentHealth, ProtectionGuess guess) {
        if (guess == null || probe.color == -1) {
            return new ProtectionUpdate(false, "\u5ffd\u7565:\u65e0\u6cd5\u63a8\u7b97");
        }
        double attackDamage = effectiveAttackDamage(probe);
        String team = teamName(probe.color);
        PROTECTION_VISIBLE_ENEMY_TEAMS.add(team);
        if (isProtectionZeroLocked()) {
            setProtectionZeroForTeam(team);
            FireballPredictorAgentLog.write("protection zero locked after bed: team=" + team
                    + " ticks=" + homeRecordedTicks
                    + " target=" + probe.name
                    + " observedDamage=" + formatOneDecimal(observedDamage)
                    + " armor=" + probe.armor.label
                    + " sword=" + probe.sword.label);
            return new ProtectionUpdate(true, "\u5f00\u5c40\u9501\u5b9a\u21920");
        }
        List<ProtectionSample> samples = PROTECTION_SAMPLES_BY_TEAM.get(team);
        if (samples == null) {
            samples = new ArrayList<ProtectionSample>();
            PROTECTION_SAMPLES_BY_TEAM.put(team, samples);
        }
        Integer previous = PROTECTION_BY_TEAM.get(team);
        int previousLevel = previous == null ? -1 : previous.intValue();
        guess = adjustProtectionGuessWithProgression(probe, observedDamage, guess, previousLevel);
        ProtectionSample acceptedSample = new ProtectionSample(attackDamage, probe.armor.points, observedDamage, guess.level);
        samples.add(acceptedSample);
        while (samples.size() > PROTECTION_SAMPLE_LIMIT) {
            samples.remove(0);
        }
        updateProtectionProbability(team, samples);
        ProtectionProbability probability = PROTECTION_PROBABILITY_BY_TEAM.get(team);
        ProtectionGuess voted = inferProtectionFromSamples(samples, previousLevel);
        if (voted != null && probability != null && probability.firstLevel >= 0) {
            voted = new ProtectionGuess(probability.firstLevel, voted.predictedDamage, voted.error);
        }
        if (voted != null) {
            PROTECTION_BY_TEAM.put(team, Integer.valueOf(voted.level));
            FireballPredictorAgentLog.write("protection inferred: team=" + team
                    + " level=" + voted.level
                    + " singleGuess=" + guess.level
                    + " samples=" + samples.size()
                    + " target=" + probe.name
                    + " observedDamage=" + formatOneDecimal(observedDamage)
                    + " health=" + formatOneDecimal(probe.beforeHealth) + "->" + formatOneDecimal(currentHealth)
                    + " armor=" + probe.armor.label
                    + " armorPoints=" + probe.armor.points
                    + " sword=" + probe.sword.label
                    + " sharp=" + probe.sword.sharpnessLevel
                    + " critical=" + probe.critical
                    + " predictedDamage=" + formatOneDecimal(voted.predictedDamage)
                    + " error=" + formatOneDecimal(voted.error));
            pushProtectionShare(team, acceptedSample);
            return new ProtectionUpdate(true, "\u5165\u6837" + samples.size() + "\u2192" + voted.level);
        }
        return new ProtectionUpdate(false, "\u5ffd\u7565:\u6837\u672c\u5f02\u5e38");
    }

    private static void tickProtectionShare(Reflection ref) {
        drainRemoteProtectionSamples();
        if (++protectionShareCounter < PROTECTION_SHARE_SYNC_INTERVAL_TICKS) {
            return;
        }
        protectionShareCounter = 0;
        final String gameId = protectionShareGameId();
        if (gameId.length() == 0 || protectionShareBusy) {
            return;
        }
        protectionShareBusy = true;
        final long since = protectionShareSince;
        PROTECTION_SHARE_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = sendProtectionShareLine("PULL " + gameId + " " + since + " "
                            + PROTECTION_SHARE_SENDER_ID + " " + PROTECTION_SHARE_ROOM);
                    if (response == null || response.indexOf("\"ok\":true") < 0) {
                        FireballPredictorAgentLog.write("protection share pull failed: " + response);
                        return;
                    }
                    long serverTime = (long) readJsonNumber(response, "serverTime", since);
                    List<String> objects = readJsonObjectsInArray(response, "samples");
                    if (!objects.isEmpty()) {
                        synchronized (REMOTE_PROTECTION_PENDING) {
                            for (String object : objects) {
                                RemoteProtectionSample sample = RemoteProtectionSample.fromJson(object);
                                if (sample != null) {
                                    REMOTE_PROTECTION_PENDING.add(sample);
                                }
                            }
                        }
                    }
                    protectionShareSince = Math.max(protectionShareSince, serverTime);
                } catch (Throwable t) {
                    FireballPredictorAgentLog.write("protection share pull error: " + t + " " + t.getMessage());
                } finally {
                    protectionShareBusy = false;
                }
            }
        });
    }

    private static void pushProtectionShare(final String team, final ProtectionSample sample) {
        final String gameId = protectionShareGameId();
        if (gameId.length() == 0 || team == null || sample == null) {
            return;
        }
        PROTECTION_SHARE_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = "{\"room\":\"" + PROTECTION_SHARE_ROOM
                            + "\",\"gameId\":\"" + jsonEscape(gameId)
                            + "\",\"senderId\":\"" + PROTECTION_SHARE_SENDER_ID
                            + "\",\"team\":\"" + jsonEscape(team)
                            + "\",\"rawDamage\":" + formatOneDecimal(sample.rawDamage)
                            + ",\"armorPoints\":" + sample.armorPoints
                            + ",\"observedDamage\":" + formatOneDecimal(sample.observedDamage)
                            + ",\"guessedLevel\":" + sample.guessedLevel + "}";
                    String response = sendProtectionShareLine("PUSH " + json);
                    if (response == null || response.indexOf("\"ok\":true") < 0) {
                        FireballPredictorAgentLog.write("protection share push failed: " + response);
                    }
                } catch (Throwable t) {
                    FireballPredictorAgentLog.write("protection share push error: " + t + " " + t.getMessage());
                }
            }
        });
    }

    private static void drainRemoteProtectionSamples() {
        List<RemoteProtectionSample> pending;
        synchronized (REMOTE_PROTECTION_PENDING) {
            if (REMOTE_PROTECTION_PENDING.isEmpty()) {
                return;
            }
            pending = new ArrayList<RemoteProtectionSample>(REMOTE_PROTECTION_PENDING);
            REMOTE_PROTECTION_PENDING.clear();
        }
        for (RemoteProtectionSample remote : pending) {
            if (remote.team == null || remote.team.length() == 0 || teamColor(remote.team) == myColor) {
                continue;
            }
            PROTECTION_VISIBLE_ENEMY_TEAMS.add(remote.team);
            List<ProtectionSample> samples = PROTECTION_SAMPLES_BY_TEAM.get(remote.team);
            if (samples == null) {
                samples = new ArrayList<ProtectionSample>();
                PROTECTION_SAMPLES_BY_TEAM.put(remote.team, samples);
            }
            samples.add(new ProtectionSample(remote.rawDamage, remote.armorPoints,
                    remote.observedDamage, remote.guessedLevel));
            while (samples.size() > PROTECTION_SAMPLE_LIMIT) {
                samples.remove(0);
            }
            updateProtectionProbability(remote.team, samples);
            ProtectionProbability probability = PROTECTION_PROBABILITY_BY_TEAM.get(remote.team);
            if (probability != null && probability.firstLevel >= 0) {
                PROTECTION_BY_TEAM.put(remote.team, Integer.valueOf(probability.firstLevel));
                FireballPredictorAgentLog.write("protection share merged: team=" + remote.team
                        + " level=" + probability.firstLevel
                        + " first=" + probability.firstPercent
                        + " second=" + probability.secondLevel + ":" + probability.secondPercent);
            }
        }
    }

    private static String protectionShareGameId() {
        if (hypixelStatsGameKey != null && hypixelStatsGameKey.trim().length() > 0) {
            return "players-" + sanitizeShareToken(hypixelStatsGameKey);
        }
        if (!homeSet) {
            return "";
        }
        int bucketX = Math.floorDiv(homeX, 4);
        int bucketZ = Math.floorDiv(homeZ, 4);
        return "bed-" + homeTeamColor + "-" + bucketX + "-" + homeY + "-" + bucketZ;
    }

    private static String sendProtectionShareLine(String line) throws IOException {
        Socket socket = new Socket();
        BufferedWriter writer = null;
        BufferedReader reader = null;
        try {
            socket.connect(new InetSocketAddress(PROTECTION_SHARE_HOST, PROTECTION_SHARE_PORT),
                    PROTECTION_SHARE_CONNECT_TIMEOUT_MILLIS);
            socket.setSoTimeout(PROTECTION_SHARE_READ_TIMEOUT_MILLIS);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer.write(line);
            writer.write('\n');
            writer.flush();
            return reader.readLine();
        } finally {
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
        }
    }

    private static boolean isProtectionZeroLocked() {
        return homeSet && homeRecordedTicks < PROTECTION_ZERO_LOCK_TICKS_AFTER_BED;
    }

    private static void setProtectionZeroForTeam(String team) {
        if (team == null) {
            return;
        }
        PROTECTION_BY_TEAM.put(team, Integer.valueOf(0));
        PROTECTION_PROBABILITY_BY_TEAM.put(team, new ProtectionProbability(0, 100, -1, 0));
        PROTECTION_SAMPLES_BY_TEAM.remove(team);
    }

    private static void updateProtectionProbability(String team, List<ProtectionSample> samples) {
        int[] votes = protectionVotes(samples);
        int totalVotes = 0;
        for (int vote : votes) {
            totalVotes += vote;
        }
        if (totalVotes <= 0) {
            PROTECTION_PROBABILITY_BY_TEAM.remove(team);
            return;
        }
        int first = strongestProtectionVote(votes, -1);
        int second = strongestProtectionVote(votes, first);
        int firstPercent = percent(votes[first], totalVotes);
        int secondPercent = second < 0 ? 0 : percent(votes[second], totalVotes);
        PROTECTION_PROBABILITY_BY_TEAM.put(team,
                new ProtectionProbability(first, firstPercent, second, secondPercent));
    }

    private static ProtectionGuess adjustProtectionGuessWithProgression(DamageProbe probe, double observedDamage,
                                                                        ProtectionGuess guess, int previousLevel) {
        if (guess == null) {
            return null;
        }
        if (previousLevel >= 2 && "iron".equals(probe.armor.label)
                && probe.sword.sharpnessLevel == 0 && !probe.critical) {
            String sword = probe.sword.label;
            if ("gold_sword".equals(sword)) {
                sword = "wood_sword";
            }
            boolean lateLevelThreeWindow = isLateProtectionThreeWindow();
            boolean levelThreeEvidence =
                    ("wood_sword".equals(sword) && observedDamage <= 1.05D)
                            || ("stone_sword".equals(sword) && observedDamage <= 1.25D)
                            || ("iron_sword".equals(sword) && observedDamage <= 1.55D)
                            || ("diamond_sword".equals(sword) && observedDamage <= 2.55D)
                            || (lateLevelThreeWindow && "wood_sword".equals(sword) && observedDamage <= 1.70D)
                            || (lateLevelThreeWindow && "stone_sword".equals(sword) && observedDamage <= 2.60D)
                            || (lateLevelThreeWindow && "iron_sword".equals(sword) && observedDamage <= 2.85D)
                            || (lateLevelThreeWindow && "diamond_sword".equals(sword) && observedDamage <= 3.30D);
            if (levelThreeEvidence) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
        }
        return guess;
    }

    private static boolean isLateProtectionThreeWindow() {
        return homeSet && homeRecordedTicks >= PROTECTION_THREE_ECONOMY_TICKS_AFTER_BED;
    }


    private static ProtectionGuess inferProtectionFromSamples(List<ProtectionSample> samples, int preferredLevel) {
        if (samples == null || samples.isEmpty()) {
            return null;
        }
        int[] votes = protectionVotes(samples);
        int totalVotes = 0;
        for (int vote : votes) {
            totalVotes += vote;
        }
        if (totalVotes > 0) {
            int votedLevel = strongestProtectionVote(votes, -1);
            if (votedLevel > 0 && votes[votedLevel] < 2 && votes[0] > 0) {
                votedLevel = 0;
            }
            if (votedLevel > 0 && votes[votedLevel] < votes[0] && votes[votedLevel] < 3) {
                votedLevel = 0;
            }
            return new ProtectionGuess(votedLevel, 0.0D, 0.0D);
        }
        ProtectionGuess best = null;
        ProtectionGuess preferred = null;
        for (int level = 0; level <= 4; level++) {
            double totalError = 0.0D;
            double totalPredicted = 0.0D;
            for (ProtectionSample sample : samples) {
                DamageRange range = predictedDamageRange(sample.rawDamage, sample.armorPoints, level);
                totalPredicted += (range.low + range.high) / 2.0D;
                totalError += rangeError(sample.observedDamage, range.low, range.high);
            }
            double averagePredicted = totalPredicted / samples.size();
            ProtectionGuess candidate = new ProtectionGuess(level, averagePredicted, totalError);
            if (candidate.level == preferredLevel) {
                preferred = candidate;
            }
            if (best == null || candidate.error < best.error
                    || (Math.abs(candidate.error - best.error) < 0.0001D && candidate.level > best.level)) {
                best = candidate;
            }
        }
        if (preferred != null && best != null && preferred.error <= best.error + 0.25D) {
            return preferred;
        }
        return best;
    }

    private static int[] protectionVotes(List<ProtectionSample> samples) {
        int[] votes = new int[5];
        if (samples == null) {
            return votes;
        }
        for (ProtectionSample sample : samples) {
            if (sample.guessedLevel >= 0 && sample.guessedLevel <= 4) {
                votes[sample.guessedLevel]++;
            }
        }
        return votes;
    }

    private static int strongestProtectionVote(int[] votes, int excludedLevel) {
        int best = -1;
        for (int level = 0; level < votes.length; level++) {
            if (level == excludedLevel || votes[level] <= 0) {
                continue;
            }
            if (best < 0 || votes[level] > votes[best]
                    || (votes[level] == votes[best] && level < best)) {
                best = level;
            }
        }
        return best;
    }

    private static int percent(int count, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((count * 100.0D) / total);
    }

    private static DamageRange predictedDamageRange(double rawDamage, int armorPoints, int level) {
        double low = predictedDamageWithProtection(rawDamage, armorPoints, level, true);
        double high = predictedDamageWithProtection(rawDamage, armorPoints, level, false);
        return new DamageRange(Math.min(low, high), Math.max(low, high));
    }

    private static double predictedDamageWithProtection(double rawDamage, int armorPoints, int level, boolean strongestProtectionRoll) {
        double armorMultiplier = 1.0D - Math.min(20, Math.max(0, armorPoints)) * 0.04D;
        double afterArmor = rawDamage * armorMultiplier;
        int rawEpf = protectionEpf(level);
        int effectiveEpf;
        if (rawEpf <= 0) {
            effectiveEpf = 0;
        } else if (strongestProtectionRoll) {
            effectiveEpf = rawEpf;
        } else {
            effectiveEpf = (rawEpf + 1) / 2;
        }
        return afterArmor * (25.0D - effectiveEpf) / 25.0D;
    }

    private static int protectionEpf(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level == 1) {
            return 6;
        }
        if (level == 2) {
            return 9;
        }
        if (level == 3) {
            return 15;
        }
        return 20;
    }

    private static double rangeError(double observedDamage, double low, double high) {
        double min = Math.min(low, high);
        double max = Math.max(low, high);
        double roundedMin = Math.min(Math.round(min), Math.round(max));
        double roundedMax = Math.max(Math.round(min), Math.round(max));
        if (observedDamage >= roundedMin && observedDamage <= roundedMax) {
            return 0.0D;
        }
        if (observedDamage < roundedMin) {
            return roundedMin - observedDamage;
        }
        return observedDamage - roundedMax;
    }

    private static String itemIdentity(Reflection ref, Object stack) {
        if (stack == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try {
            builder.append(String.valueOf(invokeFirstOptional(stack, new String[]{"getUnlocalizedName", "func_77977_a", "a"})));
        } catch (Throwable ignored) {
        }
        try {
            Object item = ref.itemStackItem.invoke(stack);
            if (item != null) {
                builder.append(' ').append(String.valueOf(item));
                builder.append(' ').append(String.valueOf(invokeFirstOptional(item, new String[]{"getUnlocalizedName", "func_77658_a", "a"})));
            }
        } catch (Throwable ignored) {
        }
        return builder.toString().toLowerCase();
    }

    private static void scanHypixelStats(Reflection ref, Object mc, Object world, Object player) {
        try {
            HypixelStatsConfig config = hypixelStatsConfig();
            if (!config.enabled || config.apiKey.length() == 0 || myColor == -1
                    || !homeSet || !isHomeBedPresent(ref, world)) {
                weakEnemyWarningActive = false;
                hypixelStatsDetectedTicks = 0;
                return;
            }
            if (hypixelStatsWorld != world) {
                clearHypixelStatsState();
                hypixelStatsWorld = world;
            }

            List<PlayerSnapshot> enemies = collectEnemySnapshots(ref, mc, world, player);
            if (enemies.isEmpty()) {
                weakEnemyWarningActive = false;
                return;
            }
            if (enemies.size() > MAX_BEDWARS_ENEMIES || isLikelyHypixelLobby(collectScoreboardText(world))) {
                weakEnemyWarningActive = false;
                hypixelStatsDetectedTicks = 0;
                return;
            }
            hypixelStatsDetectedTicks += HYPIXEL_STATS_SCAN_INTERVAL_TICKS;
            if (!hypixelStatsQueryStarted) {
                hypixelStatsQueryStarted = true;
                hypixelStatsGameKey = gameKey(enemies);
                HYPIXEL_STATS_GAME_ENEMIES.clear();
            }
            mergePlayerSnapshots(HYPIXEL_STATS_GAME_ENEMIES, enemies, Integer.MAX_VALUE);
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
                    postHypixelStatsLine(ref.loader, cached.withName(enemy.name), enemy.color);
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
                postLocalChat(ref.loader, "\u00a7b[Stats] \u00a7f\u6b63\u5728\u67e5\u8be2\u5bf9\u9762\u73a9\u5bb6...");
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

    private static boolean isBedwarsMatch(List<String> screenText) {
        boolean sawBedwars = false;
        boolean sawMatchLine = false;
        for (String line : screenText) {
            String lower = stripFormatting(line).toLowerCase();
            if (lower.contains("bed wars") || lower.contains("bedwars") || lower.contains("\u8d77\u5e8a")) {
                sawBedwars = true;
            }
            if (lower.contains("red:") || lower.contains("blue:") || lower.contains("yellow:")
                    || lower.contains("green:") || lower.contains("\u7ea2\u961f")
                    || lower.contains("\u84dd\u961f") || lower.contains("\u9ec4\u961f")
                    || lower.contains("\u7eff\u961f") || lower.contains("\u94bb\u77f3")
                    || lower.contains("diamond") || lower.contains("emerald")) {
                sawMatchLine = true;
            }
        }
        return sawBedwars && sawMatchLine;
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

    private static boolean isLikelyHypixelLobby(List<String> lines) {
        boolean sawLobby = false;
        boolean sawLobbyOnlyMarker = false;
        for (String line : lines) {
            String lower = stripFormatting(line).toLowerCase();
            if (lower.contains("lobby")) {
                sawLobby = true;
            }
            if (lower.contains("coins") || lower.contains("loot chest")
                    || lower.contains("mystery dust") || lower.contains("www.hypixel.net")
                    || lower.contains("hypixel.net") || lower.contains("\u5927\u5385")) {
                sawLobbyOnlyMarker = true;
            }
        }
        return sawLobby || sawLobbyOnlyMarker;
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

    private static void postHypixelStatsLine(ClassLoader loader, HypixelStatsEntry entry, int color) {
        try {
            postLocalChat(loader, "\u00a7b[Stats] " + chatTeamColorCode(color) + entry.name
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

    private static String formatOneDecimal(double value) {
        return String.format(java.util.Locale.US, "%.1f", Double.valueOf(value));
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
        Exception lastError = null;
        int attempts = Math.max(1, config.apiKeys.length);
        for (int attempt = 0; attempt < attempts; attempt++) {
            String apiKey = nextHypixelApiKey(config);
            try {
                return fetchHypixelStatsWithKey(apiKey, player);
            } catch (HypixelHttpException e) {
                lastError = e;
                FireballPredictorAgentLog.write("hypixel stats key failed for " + player.name
                        + ": http=" + e.code + " key=" + maskApiKey(apiKey) + " body=" + e.body);
                if (e.code != 403 || e.body == null || e.body.indexOf("Invalid API key") < 0) {
                    throw e;
                }
            }
        }
        throw lastError == null ? new IllegalStateException("Hypixel stats fetch failed") : lastError;
    }

    private static HypixelStatsEntry fetchHypixelStatsWithKey(String apiKey, PlayerSnapshot player) throws Exception {
        URL url = new URL("https://api.hypixel.net/player?uuid=" + player.uuid);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Api-Key", apiKey);
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
            throw new HypixelHttpException(code, body.toString());
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

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
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

    private static String readJsonString(String json, String field) {
        String needle = "\"" + field + "\"";
        int index = json.indexOf(needle);
        if (index < 0) {
            return "";
        }
        int colon = json.indexOf(':', index + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return "";
    }

    private static List<String> readJsonObjectsInArray(String json, String field) {
        List<String> result = new ArrayList<String>();
        String needle = "\"" + field + "\"";
        int fieldIndex = json.indexOf(needle);
        if (fieldIndex < 0) {
            return result;
        }
        int arrayStart = json.indexOf('[', fieldIndex + needle.length());
        if (arrayStart < 0) {
            return result;
        }
        int depth = 0;
        int objectStart = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = arrayStart + 1; i < json.length(); i++) {
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
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    result.add(json.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            } else if (ch == ']' && depth == 0) {
                break;
            }
        }
        return result;
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

    private static void clearDamageProbeState() {
        damageAttackWasDown = false;
        damageProbe = null;
        lastAimUuid = null;
        lastAimName = null;
        lastAimColor = -1;
        lastAimHealth = -1.0D;
        protectionSampleCooldownTicks = 0;
    }

    private static void clearProtectionShareState() {
        protectionShareCounter = 0;
        protectionShareSince = 0L;
        protectionShareBusy = false;
        synchronized (REMOTE_PROTECTION_PENDING) {
            REMOTE_PROTECTION_PENDING.clear();
        }
    }

    private static String sanitizeShareToken(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private static String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private static boolean sameUuid(String a, String b) {
        return a != null && b != null && a.equals(b);
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
                postHypixelStatsLine(loader, entry, player.color);
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

    private static final class HypixelHttpException extends Exception {
        final int code;
        final String body;

        HypixelHttpException(int code, String body) {
            super("Hypixel HTTP " + code + ": " + body);
            this.code = code;
            this.body = body;
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

    private static final class DamageProbe {
        final String uuid;
        final String name;
        final int color;
        final Object target;
        final double beforeHealth;
        final ArmorProfile armor;
        final SwordProfile sword;
        final boolean critical;
        int ticks;

        DamageProbe(String uuid, String name, int color, Object target, double beforeHealth, ArmorProfile armor, SwordProfile sword, boolean critical) {
            this.uuid = uuid;
            this.name = name;
            this.color = color;
            this.target = target;
            this.beforeHealth = beforeHealth;
            this.armor = armor;
            this.sword = sword;
            this.critical = critical;
        }
    }

    private static final class ArmorProfile {
        final String label;
        final int points;

        ArmorProfile(String label, int points) {
            this.label = label;
            this.points = points;
        }
    }

    private static final class SwordProfile {
        final String label;
        final double damage;
        final int sharpnessLevel;

        SwordProfile(String label, double damage, int sharpnessLevel) {
            this.label = label;
            this.damage = damage;
            this.sharpnessLevel = sharpnessLevel;
        }
    }

    private static final class ProtectionGuess {
        final int level;
        final double predictedDamage;
        final double error;

        ProtectionGuess(int level, double predictedDamage, double error) {
            this.level = level;
            this.predictedDamage = predictedDamage;
            this.error = error;
        }
    }

    private static final class DamageRange {
        final double low;
        final double high;

        DamageRange(double low, double high) {
            this.low = low;
            this.high = high;
        }
    }

    private static final class ProtectionUpdate {
        final boolean accepted;
        final String message;

        ProtectionUpdate(boolean accepted, String message) {
            this.accepted = accepted;
            this.message = message;
        }
    }

    private static final class ProtectionSample {
        final double rawDamage;
        final int armorPoints;
        final double observedDamage;
        final int guessedLevel;

        ProtectionSample(double rawDamage, int armorPoints, double observedDamage, int guessedLevel) {
            this.rawDamage = rawDamage;
            this.armorPoints = armorPoints;
            this.observedDamage = observedDamage;
            this.guessedLevel = guessedLevel;
        }
    }

    private static final class RemoteProtectionSample {
        final String team;
        final double rawDamage;
        final int armorPoints;
        final double observedDamage;
        final int guessedLevel;

        RemoteProtectionSample(String team, double rawDamage, int armorPoints,
                               double observedDamage, int guessedLevel) {
            this.team = team;
            this.rawDamage = rawDamage;
            this.armorPoints = armorPoints;
            this.observedDamage = observedDamage;
            this.guessedLevel = guessedLevel;
        }

        static RemoteProtectionSample fromJson(String json) {
            String team = readJsonString(json, "team");
            int guessedLevel = (int) readJsonNumber(json, "guessedLevel", -1.0D);
            if (team.length() == 0 || guessedLevel < 0 || guessedLevel > 4) {
                return null;
            }
            return new RemoteProtectionSample(team,
                    readJsonNumber(json, "rawDamage", 0.0D),
                    (int) readJsonNumber(json, "armorPoints", 0.0D),
                    readJsonNumber(json, "observedDamage", 0.0D),
                    guessedLevel);
        }
    }

    private static final class ProtectionProbability {
        final int firstLevel;
        final int firstPercent;
        final int secondLevel;
        final int secondPercent;

        ProtectionProbability(int firstLevel, int firstPercent, int secondLevel, int secondPercent) {
            this.firstLevel = firstLevel;
            this.firstPercent = firstPercent;
            this.secondLevel = secondLevel;
            this.secondPercent = secondPercent;
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
        final Field entityFallDistance;
        final Field entityOnGround;
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
        final Field movingObjectEntityHit;

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
            entityFallDistance = optionalField(entityClass, "fallDistance", "O");
            entityOnGround = optionalField(entityClass, "onGround", "C");
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
            movingObjectEntityHit = optionalField(movingObjectClass, "entityHit", "d");

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

        private static Field optionalField(Class<?> owner, String... names) {
            for (String name : names) {
                try {
                    Field field = owner.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
            return null;
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
