import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class PatchModdedWarning {
    private static final String TARGET_METHOD = "showModdedAppWarning";
    private static final String TARGET_DESCRIPTOR =
            "(Lorg/fossify/commons/activities/BaseSimpleActivity;)V";

    public static void main(String[] args) throws Exception {
        String inputPath = args[0];
        String outputPath = args[1];

        byte[] original;
        try (FileInputStream in = new FileInputStream(inputPath)) {
            original = in.readAllBytes();
        }

        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                              String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals(TARGET_METHOD) && descriptor.equals(TARGET_DESCRIPTOR)) {
                    System.out.println("Patching " + name + descriptor + " to a no-op");
                    mv.visitCode();
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return new MethodVisitor(Opcodes.ASM9) {
                    };
                }
                return mv;
            }
        };

        reader.accept(visitor, 0);
        byte[] patched = writer.toByteArray();

        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            out.write(patched);
        }

        System.out.println("Wrote patched class to " + outputPath);
    }
}
