package com.example.fireballpredictor.agent;

import java.lang.instrument.Instrumentation;

public final class FireballPredictorNoopAgent {
    private FireballPredictorNoopAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        FireballPredictorAgentLog.write("noop premain entered; args=" + agentArgs);
        System.out.println("[FireballPredictor] No-op Java agent loaded.");
    }
}
