package org.cloudburstmc.server.plugin.loader.java;

import lombok.Getter;
import org.cloudburstmc.api.plugin.Dependency;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

@Getter
public class PluginAnnotationVisitor extends AnnotationVisitor {
    private final String className;
    private final PluginInformation information;
    private Type type = Type.INFORMATION;

    public PluginAnnotationVisitor(String className) {
        super(Opcodes.ASM5);
        this.className = className;
        this.information = new PluginInformation(className);
    }

    @Override
    public void visit(String name, Object value) {
        switch (type) {
            case DEPENDENCIES:
                information.getDependencies().add((Dependency) value);
                break;
            case AUTHORS:
                information.getAuthors().add((String) value);
                break;
            default:
                switch (name) {
                    case "id":
                        information.setId((String) value);
                        break;
                    case "version":
                        information.setVersion((String) value);
                        break;
                    case "description":
                        information.setDescription((String) value);
                        break;
                    case "name":
                        information.setName((String) value);
                        break;
                    case "url":
                        information.setUrl((String) value);
                }
                break;
        }

        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        switch (name) {
            case "dependencies":
                type = Type.DEPENDENCIES;
                break;
            case "authors":
                type = Type.AUTHORS;
                break;
        }
        return this;
    }

    @Override
    public void visitEnd() {
        type = Type.INFORMATION;
        super.visitEnd();
    }

    private enum Type {
        INFORMATION,
        DEPENDENCIES,
        AUTHORS
    }
}
