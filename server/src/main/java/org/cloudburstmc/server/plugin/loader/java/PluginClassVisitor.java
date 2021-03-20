package org.cloudburstmc.server.plugin.loader.java;

import lombok.Getter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

public class PluginClassVisitor extends ClassVisitor {
    private static final String PLUGIN_DESCRIPTOR = "Lorg/cloudburstmc/api/plugin/Plugin;";

    @Getter
    private String className;
    private PluginAnnotationVisitor annotationVisitor;

    public PluginClassVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String className, String signature, String superName, String[] interfaces) {
        this.className = className;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String description, boolean visible) {
        if (visible && PLUGIN_DESCRIPTOR.equals(description)) {
            return this.annotationVisitor = new PluginAnnotationVisitor(className);
        }

        return null;
    }

    public Optional<PluginInformation> getInformation() {
        return annotationVisitor == null ? Optional.empty() : Optional.of(annotationVisitor.getInformation());
    }
}
