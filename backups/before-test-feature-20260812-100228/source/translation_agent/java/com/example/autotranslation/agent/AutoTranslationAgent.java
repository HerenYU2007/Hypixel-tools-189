package com.example.autotranslation.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public final class AutoTranslationAgent {
    private AutoTranslationAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            AutoTranslationAgentLog.write("premain entered; args=" + agentArgs);
            appendThisJarToBootstrap(instrumentation);
            AutoTranslationHooks.initialize();
            instrumentation.addTransformer(new AutoTranslationInstrumentationTransformer(), false);
            AutoTranslationAgentLog.write("transformer registered");
            System.out.println("[MC Auto Translation Tool Agent] loaded.");
        } catch (Throwable t) {
            AutoTranslationAgentLog.write("premain failed: " + t);
            t.printStackTrace();
        }
    }

    private static void appendThisJarToBootstrap(Instrumentation instrumentation) {
        try {
            File jar = new File(AutoTranslationAgent.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (jar.isFile()) {
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(jar));
                AutoTranslationAgentLog.write("agent jar appended to bootstrap class loader: " + jar.getAbsolutePath());
            }
        } catch (URISyntaxException ignored) {
        } catch (Exception e) {
            AutoTranslationAgentLog.write("appendToBootstrapClassLoaderSearch failed: " + e);
            System.err.println("[MC Auto Translation Tool Agent] bootstrap append failed: " + e);
        }
    }
}
