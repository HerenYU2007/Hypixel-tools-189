# Fireball Predictor Agent for Minecraft 1.8.9

This build keeps the Forge mod behavior but loads through an explicit startup hook instead of Forge.

Build:

```powershell
.\build-agent.ps1
```

Output:

```text
outputs\fireballpredictor-standalone-agent-1.8.9-1.1.0-5tick-3.25xwarning-multi.jar
```

Preferred JVM argument:

```text
-javaagent:C:\Users\97105\Documents\1.8.9\outputs\fireballpredictor-standalone-agent-1.8.9-1.1.0-5tick-3.25xwarning-multi.jar=appendBootstrap
```

LaunchWrapper alternative:

```text
--tweakClass com.example.fireballpredictor.agent.FireballPredictorTweaker
```

For `--tweakClass`, the jar also needs to be on the launch classpath. The `-javaagent` route is usually easier because the JVM loads the jar directly.

Notes:

- This targets Minecraft 1.8.9, including Lunar's MCP-named 1.8 runtime and vanilla obfuscated class names.
- It does not use Forge, runtime attach, or process injection.
- Third-party clients may block, ignore, or rewrite JVM arguments.
