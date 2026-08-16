package com.example.fireballpredictor.agent;

import java.util.List;

// 中文注释：保护推算的纯算法集中在这里。
// Hooks 负责采集血量和更新 HUD，本类只根据样本给出等级、概率和规则表结果。
final class ProtectionRules {
    private ProtectionRules() {
    }

    static double effectiveAttackDamage(DamageProbe probe) {
        return probe.sword.damage * (probe.critical ? 1.5D : 1.0D);
    }

    static ProtectionGuess inferProtectionLevel(DamageProbe probe, double observedDamage,
                                                boolean levelTwoWindow, boolean lateLevelThreeWindow) {
        if (probe.critical) {
            return null;
        }
        ProtectionGuess sharpOneOneGuess = inferSharpnessOneProtectionOneRule(probe, observedDamage);
        if (sharpOneOneGuess != null) {
            return sharpOneOneGuess;
        }
        ProtectionGuess sharpOneZeroGuess = inferSharpnessOneProtectionZeroRule(probe, observedDamage);
        if (sharpOneZeroGuess != null) {
            return sharpOneZeroGuess;
        }
        ProtectionGuess ruleGuess = inferProtectionFromRuleTable(probe, observedDamage, levelTwoWindow, lateLevelThreeWindow);
        if (ruleGuess != null) {
            return ruleGuess;
        }
        return inferProtectionLevel(effectiveAttackDamage(probe), probe.armor.points, observedDamage);
    }

