package com.example.fireballpredictor.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public final class FireballPredictorAgent {
    private FireballPredictorAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            FireballPredictorAgentLog.write("premain entered; args=" + agentArgs);
            if (agentArgs != null && agentArgs.contains("appendSystem")) {
                appendThisJar(instrumentation);
            }
            if (agentArgs != null && (agentArgs.contains("appendBootstrap") || agentArgs.contains("appendSystem"))) {
                appendThisJarToBootstrap(instrumentation);
            }
            AutoTranslationHooks.initialize();
            instrumentation.addTransformer(new FireballPredictorInstrumentationTransformer(), false);
            FireballPredictorAgentLog.write("transformer registered");
            System.out.println("[FireballPredictor] Java agent loaded for Minecraft 1.8.9.");
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("premain failed: " + t);
            t.printStackTrace();
        }
    }

    private static void appendThisJar(Instrumentation instrumentation) {
        try {
            File jar = new File(FireballPredictorAgent.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (jar.isFile()) {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar));
                FireballPredictorAgentLog.write("agent jar appended to system class loader: " + jar.getAbsolutePath());
            }
        } catch (URISyntaxException ignored) {
        } catch (Exception e) {
            FireballPredictorAgentLog.write("appendToSystemClassLoaderSearch failed: " + e);
            System.err.println("[FireballPredictor] Could not append agent jar to system class loader: " + e);
        }
    }

    private static void appendThisJarToBootstrap(Instrumentation instrumentation) {
        try {
            File jar = new File(FireballPredictorAgent.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (jar.isFile()) {
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(jar));
                FireballPredictorAgentLog.write("agent jar appended to bootstrap class loader: " + jar.getAbsolutePath());
            }
        } catch (URISyntaxException ignored) {
        } catch (Exception e) {
            FireballPredictorAgentLog.write("appendToBootstrapClassLoaderSearch failed: " + e);
            System.err.println("[FireballPredictor] Could not append agent jar to bootstrap class loader: " + e);
        }
    }
}
