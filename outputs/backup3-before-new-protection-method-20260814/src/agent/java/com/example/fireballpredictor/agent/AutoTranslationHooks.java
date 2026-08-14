package com.example.fireballpredictor.agent;

import org.universaltranslator.core.PersistentTranslationCache;
import org.universaltranslator.core.RenderTranslationSession;
import org.universaltranslator.core.TextKind;
import org.universaltranslator.core.TranslationCache;
import org.universaltranslator.core.TranslationDisplayMode;
import org.universaltranslator.core.TranslationProvider;
import org.universaltranslator.core.TranslationRequest;
import org.universaltranslator.core.TranslationStore;
import org.universaltranslator.core.TranslationTextColor;
import org.universaltranslator.core.TranslationTextStyling;
import org.universaltranslator.core.provider.FallbackTranslationProvider;
import org.universaltranslator.core.provider.LibreTranslateProvider;
import org.universaltranslator.core.provider.LlamaCppOfflineProvider;
import org.universaltranslator.core.provider.OpenAiChatTranslationProvider;
import org.universaltranslator.core.provider.TencentHunyuanProvider;
import org.universaltranslator.core.provider.YoudaoLlmTranslationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoTranslationHooks {
    private static final ThreadLocal<ArrayDeque<TextKind>> CONTEXT = new ThreadLocal<ArrayDeque<TextKind>>() {
        @Override
        protected ArrayDeque<TextKind> initialValue() {
            return new ArrayDeque<TextKind>();
        }
    };
    private static final ThreadLocal<Integer> TEXT_INPUT_DEPTH = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    private static volatile boolean initialized;
    private static volatile boolean enabled;
    private static volatile boolean translateChat;
    private static volatile boolean translateOther;
    private static volatile String targetLanguage = "zh-CN";
    private static volatile TranslationTextColor textColor = TranslationTextColor.AQUA;
    private static volatile TranslationProvider provider;
    private static volatile RenderTranslationSession session;
    private static volatile boolean glossaryEnabled;
    private static volatile List<GlossaryTerm> glossaryTerms = Collections.emptyList();
    private static final ExecutorService CHAT_TRANSLATOR = Executors.newSingleThreadExecutor();
    private static final Pattern PARTY_RATIO = Pattern.compile("(^|[^A-Za-z0-9_])([1-4])/([2-4])(?=$|[^0-9_])");
    private static final Pattern ANY_SHORT_MODE = Pattern.compile("(?i)(^|[^A-Za-z0-9_])(?:any\\s*1|anyone|any)\\s*([1-4])s\\??(?=$|[^A-Za-z0-9_])");
    private static final Pattern ANY_VERSUS_MODE = Pattern.compile("(?i)(^|[^A-Za-z0-9_])(?:any\\s*1|anyone|any)\\s*([1-4])v([1-4])\\??(?=$|[^A-Za-z0-9_])");
    private static final Pattern ANYONE_ABBREVIATION = Pattern.compile("(?i)(^|[^A-Za-z0-9_])any\\s*1\\??(?=$|[^A-Za-z0-9_])");
    private static final Pattern DEF_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])de+f+(?=$|[^A-Za-z0-9_])");
    private static final Pattern VC_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])vc+(?=$|[^A-Za-z0-9_])");
    private static final Pattern INV_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])inv+(?=$|[^A-Za-z0-9_])");
    private static final Pattern INC_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])inc+(?=$|[^A-Za-z0-9_])");
    private static final Pattern INC_SUFFIX_COUNT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])inc+\\s*([1-4])(?=$|[^A-Za-z0-9_])");
    private static final Pattern INC_PREFIX_COUNT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])([1-4])\\s*inc+(?=$|[^A-Za-z0-9_])");
    private static final Pattern PROT_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])prot+(?=$|[^A-Za-z0-9_])");
    private static final Pattern SHARP_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])sharp+(?=$|[^A-Za-z0-9_])");
    private static final Pattern TRAP_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])trap+(?=$|[^A-Za-z0-9_])");
    private static final Pattern RUSH_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])rush+(?=$|[^A-Za-z0-9_])");
    private static final Pattern MID_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])mid+(?=$|[^A-Za-z0-9_])");
    private static final Pattern BASE_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])base+(?=$|[^A-Za-z0-9_])");
    private static final Pattern VOID_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])void+(?=$|[^A-Za-z0-9_])");
    private static final Pattern GAP_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])gap+(?=$|[^A-Za-z0-9_])");
    private static final Pattern PEARL_SPAM = Pattern.compile("(?i)(^|[^A-Za-z0-9_])pearl+(?=$|[^A-Za-z0-9_])");
    private static final Pattern LF_COUNT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])lf\\s*([1-3])(?=$|[^A-Za-z0-9_])");
    private static final Pattern NEED_COUNT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])need\\s+([1-3])(?=$|[^A-Za-z0-9_])");
    private static final Pattern STAR_REQUIREMENT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])(\\d{2,4})\\s*(\\+?)\\s*stars?\\b");
    private static final Pattern FKDR_REQUIREMENT_PREFIX = Pattern.compile("(?i)(^|[^A-Za-z0-9_])(\\d+(?:\\.\\d+)?)\\s*fkdr\\s*(\\+?)");
    private static final Pattern FKDR_REQUIREMENT_SUFFIX = Pattern.compile("(?i)(^|[^A-Za-z0-9_])fkdr\\s*(\\d+(?:\\.\\d+)?)\\s*(\\+?)");
    private static final Pattern PLUS_FKDR_REQUIREMENT = Pattern.compile("(?i)(^|[^A-Za-z0-9_])(\\d+(?:\\.\\d+)?)\\s*\\+\\s*f[dk]dr\\b");
    private static final Pattern FDKR_TYPO = Pattern.compile("(?i)\\bfdkr\\b");
    private static final Pattern STABLE_LATIN_TERM = Pattern.compile(
            "(?i)\\b(FKDR|WLR|KDR|KD|KB|MVP|VIP|YT|TNT|FB|VC|Nick|Rank)\\+*\\b|\\b[1-4]v[1-4]\\b|\\b[1-4]s\\b");

    private AutoTranslationHooks() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Properties properties = loadProperties();
            enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
            translateChat = Boolean.parseBoolean(properties.getProperty("translate-chat", "true"));
            translateOther = Boolean.parseBoolean(properties.getProperty("translate-other", "true"));
            targetLanguage = properties.getProperty("target-language", "zh-CN").trim();
            textColor = TranslationTextColor.fromConfig(properties.getProperty("translated-text-color", "aqua"));
            glossaryEnabled = Boolean.parseBoolean(properties.getProperty("glossary-enabled", "true"));
            if (!enabled) {
                FireballPredictorAgentLog.write("translation disabled by config");
                return;
            }

            File configDirectory = configDirectory();
            glossaryTerms = loadGlossary(properties, configDirectory);
            provider = createProvider(properties, configDirectory);
            TranslationStore store = Boolean.parseBoolean(properties.getProperty("disk-cache", "true"))
                    ? new PersistentTranslationCache(new File(configDirectory,
                    "universal-translator-agent-test-cache.properties").toPath(), 10000)
                    : new TranslationCache(10000);
            int workers = provider.id().contains("offline-llama:") ? 1 : 2;
            session = new RenderTranslationSession(
                    provider,
                    "auto",
                    targetLanguage,
                    store,
                    workers,
                    TranslationDisplayMode.fromConfig(properties.getProperty("display-mode", "translated-only")),
                    Boolean.parseBoolean(properties.getProperty("translate-english-only", "true")));
            FireballPredictorAgentLog.write("translation initialized; provider=" + provider.id());
        } catch (Throwable t) {
            enabled = false;
            FireballPredictorAgentLog.write("translation init failed: " + t);
            System.err.println("[MC Auto Translation Tool Agent] init failed: " + t);
        }
    }

    public static String translate(String text) {
        if (TEXT_INPUT_DEPTH.get().intValue() > 0) {
            return text;
        }
        return translate(text, currentKind());
    }

    public static List<String> translateTooltipLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }
        pushTooltip();
        try {
            List<String> replacement = null;
            for (int i = 0; i < lines.size(); i++) {
                String original = lines.get(i);
                String translated = translate(original, TextKind.TOOLTIP);
                if (original == null ? translated != null : !original.equals(translated)) {
                    if (replacement == null) {
                        replacement = new ArrayList<String>(lines);
                    }
                    replacement.set(i, translated);
                }
            }
            return replacement == null ? lines : replacement;
        } finally {
            pop();
        }
    }

    public static List<String> translateItemTooltipLines(List<String> lines) {
        return translateTooltipLines(lines);
    }

    public static Object translateChatComponent(Object component) {
        if (component == null) {
            return null;
        }
        try {
            String text = componentText(component);
            if (!isLikelyPlayerChat(text)) {
                return component;
            }
            int split = text.indexOf(": ");
            String prefix = text.substring(0, split + 2);
            String message = text.substring(split + 2);
            String translated = translate(message, TextKind.CHAT);
            if (message.equals(translated)) {
                return component;
            }
            return newTextComponent(component, prefix + message + " \u00a78| \u00a7b" + translated + "\u00a7r");
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("translateChatComponent failed: " + t);
            return component;
        }
    }

    public static void observeChatComponent(Object component) {
        shouldDelayChatComponent(component);
    }

    public static boolean shouldDelayChatComponent(Object component) {
        try {
            if (component == null || !enabled || !translateChat || provider == null) {
                return false;
            }
            String text = componentText(component);
            if (!isLikelyPlayerChat(text)) {
                return false;
            }
            int split = text.indexOf(": ");
            String prefix = text.substring(0, split + 2);
            String message = text.substring(split + 2).trim();
            ClassLoader loader = component.getClass().getClassLoader();
            CHAT_TRANSLATOR.submit(new ChatTranslationTask(loader, prefix, message));
            return true;
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("shouldDelayChatComponent failed: " + t);
            return false;
        }
    }

    public static void pushChat() {
        CONTEXT.get().push(TextKind.CHAT);
    }

    public static void pushTooltip() {
        CONTEXT.get().push(TextKind.TOOLTIP);
    }

    public static void pushTextInput() {
        TEXT_INPUT_DEPTH.set(Integer.valueOf(TEXT_INPUT_DEPTH.get().intValue() + 1));
    }

    public static void popTextInput() {
        int depth = TEXT_INPUT_DEPTH.get().intValue();
        TEXT_INPUT_DEPTH.set(Integer.valueOf(Math.max(0, depth - 1)));
    }

    public static void pop() {
        ArrayDeque<TextKind> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    private static String translate(String text, TextKind kind) {
        RenderTranslationSession active = session;
        if (!enabled || active == null || text == null || !allows(kind)) {
            return text;
        }
        String translated = active.lookup(text, kind);
        if (text.equals(translated)) {
            return text;
        }
        return TranslationTextStyling.applyLegacyColor(translated, textColor);
    }

    private static TextKind currentKind() {
        ArrayDeque<TextKind> stack = CONTEXT.get();
        return stack.isEmpty() ? TextKind.OTHER : stack.peek();
    }

    private static boolean allows(TextKind kind) {
        return kind == TextKind.CHAT || kind == TextKind.SYSTEM_MESSAGE ? translateChat : translateOther;
    }

    private static boolean isLikelyPlayerChat(String text) {
        if (text == null || text.length() < 4 || text.length() > 300) {
            return false;
        }
        int split = text.indexOf(": ");
        if (split <= 0 || split > 90 || split + 2 >= text.length()) {
            return false;
        }
        String prefix = text.substring(0, split);
        String message = text.substring(split + 2).trim();
        if (message.length() < 2 || message.length() > 180) {
            return false;
        }
        if (containsHan(message)) {
            return false;
        }
        if (!containsTranslatableText(message) && !containsHypixelPattern(message)) {
            return false;
        }
        String lower = prefix.toLowerCase(java.util.Locale.ROOT);
        boolean shoutPrefix = lower.contains("shout") || prefix.contains("\u558a\u8bdd");
        if (!shoutPrefix && (lower.contains("server") || lower.contains("lobby") || lower.contains("bedwars"))) {
            return false;
        }
        return true;
    }

    private static boolean containsAsciiLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTranslatableText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAsciiWord(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '_';
    }

    private static boolean containsHypixelPattern(String text) {
        return PARTY_RATIO.matcher(text).find()
                || ANY_SHORT_MODE.matcher(text).find()
                || ANY_VERSUS_MODE.matcher(text).find();
    }

    private static boolean containsHan(String text) {
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(text.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                return true;
            }
        }
        return false;
    }

    private static String componentText(Object component) throws Exception {
        for (String name : new String[]{"getUnformattedText", "getFormattedText", "c", "d", "e"}) {
            try {
                Object value = component.getClass().getMethod(name).invoke(component);
                if (value instanceof String) {
                    return (String) value;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return String.valueOf(component);
    }

    private static Object newTextComponent(Object original, String text) throws Exception {
        ClassLoader loader = original.getClass().getClassLoader();
        Class<?> textComponent = loadFirst(loader, "net.minecraft.util.ChatComponentText", "fa");
        return textComponent.getConstructor(String.class).newInstance(text);
    }

    private static Class<?> loadFirst(ClassLoader loader, String... names) throws ClassNotFoundException {
        ClassNotFoundException failure = null;
        for (String name : names) {
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException e) {
                failure = e;
            }
        }
        throw failure;
    }

    private static final class ChatTranslationTask implements Runnable {
        private final ClassLoader loader;
        private final String prefix;
        private final String message;

        private ChatTranslationTask(ClassLoader loader, String prefix, String message) {
            this.loader = loader;
            this.prefix = prefix;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                TranslationProvider active = provider;
                if (active == null) {
                    return;
                }
                String prepared = applyHypixelTerminology(applyGlossary(message));
                String translated = containsTranslatableAscii(prepared)
                        ? active.translate(new TranslationRequest(prepared, "auto", targetLanguage, TextKind.CHAT))
                        : prepared;
                if (translated == null || translated.trim().isEmpty() || containsAsciiOnlyNoise(translated)) {
                    translated = message;
                }
                translated = applyHypixelTerminology(applyGlossary(translated));
                translated = TranslationTextStyling.applyLegacyColor(translated, textColor);
                postLocalChat(loader, "\u00a78[\u8bd1] \u00a7r" + prefix + message + " \u00a78| " + translated + "\u00a7r");
                FireballPredictorAgentLog.write("translated chat: " + message + " => " + translated);
            } catch (Throwable t) {
                FireballPredictorAgentLog.write("chat translation failed: " + t);
                try {
                    postLocalChat(loader, prefix + message);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean containsAsciiOnlyNoise(String text) {
        String trimmed = text.trim();
        return trimmed.equals("?") || trimmed.equals("??") || trimmed.equals("???")
                || trimmed.equalsIgnoreCase("null");
    }

    private static String applyGlossary(String text) {
        List<GlossaryTerm> terms = glossaryTerms;
        if (!glossaryEnabled || text == null || text.isEmpty() || terms.isEmpty()) {
            return text;
        }
        String result = text;
        for (GlossaryTerm term : terms) {
            result = term.replace(result);
        }
        return result;
    }

    private static String applyHypixelTerminology(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = replaceAnyModes(text);
        result = replaceAnyOneAbbreviation(result);
        result = replaceRepeatedCalls(result);
        result = FDKR_TYPO.matcher(result).replaceAll("FKDR");
        result = replacePartyRatios(result);
        result = replaceCountPattern(result, LF_COUNT, "\u7f3a");
        result = replaceCountPattern(result, NEED_COUNT, "\u6765");
        result = replaceStarRequirements(result);
        result = replacePlusFkdrRequirements(result);
        result = replaceFkdrRequirements(result, FKDR_REQUIREMENT_PREFIX, true);
        result = replaceFkdrRequirements(result, FKDR_REQUIREMENT_SUFFIX, false);
        return result;
    }

    private static String replaceRepeatedCalls(String text) {
        String result = replaceFixedPattern(text, DEF_SPAM, "\u5b88\u5bb6");
        result = replaceFixedPattern(result, VC_SPAM, "\u8bed\u97f3");
        result = replaceIncCounts(result, INC_SUFFIX_COUNT, true);
        result = replaceIncCounts(result, INC_PREFIX_COUNT, false);
        result = replaceFixedPattern(result, INV_SPAM, "\u6709\u9690\u8eab");
        result = replaceFixedPattern(result, INC_SPAM, "\u6709\u4eba\u6765\u8fdb\u653b\u4e86");
        result = replaceFixedPattern(result, PROT_SPAM, "\u4fdd\u62a4");
        result = replaceFixedPattern(result, SHARP_SPAM, "\u950b\u5229");
        result = replaceFixedPattern(result, TRAP_SPAM, "\u9677\u9631");
        result = replaceFixedPattern(result, RUSH_SPAM, "\u51b2/\u901f\u653b");
        result = replaceFixedPattern(result, MID_SPAM, "\u4e2d");
        result = replaceFixedPattern(result, BASE_SPAM, "\u5bb6");
        result = replaceFixedPattern(result, VOID_SPAM, "\u8df3\u865a\u7a7a\u56de\u5bb6");
        result = replaceFixedPattern(result, GAP_SPAM, "\u91d1\u82f9\u679c");
        result = replaceFixedPattern(result, PEARL_SPAM, "\u73cd\u73e0");
        return result;
    }

    private static String replaceIncCounts(String text, Pattern pattern, boolean suffixCount) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String count = suffixCount ? matcher.group(2) : matcher.group(2);
            String replacement = matcher.group(1) + "\u6709" + count + "\u4eba\u6765\u8fdb\u653b\u4e86";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceFixedPattern(String text, Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(1) + value));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceAnyModes(String text) {
        Matcher versus = ANY_VERSUS_MODE.matcher(text);
        StringBuffer output = null;
        while (versus.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String replacement = versus.group(1) + "\u6709\u4eba\u6765\u73a9"
                    + versus.group(2) + "V" + versus.group(3) + "\u5417";
            versus.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output != null) {
            versus.appendTail(output);
            text = output.toString();
        }

        Matcher shortMode = ANY_SHORT_MODE.matcher(text);
        output = null;
        while (shortMode.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String replacement = shortMode.group(1) + "\u6709\u4eba\u6765\u6253"
                    + shortMode.group(2) + "s\u5417";
            shortMode.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        shortMode.appendTail(output);
        return output.toString();
    }

    private static String replacePartyRatios(String text) {
        Matcher matcher = PARTY_RATIO.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            int current = Integer.parseInt(matcher.group(2));
            int target = Integer.parseInt(matcher.group(3));
            if (current > target) {
                continue;
            }
            if (output == null) {
                output = new StringBuffer();
            }
            int next = matcher.end();
            String separator = next < text.length() && isAsciiWord(text.charAt(next)) ? " " : "";
            String replacement = matcher.group(1) + chineseDigit(target) + "\u7b49" + chineseDigit(current) + separator;
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceAnyOneAbbreviation(String text) {
        Matcher matcher = ANYONE_ABBREVIATION.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(1) + "\u6709\u4eba\u5417"));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceCountPattern(String text, Pattern pattern, String verb) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            int count = Integer.parseInt(matcher.group(2));
            String replacement = matcher.group(1) + verb
                    + ("\u6765".equals(verb) ? chinesePersonCount(count) : chineseDigit(count));
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceStarRequirements(String text) {
        Matcher matcher = STAR_REQUIREMENT.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String plus = matcher.group(3) == null || matcher.group(3).isEmpty() ? "" : "+";
            String replacement = matcher.group(1) + matcher.group(2) + "\u661f" + plus;
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceFkdrRequirements(String text, Pattern pattern, boolean numberFirst) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String value = numberFirst ? matcher.group(2) : matcher.group(2);
            String plus = numberFirst ? matcher.group(3) : matcher.group(3);
            String replacement = matcher.group(1) + "FKDR" + spokenNumber(value)
                    + (plus == null || plus.isEmpty() ? "" : "\u4ee5\u4e0a");
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replacePlusFkdrRequirements(String text) {
        Matcher matcher = PLUS_FKDR_REQUIREMENT.matcher(text);
        StringBuffer output = null;
        while (matcher.find()) {
            if (output == null) {
                output = new StringBuffer();
            }
            String value = matcher.group(2);
            String replacement;
            if (looksLikeStarRequirement(value)) {
                replacement = matcher.group(1) + value + "\u661f+ FKDR";
            } else {
                replacement = matcher.group(1) + "FKDR" + spokenNumber(value) + "\u4ee5\u4e0a";
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        if (output == null) {
            return text;
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static boolean looksLikeStarRequirement(String value) {
        try {
            return Double.parseDouble(value) >= 50.0D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean containsTranslatableAscii(String text) {
        String stripped = STABLE_LATIN_TERM.matcher(text).replaceAll("");
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    private static String chineseDigit(int value) {
        switch (value) {
            case 0: return "\u96f6";
            case 1: return "\u4e00";
            case 2: return "\u4e8c";
            case 3: return "\u4e09";
            case 4: return "\u56db";
            case 5: return "\u4e94";
            case 6: return "\u516d";
            case 7: return "\u4e03";
            case 8: return "\u516b";
            case 9: return "\u4e5d";
            case 10: return "\u5341";
            default: return String.valueOf(value);
        }
    }

    private static String chinesePersonCount(int value) {
        switch (value) {
            case 1: return "\u4e00\u4e2a";
            case 2: return "\u4e24\u4e2a";
            case 3: return "\u4e09\u4e2a";
            default: return chineseDigit(value) + "\u4e2a";
        }
    }

    private static String spokenNumber(String value) {
        try {
            if (value.indexOf('.') < 0) {
                int integer = Integer.parseInt(value);
                if (integer >= 0 && integer <= 10) {
                    return chineseDigit(integer);
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    private static List<GlossaryTerm> loadGlossary(Properties properties, File configDirectory) throws Exception {
        if (!glossaryEnabled) {
            return Collections.emptyList();
        }
        List<GlossaryTerm> terms = new ArrayList<GlossaryTerm>();
        boolean caseSensitive = Boolean.parseBoolean(properties.getProperty("glossary-case-sensitive", "false"));

        String fileName = properties.getProperty("glossary-file", "universal-translator-glossary.properties").trim();
        File glossaryFile = new File(fileName);
        if (!glossaryFile.isAbsolute()) {
            glossaryFile = new File(configDirectory, fileName);
        }
        if (!glossaryFile.exists()) {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(glossaryFile), StandardCharsets.UTF_8);
            try {
                writer.write("# MC Auto Translation Tool glossary\r\n");
                writer.write("# Format: source=preferred translation\r\n");
                writeDefaultGlossary(writer);
            } finally {
                writer.close();
            }
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(glossaryFile), StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    separator = trimmed.indexOf(':');
                }
                if (separator <= 0) {
                    continue;
                }
                addGlossaryTerm(terms, trimmed.substring(0, separator),
                        trimmed.substring(separator + 1), caseSensitive);
            }
        } finally {
            reader.close();
        }
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith("glossary.term.")) {
                addGlossaryTerm(terms, name.substring("glossary.term.".length()),
                        properties.getProperty(name), caseSensitive);
            }
        }
        Collections.sort(terms, new Comparator<GlossaryTerm>() {
            @Override
            public int compare(GlossaryTerm left, GlossaryTerm right) {
                return right.source.length() - left.source.length();
            }
        });
        FireballPredictorAgentLog.write("translation glossary loaded; terms=" + terms.size()
                + "; file=" + glossaryFile.getAbsolutePath());
        return Collections.unmodifiableList(terms);
    }

    private static void writeDefaultGlossary(OutputStreamWriter writer) throws Exception {
        writer.write("1 inc=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("2 inc=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("inc1=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("inc2=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("inc3=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("inc4=鏈?浜烘潵杩涙敾浜哱r\n");
        writer.write("any 1s=鏈変汉鏉ユ墦1s鍚梊r\n");
        writer.write("any 2s=鏈変汉鏉ユ墦2s鍚梊r\n");
        writer.write("any 3s=鏈変汉鏉ユ墦3s鍚梊r\n");
        writer.write("any 4s=鏈変汉鏉ユ墦4s鍚梊r\n");
        writer.write("any 1v1=鏈変汉鏉ョ帺1V1鍚梊r\n");
        writer.write("any 2v2=鏈変汉鏉ョ帺2V2鍚梊r\n");
        writer.write("any 3v3=鏈変汉鏉ョ帺3V3鍚梊r\n");
        writer.write("any 4v4=鏈変汉鏉ョ帺4V4鍚梊r\n");
        writer.write("any1=鏈変汉鍚梊r\n");
        writer.write("LF1=缂轰竴\r\n");
        writer.write("LF2=缂轰簩\r\n");
        writer.write("bedwars=璧峰簥鎴樹簤\r\n");
        writer.write("party=杞r\n");
        writer.write("full party=婊¤溅\r\n");
        writer.write("sweat party=姹楄溅\r\n");
        writer.write("casual party=濞变箰杞r\n");
        writer.write("party me=鎷夋垜\r\n");
        writer.write("p me=鎷夋垜\r\n");
        writer.write("inv me=閭€璇锋垜\r\n");
        writer.write("inv=鏈夐殣韬玕r\n");
        writer.write("say 1=鎵?\r\n");
        writer.write("say me=鎶ュ悕瀛梊r\n");
        writer.write("say=鎵?\r\n");
        writer.write("full=婊′簡\r\n");
        writer.write("sry full=鎶辨瓑婊′簡\r\n");
        writer.write("slot=浣嶇疆\r\n");
        writer.write("vc=璇煶\r\n");
        writer.write("duo=鍙屾帓\r\n");
        writer.write("solo=鍗曟帓\r\n");
        writer.write("doubles=鍙屾帓\r\n");
        writer.write("trios=涓夋帓\r\n");
        writer.write("fours=鍥涙帓\r\n");
        writer.write("no cap=鐪熺殑\r\n");
        writer.write("rank=Rank\r\n");
        writer.write("default=鐧藉悕\r\n");
        writer.write("non=鐧藉悕鐜╁\r\n");
        writer.write("VIP+ Rank=VIP+ Rank\r\n");
        writer.write("MVP++ Rank=MVP++ Rank\r\n");
        writer.write("YT Rank=YT Rank\r\n");
        writer.write("MVP++=MVP++\r\n");
        writer.write("YouTube Rank=YT Rank\r\n");
        writer.write("staff=绠＄悊鍛榎r\n");
        writer.write("nicked=寮€Nick\r\n");
        writer.write("nick=Nick\r\n");
        writer.write("star=鏄焅r\n");
        writer.write("stars=鏄焅r\n");
        writer.write("prestige=鑽ｈ€€\r\n");
        writer.write("Iron Prestige=閾佽崳\r\n");
        writer.write("Gold Prestige=閲戣崳\r\n");
        writer.write("Diamond Prestige=閽昏崳\r\n");
        writer.write("Emerald Prestige=缁胯崳\r\n");
        writer.write("Sapphire Prestige=钃濊崳\r\n");
        writer.write("fkdr=FKDR\r\n");
        writer.write("wlr=WLR\r\n");
        writer.write("kdr=KD\r\n");
        writer.write("WS=杩炶儨\r\n");
        writer.write("stats=鏁版嵁\r\n");
        writer.write("500+=500鏄?\r\n");
        writer.write("good stats=鏁版嵁濂絓r\n");
        writer.write("bad stats=鏁版嵁宸甛r\n");
        writer.write("sweat=姹椾汉\r\n");
        writer.write("sweaty=寰堟睏\r\n");
        writer.write("tryhard=璁ょ湡鐜╁\r\n");
        writer.write("noob=鑿滈笩\r\n");
        writer.write("bot=浜烘満\r\n");
        writer.write("clutch=鏋侀檺鎿嶄綔\r\n");
        writer.write("carry=甯r\n");
        writer.write("skill issue=鎶€鏈棶棰榎r\n");
        writer.write("destroy=钖勭罕\r\n");
        writer.write("destroyed=钖勭罕/鎵撶垎\r\n");
        writer.write("wipe=娓呴槦\r\n");
        writer.write("teamwipe=鍥㈢伃\r\n");
        writer.write("inc=鏈変汉鏉ヨ繘鏀讳簡\r\n");
        writer.write("invis inc=闅愯韩鏉ヤ簡\r\n");
        writer.write("mid=涓璡r\n");
        writer.write("base=瀹禱r\n");
        writer.write("above=澶翠笂\r\n");
        writer.write("behind=鍚庨潰\r\n");
        writer.write("low=娈嬭\r\n");
        writer.write("one tap=涓€鍒€\r\n");
        writer.write("void=璺宠櫄绌哄洖瀹禱r\n");
        writer.write("go back=鍥炲幓\r\n");
        writer.write("defend=瀹圽r\n");
        writer.write("def=瀹堝\r\n");
        writer.write("rush=鍐?閫熸敾\r\n");
        writer.write("target=閽堝\r\n");
        writer.write("rotate=杞偣\r\n");
        writer.write("get bed=鎷嗗簥\r\n");
        writer.write("bed open=搴婂紑浜哱r\n");
        writer.write("free bed=鐧界粰搴奬r\n");
        writer.write("bed trade=鎹㈠簥\r\n");
        writer.write("pressure=鍘嬪姏\r\n");
        writer.write("skip=缁曡繃\r\n");
        writer.write("dia=閽籠r\n");
        writer.write("dias=閽籠r\n");
        writer.write("ems=缁縗r\n");
        writer.write("emerald=缁垮疂鐭砛r\n");
        writer.write("gen=璧勬簮鐐筡r\n");
        writer.write("split=鍒嗚祫婧怽r\n");
        writer.write("obby=榛戞洔鐭砛r\n");
        writer.write("prot=淇濇姢\r\n");
        writer.write("sharp=閿嬪埄\r\n");
        writer.write("trap=闄烽槺\r\n");
        writer.write("invis=闅愯韩\r\n");
        writer.write("jump=璺宠穬鑽痋r\n");
        writer.write("speed=閫熷害鑽痋r\n");
        writer.write("pots=鑽痋r\n");
        writer.write("gap=閲戣嫻鏋淺r\n");
        writer.write("pearl=鐝嶇彔\r\n");
        writer.write("TNT=TNT\r\n");
        writer.write("FB=鐏悆\r\n");
        writer.write("KB stick=鍑婚€€妫抃r\n");
        writer.write("final=缁堟潃\r\n");
        writer.write("final kill=缁堟潃\r\n");
        writer.write("finals=缁堟潃鏁癨r\n");
        writer.write("bed=搴奬r\n");
        writer.write("main=澶у彿\r\n");
        writer.write("alt=灏忓彿\r\n");
        writer.write("queue=鎺抃r\n");
        writer.write("solo queue=鍗曟帓\r\n");
        writer.write("speedbridge=閫熸惌\r\n");
        writer.write("ninja bridge=蹇嶆ˉ\r\n");
        writer.write("godbridge=绁炴ˉ\r\n");
        writer.write("breezily=breezily\r\n");
        writer.write("combo=杩炲嚮\r\n");
        writer.write("W-tap=W tap\r\n");
        writer.write("strafe=璧颁綅\r\n");
        writer.write("blockhit=鏍兼尅鐮峔r\n");
        writer.write("trade hits=鎹㈣\r\n");
        writer.write("KB=鍑婚€€\r\n");
        writer.write("ez=绠€鍗?澶彍\r\n");
        writer.write("L=杈撻夯浜哱r\n");
        writer.write("L player=鑿滈€糪r\n");
        writer.write("get good=缁冪粌鍚r\n");
        writer.write("trash=鍨冨溇\r\n");
        writer.write("bad=鑿淺r\n");
        writer.write("rekt=鎵撶垎浜哱r\n");
        writer.write("owned=琚暀鑲蹭簡\r\n");
        writer.write("cry=鍝幓鍚r\n");
        writer.write("cope=鍢寸‖\r\n");
        writer.write("mad=鐮撮槻浜哱r\n");
        writer.write("gimme VIP+ Rank plz=璇风粰鎴慥IP+ Rank\r\n");
        writer.write("stats?=鏁版嵁锛焅r\n");
        writer.write("what's your fkdr=浣燜KDR澶氬皯\r\n");
        writer.write("who wants party=璋佹兂缁勮溅\r\n");
        writer.write("join me=鍔犲叆鎴慭r\n");
        writer.write("I'm carrying=鎴戝甫\r\n");
        writer.write("don't chase=鍒拷\r\n");
        writer.write("nice clutch=婕備寒鐨勬瀬闄愭搷浣淺r\n");
        writer.write("gg=濂藉眬\r\n");
        writer.write("gg ez=濂藉眬锛堝甫鍢茶锛塡r\n");
    }

    private static void addGlossaryTerm(
            List<GlossaryTerm> terms,
            String source,
            String target,
            boolean caseSensitive
    ) {
        if (source == null || target == null) {
            return;
        }
        String normalizedSource = source.trim();
        String normalizedTarget = target.trim();
        if (normalizedSource.isEmpty() || normalizedTarget.isEmpty()) {
            return;
        }
        terms.add(new GlossaryTerm(normalizedSource, normalizedTarget, caseSensitive));
    }

    private static final class GlossaryTerm {
        private final String source;
        private final String target;
        private final String foldedSource;
        private final boolean caseSensitive;

        private GlossaryTerm(String source, String target, boolean caseSensitive) {
            this.source = source;
            this.target = target;
            this.caseSensitive = caseSensitive;
            this.foldedSource = caseSensitive ? source : source.toLowerCase(java.util.Locale.ROOT);
        }

        private String replace(String text) {
            String haystack = caseSensitive ? text : text.toLowerCase(java.util.Locale.ROOT);
            StringBuilder output = null;
            int index = 0;
            while (index < text.length()) {
                int match = haystack.indexOf(foldedSource, index);
                if (match < 0) {
                    break;
                }
                int end = match + source.length();
                if (!hasTermBoundary(text, match, end)) {
                    index = match + 1;
                    continue;
                }
                if (output == null) {
                    output = new StringBuilder(text.length() + target.length());
                }
                output.append(text, index, match).append(target);
                index = end;
            }
            if (output == null) {
                return text;
            }
            output.append(text, index, text.length());
            return output.toString();
        }

        private static boolean hasTermBoundary(String text, int start, int end) {
            boolean leftWord = start > 0 && isAsciiWord(text.charAt(start - 1));
            boolean rightWord = end < text.length() && isAsciiWord(text.charAt(end));
            boolean sourceStartsWord = isAsciiWord(text.charAt(start));
            boolean sourceEndsWord = isAsciiWord(text.charAt(end - 1));
            return (!sourceStartsWord || !leftWord) && (!sourceEndsWord || !rightWord);
        }

        private static boolean isAsciiWord(char value) {
            return (value >= 'a' && value <= 'z')
                    || (value >= 'A' && value <= 'Z')
                    || (value >= '0' && value <= '9')
                    || value == '_';
        }
    }

    private static void postLocalChat(final ClassLoader loader, final String text) throws Exception {
        final Class<?> minecraftClass = loadFirst(loader, "net.minecraft.client.Minecraft", "ave");
        Object minecraft = invokeFirstStatic(minecraftClass, new String[]{"getMinecraft", "func_71410_x", "A"});
        if (minecraft == null) {
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Object mc = invokeFirstStatic(minecraftClass, new String[]{"getMinecraft", "func_71410_x", "A"});
                    Object ingame = fieldFirst(mc, new String[]{"ingameGUI", "q"});
                    Object chatGui = invokeFirst(ingame, new String[]{"getChatGUI", "d"});
                    Object component = newTextComponentWithLoader(loader, text);
                    invokeFirst(chatGui, new String[]{"printChatMessage", "a"}, component);
                } catch (Throwable t) {
                    FireballPredictorAgentLog.write("postLocalChat task failed: " + t);
                }
            }
        };
        invokeFirst(minecraft, new String[]{"addScheduledTask", "func_152344_a", "a"}, task);
    }

    private static Object newTextComponentWithLoader(ClassLoader loader, String text) throws Exception {
        Class<?> textComponent = loadFirst(loader, "net.minecraft.util.ChatComponentText", "fa");
        return textComponent.getConstructor(String.class).newInstance(text);
    }

    private static Object fieldFirst(Object target, String[] names) throws Exception {
        Class<?> type = target.getClass();
        for (String name : names) {
            try {
                java.lang.reflect.Field field = type.getField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(java.util.Arrays.toString(names));
    }

    private static Object invokeFirstStatic(Class<?> type, String[] names) throws Exception {
        for (String name : names) {
            try {
                java.lang.reflect.Method method = type.getMethod(name);
                method.setAccessible(true);
                return method.invoke(null);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(java.util.Arrays.toString(names));
    }

    private static Object invokeFirst(Object target, String[] names, Object... args) throws Exception {
        Class<?> type = target.getClass();
        for (String name : names) {
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                    continue;
                }
                if (!parametersAccept(method.getParameterTypes(), args)) {
                    continue;
                }
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + java.util.Arrays.toString(names));
    }

    private static boolean parametersAccept(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] != null && !types[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static TranslationProvider createProvider(Properties properties, File configDirectory) {
        String provider = properties.getProperty("provider", "offline").trim();
        if ("offline".equalsIgnoreCase(provider)) {
            TranslationProvider local = LlamaCppOfflineProvider.forModel(
                    new File(configDirectory, "universal-translator-offline").toPath(),
                    Boolean.parseBoolean(properties.getProperty("offline-auto-download", "true")),
                    properties.getProperty("offline-model", "lite"));
            if (Boolean.parseBoolean(properties.getProperty("api-fallback", "false"))) {
                return new FallbackTranslationProvider(local, createApiProvider(
                        properties.getProperty("api-fallback-provider", "libretranslate"), properties));
            }
            return local;
        }
        return createApiProvider(provider, properties);
    }

    private static TranslationProvider createApiProvider(String provider, Properties properties) {
        if ("libretranslate".equalsIgnoreCase(provider)) {
            return new LibreTranslateProvider(
                    properties.getProperty("libretranslate-endpoint", "http://127.0.0.1:5000/translate").trim(),
                    properties.getProperty("api-key", "").trim());
        }
        if ("tencent-hunyuan".equalsIgnoreCase(provider)) {
            return new TencentHunyuanProvider(
                    properties.getProperty("tencent-secret-id", "").trim(),
                    properties.getProperty("tencent-secret-key", "").trim(),
                    properties.getProperty("tencent-model", "hunyuan-translation-lite").trim());
        }
        if ("youdao".equalsIgnoreCase(provider) || "youdao-llm".equalsIgnoreCase(provider)) {
            return new YoudaoLlmTranslationProvider(
                    properties.getProperty("youdao-endpoint",
                            "https://openapi.youdao.com/proxy/http/llm-trans").trim(),
                    properties.getProperty("youdao-app-key", "").trim(),
                    properties.getProperty("youdao-app-secret", "").trim(),
                    properties.getProperty("youdao-handle-option", "3").trim(),
                    properties.getProperty("youdao-prompt", "").trim());
        }
        if ("openai-chat".equalsIgnoreCase(provider)) {
            return new OpenAiChatTranslationProvider(
                    properties.getProperty("openai-endpoint", "https://api.openai.com/v1/chat/completions").trim(),
                    properties.getProperty("api-key", "").trim(),
                    properties.getProperty("openai-model", "gpt-4o-mini").trim(),
                    "openai-chat");
        }
        throw new IllegalArgumentException("Unsupported translation provider: " + provider);
    }

    private static Properties loadProperties() throws Exception {
        File directory = configDirectory();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create config directory: " + directory);
        }
        File file = new File(directory, "universal-translator-agent-test.properties");
        Properties defaults = defaults();
        if (!file.exists()) {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            try {
                defaults.store(writer, "MC Auto Translation Tool standalone agent test config");
            } finally {
                writer.close();
            }
            return defaults;
        }
        Properties loaded = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        try {
            loaded.load(reader);
        } finally {
            reader.close();
        }
        defaults.putAll(loaded);
        return defaults;
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "true");
        properties.setProperty("translate-chat", "true");
        properties.setProperty("translate-other", "true");
        properties.setProperty("target-language", "zh-CN");
        properties.setProperty("display-mode", "translated-only");
        properties.setProperty("translate-english-only", "false");
        properties.setProperty("translated-text-color", "aqua");
        properties.setProperty("glossary-enabled", "true");
        properties.setProperty("glossary-case-sensitive", "false");
        properties.setProperty("glossary-file", "universal-translator-glossary.properties");
        properties.setProperty("provider", "youdao");
        properties.setProperty("offline-auto-download", "true");
        properties.setProperty("offline-model", "lite");
        properties.setProperty("api-fallback", "false");
        properties.setProperty("api-fallback-provider", "libretranslate");
        properties.setProperty("libretranslate-endpoint", "http://127.0.0.1:5000/translate");
        properties.setProperty("api-key", "");
        properties.setProperty("tencent-secret-id", "");
        properties.setProperty("tencent-secret-key", "");
        properties.setProperty("tencent-model", "hunyuan-translation-lite");
        properties.setProperty("youdao-endpoint", "https://openapi.youdao.com/proxy/http/llm-trans");
        properties.setProperty("youdao-app-key", "4ba4f6d0ef2fa88e");
        properties.setProperty("youdao-app-secret", "jMwCiDu8F6CIlyPgMUQptqyqmANgCW7H");
        properties.setProperty("youdao-handle-option", "3");
        properties.setProperty("youdao-prompt", "");
        properties.setProperty("openai-endpoint", "https://api.openai.com/v1/chat/completions");
        properties.setProperty("openai-model", "gpt-4o-mini");
        properties.setProperty("disk-cache", "true");
        return properties;
    }

    private static File configDirectory() {
        String explicit = System.getProperty("universaltranslator.configDir");
        if (explicit != null && !explicit.trim().isEmpty()) {
            return new File(explicit.trim());
        }
        File userDirConfig = new File(System.getProperty("user.dir", "."), "config");
        if (userDirConfig.isDirectory()) {
            return userDirConfig;
        }
        return new File(new File(System.getProperty("user.home"), "AppData\\Roaming\\.minecraft"), "config");
    }
}
