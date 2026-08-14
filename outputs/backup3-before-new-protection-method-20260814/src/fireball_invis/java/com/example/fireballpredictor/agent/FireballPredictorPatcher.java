package com.example.fireballpredictor.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class FireballPredictorPatcher {
    private static final String HOOKS_CLASS = "com.example.fireballpredictor.agent.FireballPredictorHooks";

    private FireballPredictorPatcher() {
    }

    public static byte[] patch(String className, byte[] bytes) {
        if (bytes == null || className == null) {
            return bytes;
        }

        String normalized = className.replace('.', '/');
        boolean minecraft = "ave".equals(normalized) || "net/minecraft/client/Minecraft".equals(normalized);
        boolean guiIngame = "avo".equals(normalized) || "net/minecraft/client/gui/GuiIngame".equals(normalized);
        boolean netHandler = "bcy".equals(normalized) || "net/minecraft/client/network/NetHandlerPlayClient".equals(normalized);
        boolean entityRenderer = false;
        if (!minecraft && !guiIngame && !netHandler && !entityRenderer) {
            return bytes;
        }

        try {
            FireballPredictorAgentLog.write("patching " + className);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new HookClassVisitor(writer, minecraft, guiIngame, netHandler, entityRenderer);
            reader.accept(visitor, 0);
            FireballPredictorAgentLog.write("patched " + className);
            return writer.toByteArray();
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("patch failed for " + className + ": " + t + " " + t.getMessage());
            System.err.println("[FireballPredictor] Failed to patch " + className + ": " + t);
            return bytes;
        }
    }

    private static final class HookClassVisitor extends ClassVisitor {
        private final boolean minecraft;
        private final boolean guiIngame;
        private final boolean netHandler;
        private final boolean entityRenderer;

        HookClassVisitor(ClassVisitor delegate, boolean minecraft, boolean guiIngame, boolean netHandler, boolean entityRenderer) {
            super(Opcodes.ASM9, delegate);
            this.minecraft = minecraft;
            this.guiIngame = guiIngame;
            this.netHandler = netHandler;
            this.entityRenderer = entityRenderer;
        }

        @Override
        public MethodVisitor visitMethod(int access,
                                         String name,
                                         String descriptor,
                                         String signature,
                                         String[] exceptions) {
            MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
            boolean tick = minecraft && (("s".equals(name) && "()V".equals(descriptor))
                    || ("runTick".equals(name) && "()V".equals(descriptor)));
            boolean render = guiIngame && (("a".equals(name) && "(F)V".equals(descriptor))
                    || ("renderGameOverlay".equals(name) && "(F)V".equals(descriptor)));
            boolean explosion = netHandler && (("a".equals(name) && "(Lgk;)V".equals(descriptor))
                    || ("handleExplosion".equals(name)
                    && "(Lnet/minecraft/network/play/server/S27PacketExplosion;)V".equals(descriptor)));
            boolean worldRender = entityRenderer && (("b".equals(name) && "(FJ)V".equals(descriptor))
                    || ("renderWorld".equals(name) && "(FJ)V".equals(descriptor)));
            if (!tick && !render && !explosion && !worldRender) {
                return method;
            }
            return new HookMethodVisitor(method,
                    explosion ? "onExplosionPacket" : (worldRender ? "onRenderWorld" : (tick ? "onClientTick" : "onRenderOverlay")),
                    explosion ? 1 : (worldRender ? 1 : -1),
                    worldRender ? HookMethodVisitor.FLOAT_ARG : (explosion ? HookMethodVisitor.OBJECT_ARG : HookMethodVisitor.NO_ARG));
        }
    }

    private static final class HookMethodVisitor extends MethodVisitor {
        static final int NO_ARG = 0;
        static final int OBJECT_ARG = 1;
        static final int FLOAT_ARG = 2;

        private final String hookName;
        private final int argIndex;
        private final int argKind;

        HookMethodVisitor(MethodVisitor delegate, String hookName, int argIndex, int argKind) {
            super(Opcodes.ASM9, delegate);
            this.hookName = hookName;
            this.argIndex = argIndex;
            this.argKind = argKind;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                emitReflectiveHook();
            }
            super.visitInsn(opcode);
        }

        private void emitReflectiveHook() {
            super.visitLdcInsn(HOOKS_CLASS);
            super.visitInsn(Opcodes.ICONST_1);
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Class",
                    "forName",
                    "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
                    false);
            super.visitLdcInsn(hookName);
            if (argKind != NO_ARG) {
                super.visitInsn(Opcodes.ICONST_1);
                super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                super.visitInsn(Opcodes.DUP);
                super.visitInsn(Opcodes.ICONST_0);
                if (argKind == FLOAT_ARG) {
                    super.visitFieldInsn(
                            Opcodes.GETSTATIC,
                            "java/lang/Float",
                            "TYPE",
                            "Ljava/lang/Class;");
                } else {
                    super.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                }
                super.visitInsn(Opcodes.AASTORE);
            } else {
                super.visitInsn(Opcodes.ICONST_0);
                super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            }
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                    false);
            super.visitInsn(Opcodes.ACONST_NULL);
            if (argKind != NO_ARG) {
                super.visitInsn(Opcodes.ICONST_1);
                super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                super.visitInsn(Opcodes.DUP);
                super.visitInsn(Opcodes.ICONST_0);
                if (argKind == FLOAT_ARG) {
                    super.visitVarInsn(Opcodes.FLOAD, argIndex);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "java/lang/Float",
                            "valueOf",
                            "(F)Ljava/lang/Float;",
                            false);
                } else {
                    super.visitVarInsn(Opcodes.ALOAD, argIndex);
                }
                super.visitInsn(Opcodes.AASTORE);
            } else {
                super.visitInsn(Opcodes.ICONST_0);
                super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            }
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/reflect/Method",
                    "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                    false);
            super.visitInsn(Opcodes.POP);
        }
    }
}
