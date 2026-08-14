package com.example.fireballpredictor.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class AutoTranslationPatcher {
    private static final String HOOK_CLASS = "com.example.fireballpredictor.agent.AutoTranslationHooks";

    private AutoTranslationPatcher() {
    }

    public static byte[] patch(String className, byte[] bytes) {
        if (className == null || bytes == null) {
            return bytes;
        }
        String normalized = className.replace('.', '/');
        boolean netHandler = "net/minecraft/client/network/NetHandlerPlayClient".equals(normalized)
                || "bcy".equals(normalized);
        if (!netHandler) {
            return bytes;
        }
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    return "java/lang/Object";
                }
            };
            NetHandlerVisitor visitor = new NetHandlerVisitor(writer);
            reader.accept(visitor, ClassReader.SKIP_FRAMES);
            if (visitor.modifiedMethods() == 0) {
                FireballPredictorAgentLog.write("found no chat hook in " + className);
                return bytes;
            }
            FireballPredictorAgentLog.write("patched chat handler " + className);
            return writer.toByteArray();
        } catch (Throwable t) {
            FireballPredictorAgentLog.write("patch failed for " + className + ": " + t);
            return bytes;
        }
    }

    private static void emitGetMethod(MethodVisitor visitor, String methodName) {
        visitor.visitLdcInsn(HOOK_CLASS);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.ACONST_NULL);
        visitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
                false);
        visitor.visitLdcInsn(methodName);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
        visitor.visitInsn(Opcodes.AASTORE);
        visitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Class",
                "getMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
    }

    private static final class NetHandlerVisitor extends ClassVisitor {
        private int modifiedMethods;

        NetHandlerVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        int modifiedMethods() {
            return modifiedMethods;
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            boolean handleChat = ("handleChat".equals(name) || "a".equals(name))
                    && ("(Lnet/minecraft/network/play/server/S02PacketChat;)V".equals(descriptor)
                    || "(Lfy;)V".equals(descriptor));
            if (!handleChat) {
                return delegate;
            }
            modifiedMethods++;
            final boolean obfuscatedPacket = "(Lfy;)V".equals(descriptor);
            final String packetOwner = obfuscatedPacket
                    ? "fy"
                    : "net/minecraft/network/play/server/S02PacketChat";
            final String packetGetterName = obfuscatedPacket ? "a" : "getChatComponent";
            final String packetGetterDescriptor = obfuscatedPacket
                    ? "()Leu;"
                    : "()Lnet/minecraft/util/IChatComponent;";
            return new MethodVisitor(Opcodes.ASM9, delegate) {
                private boolean injectedAfterThreadCheck;
                private boolean emittingGeneratedCode;

                @Override
                public void visitMethodInsn(
                        int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (emittingGeneratedCode) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        return;
                    }

                    boolean threadCheck = !injectedAfterThreadCheck
                            && opcode == Opcodes.INVOKESTATIC
                            && (("fh".equals(owner) && "a".equals(name))
                            || ("net/minecraft/network/PacketThreadUtil".equals(owner)
                            && "checkThreadAndEnqueue".equals(name)));
                    if (threadCheck) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        injectedAfterThreadCheck = true;
                        emittingGeneratedCode = true;
                        try {
                            emitEarlyDelayedChat(this, packetOwner, packetGetterName, packetGetterDescriptor);
                        } finally {
                            emittingGeneratedCode = false;
                        }
                        return;
                    }

                    boolean normalChatSink = ("avt".equals(owner)
                            || "net/minecraft/client/gui/GuiNewChat".equals(owner))
                            && ("a".equals(name) || "printChatMessage".equals(name))
                            && ("(Leu;)V".equals(descriptor)
                            || "(Lnet/minecraft/util/IChatComponent;)V".equals(descriptor));
                    if (normalChatSink) {
                        emittingGeneratedCode = true;
                        try {
                            emitDelayedNormalChat(this, opcode, owner, name, descriptor, isInterface);
                        } finally {
                            emittingGeneratedCode = false;
                        }
                        return;
                    }

                    boolean overlayChatSink = ("avo".equals(owner)
                            || "net/minecraft/client/gui/GuiIngame".equals(owner))
                            && ("a".equals(name) || "setRecordPlaying".equals(name))
                            && ("(Leu;Z)V".equals(descriptor)
                            || "(Lnet/minecraft/util/IChatComponent;Z)V".equals(descriptor));
                    if (overlayChatSink) {
                        emittingGeneratedCode = true;
                        try {
                            emitDelayedOverlayChat(this, opcode, owner, name, descriptor, isInterface);
                        } finally {
                            emittingGeneratedCode = false;
                        }
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            };
        }

        private static void emitShouldDelayCall(MethodVisitor visitor) {
            emitGetMethod(visitor, "shouldDelayChatComponent");
            visitor.visitInsn(Opcodes.ACONST_NULL);
            visitor.visitInsn(Opcodes.ICONST_1);
            visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitInsn(Opcodes.AASTORE);
            visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/reflect/Method",
                    "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                    false);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
            visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Boolean",
                    "booleanValue",
                    "()Z",
                    false);
        }

        private static void emitEarlyDelayedChat(
                MethodVisitor visitor,
                String packetOwner,
                String packetGetterName,
                String packetGetterDescriptor) {
            org.objectweb.asm.Label showOriginal = new org.objectweb.asm.Label();
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    packetOwner,
                    packetGetterName,
                    packetGetterDescriptor,
                    false);
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            emitShouldDelayCall(visitor);
            visitor.visitJumpInsn(Opcodes.IFEQ, showOriginal);
            visitor.visitInsn(Opcodes.RETURN);
            visitor.visitLabel(showOriginal);
        }

        private static void emitDelayedNormalChat(
                MethodVisitor visitor,
                int opcode,
                String owner,
                String name,
                String descriptor,
                boolean isInterface) {
            org.objectweb.asm.Label showOriginal = new org.objectweb.asm.Label();
            org.objectweb.asm.Label after = new org.objectweb.asm.Label();
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            visitor.visitVarInsn(Opcodes.ASTORE, 3);
            emitShouldDelayCall(visitor);
            visitor.visitJumpInsn(Opcodes.IFEQ, showOriginal);
            visitor.visitJumpInsn(Opcodes.GOTO, after);
            visitor.visitLabel(showOriginal);
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            visitor.visitLabel(after);
        }

        private static void emitDelayedOverlayChat(
                MethodVisitor visitor,
                int opcode,
                String owner,
                String name,
                String descriptor,
                boolean isInterface) {
            org.objectweb.asm.Label showOriginal = new org.objectweb.asm.Label();
            org.objectweb.asm.Label after = new org.objectweb.asm.Label();
            visitor.visitVarInsn(Opcodes.ISTORE, 4);
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            visitor.visitVarInsn(Opcodes.ASTORE, 3);
            emitShouldDelayCall(visitor);
            visitor.visitJumpInsn(Opcodes.IFEQ, showOriginal);
            visitor.visitJumpInsn(Opcodes.GOTO, after);
            visitor.visitLabel(showOriginal);
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitVarInsn(Opcodes.ILOAD, 4);
            visitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            visitor.visitLabel(after);
        }
    }
}
