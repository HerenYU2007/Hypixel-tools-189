package com.example.fireballpredictor.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public final class FireballPredictorInstrumentationTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] translated = AutoTranslationPatcher.patch(className, classfileBuffer);
        return FireballPredictorPatcher.patch(className, translated);
    }
}
