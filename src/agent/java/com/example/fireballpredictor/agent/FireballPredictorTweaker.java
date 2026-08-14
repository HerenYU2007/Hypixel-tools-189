package com.example.fireballpredictor.agent;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.Collections;
import java.util.List;

public final class FireballPredictorTweaker implements ITweaker {
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer("com.example.fireballpredictor.agent.FireballPredictorLaunchTransformer");
        System.out.println("[FireballPredictor] LaunchWrapper tweaker loaded for Minecraft 1.8.9.");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