    private static ProtectionGuess inferProtectionFromRuleTable(DamageProbe probe, double observedDamage,
                                                                boolean levelTwoWindow, boolean lateLevelThreeWindow) {
        if (observedDamage <= 0.0D) {
            return null;
        }
        ProtectionGuess leatherDecimalGuess = inferLeatherArmorDecimalRule(probe, observedDamage, levelTwoWindow, lateLevelThreeWindow);
        if (leatherDecimalGuess != null) {
            return leatherDecimalGuess;
        }
        ProtectionGuess ironDecimalGuess = inferIronArmorDecimalRule(probe, observedDamage);
        if (ironDecimalGuess != null) {
            return ironDecimalGuess;
        }
        ProtectionGuess diamondDecimalGuess = inferDiamondArmorDecimalRule(probe, observedDamage, levelTwoWindow, lateLevelThreeWindow);
        if (diamondDecimalGuess != null) {
            return diamondDecimalGuess;
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

    private static ProtectionGuess inferLeatherArmorDecimalRule(DamageProbe probe, double observedDamage,
                                                                boolean levelTwoWindow, boolean lateLevelThreeWindow) {
        if (!"leather".equals(probe.armor.label) || probe.sword.sharpnessLevel != 0 || probe.critical) {
            return null;
        }
        String sword = normalizedSword(probe.sword.label);
        if (lateLevelThreeWindow) {
            if ("wood_sword".equals(sword) && observedDamage <= 2.35D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if ("stone_sword".equals(sword) && observedDamage <= 2.90D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if ("iron_sword".equals(sword) && observedDamage <= 3.35D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if ("diamond_sword".equals(sword) && observedDamage <= 3.85D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
        }
        if (levelTwoWindow) {
            if ("wood_sword".equals(sword) && observedDamage <= 3.05D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if ("stone_sword".equals(sword) && observedDamage <= 3.70D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if ("iron_sword".equals(sword) && observedDamage <= 4.30D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if ("diamond_sword".equals(sword) && observedDamage <= 4.75D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
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
        String sword = normalizedSword(probe.sword.label);
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

    private static ProtectionGuess inferDiamondArmorDecimalRule(DamageProbe probe, double observedDamage,
                                                                boolean levelTwoWindow, boolean lateLevelThreeWindow) {
        if (!"diamond".equals(probe.armor.label) || probe.sword.sharpnessLevel != 0 || probe.critical) {
            return null;
        }
        String sword = normalizedSword(probe.sword.label);
        if ("wood_sword".equals(sword)) {
            if (lateLevelThreeWindow && observedDamage <= 1.85D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (levelTwoWindow && observedDamage <= 1.95D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.25D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.65D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("stone_sword".equals(sword)) {
            if (lateLevelThreeWindow && observedDamage <= 2.25D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (levelTwoWindow && observedDamage <= 2.35D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 2.70D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.15D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("iron_sword".equals(sword)) {
            if (lateLevelThreeWindow && observedDamage <= 2.65D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (levelTwoWindow && observedDamage <= 2.75D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.15D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.55D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
            }
        } else if ("diamond_sword".equals(sword)) {
            if (lateLevelThreeWindow && observedDamage <= 3.25D) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
            if (levelTwoWindow && observedDamage <= 3.15D) {
                return new ProtectionGuess(2, observedDamage, 0.0D);
            }
            if (observedDamage <= 3.55D) {
                return new ProtectionGuess(1, observedDamage, 0.0D);
            }
            if (observedDamage <= 4.05D) {
                return new ProtectionGuess(0, observedDamage, 0.0D);
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

    private static ProtectionGuess inferSharpnessOneProtectionZeroRule(DamageProbe probe, double observedDamage) {
        if (probe.sword.sharpnessLevel != 1 || probe.critical) {
            return null;
        }
        String sword = normalizedSword(probe.sword.label);
        double minimum = sharpnessOneProtectionZeroMinimum(probe.armor.label, sword);
        if (minimum <= 0.0D || observedDamage < minimum) {
            return null;
        }
        return new ProtectionGuess(0, observedDamage, Math.max(0.0D, observedDamage - minimum) * 0.05D);
    }

    private static ProtectionGuess inferSharpnessOneProtectionOneRule(DamageProbe probe, double observedDamage) {
        if (probe.sword.sharpnessLevel != 1 || probe.critical || observedDamage < 1.5D) {
            return null;
        }
        String sword = normalizedSword(probe.sword.label);
        DamageRange range = sharpnessOneProtectionOneMeasuredRange(probe.armor.label, sword);
        if (range == null || observedDamage < range.low || observedDamage > range.high) {
            return null;
        }
        double center = (range.low + range.high) / 2.0D;
        return new ProtectionGuess(1, center, Math.abs(observedDamage - center) * 0.05D);
    }

    private static DamageRange sharpnessOneProtectionOneMeasuredRange(String armor, String sword) {
        if ("leather".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new DamageRange(2.60D, 4.15D);
            }
            if ("stone_sword".equals(sword)) {
                return new DamageRange(3.40D, 4.85D);
            }
            if ("iron_sword".equals(sword)) {
                return new DamageRange(4.80D, 5.55D);
            }
            if ("diamond_sword".equals(sword)) {
                return new DamageRange(5.40D, 6.15D);
            }
        }
        if ("iron".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new DamageRange(2.50D, 3.25D);
            }
            if ("stone_sword".equals(sword)) {
                return new DamageRange(2.20D, 3.75D);
            }
            if ("iron_sword".equals(sword)) {
                return new DamageRange(3.70D, 4.35D);
            }
            if ("diamond_sword".equals(sword)) {
                return new DamageRange(3.60D, 4.85D);
            }
        }
        if ("diamond".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return new DamageRange(2.30D, 2.85D);
            }
            if ("stone_sword".equals(sword)) {
                return new DamageRange(2.00D, 3.25D);
            }
            if ("iron_sword".equals(sword)) {
                return new DamageRange(2.30D, 3.65D);
            }
            if ("diamond_sword".equals(sword)) {
                return new DamageRange(2.50D, 4.15D);
            }
        }
        return null;
    }

    private static double sharpnessOneProtectionZeroMinimum(String armor, String sword) {
        if ("leather".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return 4.20D;
            }
            if ("stone_sword".equals(sword)) {
                return 4.90D;
            }
            if ("iron_sword".equals(sword)) {
                return 5.60D;
            }
            if ("diamond_sword".equals(sword)) {
                return 5.40D;
            }
        }
        if ("iron".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return 2.40D;
            }
            if ("stone_sword".equals(sword)) {
                return 3.80D;
            }
            if ("iron_sword".equals(sword)) {
                return 4.30D;
            }
            if ("diamond_sword".equals(sword)) {
                return 5.00D;
            }
        }
        if ("diamond".equals(armor)) {
            if ("wood_sword".equals(sword)) {
                return 2.80D;
            }
            if ("stone_sword".equals(sword)) {
                return 3.30D;
            }
            if ("iron_sword".equals(sword)) {
                return 3.00D;
            }
            if ("diamond_sword".equals(sword)) {
                return 4.20D;
            }
        }
        return -1.0D;
    }

    private static int[] protectionRuleDamages(String armor, String sword, int sharpness, boolean critical, int level) {
        if (sharpness != 0 || critical) {
            return null;
        }
        sword = normalizedSword(sword);
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
        sword = normalizedSword(sword);
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
        sword = normalizedSword(sword);
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

    static ProtectionProbability probabilityFromSamples(List<ProtectionSample> samples) {
        int[] votes = protectionVotes(samples);
        int totalVotes = 0;
        for (int vote : votes) {
            totalVotes += vote;
        }
        if (totalVotes <= 0) {
            return null;
        }
        int first = strongestProtectionVote(votes, -1);
        int second = strongestProtectionVote(votes, first);
        int secondPercent = second < 0 ? 0 : percent(votes[second], totalVotes);
        return new ProtectionProbability(first, percent(votes[first], totalVotes), second, secondPercent);
    }

    static ProtectionGuess adjustWithProgression(DamageProbe probe, double observedDamage,
                                                 ProtectionGuess guess, int previousLevel,
                                                 boolean lateLevelThreeWindow) {
        if (guess == null) {
            return null;
        }
        if (previousLevel >= 2 && "iron".equals(probe.armor.label)
                && probe.sword.sharpnessLevel == 0 && !probe.critical) {
            String sword = normalizedSword(probe.sword.label);
            boolean levelThreeEvidence =
                    ("wood_sword".equals(sword) && observedDamage <= 1.05D)
                            || ("stone_sword".equals(sword) && observedDamage <= 1.25D)
                            || ("iron_sword".equals(sword) && observedDamage <= 1.55D)
                            || ("diamond_sword".equals(sword) && observedDamage <= 2.55D)
                            || (lateLevelThreeWindow && "wood_sword".equals(sword) && observedDamage <= 1.95D)
                            || (lateLevelThreeWindow && "stone_sword".equals(sword) && observedDamage <= 2.70D)
                            || (lateLevelThreeWindow && "iron_sword".equals(sword) && observedDamage <= 2.90D)
                            || (lateLevelThreeWindow && "diamond_sword".equals(sword) && observedDamage <= 3.45D);
            if (levelThreeEvidence) {
                return new ProtectionGuess(3, observedDamage, 0.0D);
            }
        }
        return guess;
    }

    static ProtectionGuess inferFromSamples(List<ProtectionSample> samples, int preferredLevel,
                                            boolean lateLevelThreeWindow) {
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
            if (votes[3] >= 2 && votes[2] <= 4) {
                votedLevel = 3;
            }
            if (lateLevelThreeWindow && votes[3] >= 1 && votes[2] <= 3 && votes[1] == 0) {
                votedLevel = 3;
            }
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

    private static String normalizedSword(String sword) {
        return "gold_sword".equals(sword) ? "wood_sword" : sword;
    }
}
