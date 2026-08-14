package com.example.fireballpredictor.agent;

import net.minecraft.launchwrapper.IClassTransformer;

public final class FireballPredictorLaunchTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        String className = transformedName != null ? transformedName : name;
        return FireballPredictorPatcher.patch(className, basicClass);
    }
}
