package org.cloudburstmc.server.registry.behavior.proxy;

import org.cloudburstmc.api.util.behavior.Behavior;
import org.objectweb.asm.*;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.objectweb.asm.Opcodes.*;

/**
 * Utility class that generates a proxy factory in between a behavior's executor and implementation interface.
 * <p>
 * This removes the need to manually write a proxy factory for each behavior, making it easier
 * for plugin developers to use the API.
 */
public class BehaviorProxies {
    private static final String PROXY_CLASS_PATH = "org/cloudburstmc/server/registry/behavior/proxy/";
    private static final Type TYPE_BI_FUNCTION = Type.getType(BiFunction.class);
    private static final Type TYPE_OBJECT = Type.getType(Object.class);
    private static final Type TYPE_CONTEXT = Type.getType(Behavior.class);
    private static final Type TYPE_LOOKUP = Type.getType(MethodHandles.Lookup.class);
    private static final Type TYPE_METHOD_HANDLES = Type.getType(MethodHandles.class);
    private static final Type TYPE_LAMBDA_METAFACTORY = Type.getType(LambdaMetafactory.class);
    private static final org.objectweb.asm.commons.Method METHOD_METAFACTORY;

    private static final AtomicLong ID_ALLOCATOR = new AtomicLong();

    static {
        try {
            Method method = LambdaMetafactory.class.getMethod("metafactory",
                    MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class);
            METHOD_METAFACTORY = org.objectweb.asm.commons.Method.getMethod(method);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Unable to get LambdaMetafactory.metafactory method", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <F, E> BiFunction<Behavior<E>, F, E> createExecutorProxy(Class<F> behaviorClass, Class<E> executorClass)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        validateProxyClasses(behaviorClass, executorClass);

        Method behaviorMethod = behaviorClass.getDeclaredMethods()[0];
        Method executorMethod = executorClass.getDeclaredMethods()[0];

        org.objectweb.asm.commons.Method behaviorAsmMethod = org.objectweb.asm.commons.Method.getMethod(behaviorMethod);
        org.objectweb.asm.commons.Method executorAsmMethod = org.objectweb.asm.commons.Method.getMethod(executorMethod);

        Type behaviorType = Type.getType(behaviorClass);
        Type executorType = Type.getType(executorClass);

        // Create our class name and type
        String proxyName = behaviorClass.getSimpleName() + "Proxy" + ID_ALLOCATOR.getAndIncrement();
        Type proxyType = Type.getType("L" + PROXY_CLASS_PATH + proxyName + ";");

        // Generate lambda method
        Type[] behaviorArgTypes = behaviorAsmMethod.getArgumentTypes();
        Type[] lambdaArgTypes = new Type[behaviorArgTypes.length + 1];
        System.arraycopy(behaviorArgTypes, 0, lambdaArgTypes, 1, behaviorArgTypes.length);
        lambdaArgTypes[0] = behaviorType;
        org.objectweb.asm.commons.Method lambdaMethod = new org.objectweb.asm.commons.Method("lambda$apply$0", executorAsmMethod.getReturnType(), lambdaArgTypes);

        // Create interface signature
        Type genericContextType = createGeneric(TYPE_CONTEXT, executorType);
        Type classSignature = Type.getType(
                TYPE_OBJECT.getDescriptor() + createGeneric(
                        TYPE_BI_FUNCTION,
                        genericContextType,
                        behaviorType,
                        executorType
                )
        );

        // This was mostly generated with ASMifier, then modifications were made
        // so it could dynamically generate the proxy class.
        // There's probably an easier way to do this, but it's my first attempt.

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, proxyType.getInternalName(), classSignature.getDescriptor(),
                TYPE_OBJECT.getInternalName(), new String[]{TYPE_BI_FUNCTION.getInternalName()});
        cw.visitSource(proxyName + ".java", null);
        cw.visitInnerClass(executorType.getInternalName(), behaviorType.getInternalName(), "Executor",
                ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);
        cw.visitInnerClass(TYPE_LOOKUP.getInternalName(), TYPE_METHOD_HANDLES.getInternalName(), "Lookup",
                ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(9, label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, TYPE_OBJECT.getInternalName(), "<init>", "()V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLineNumber(10, label1);
            mv.visitInsn(RETURN);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLocalVariable("this", proxyType.getDescriptor(), null, label0, label2, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        Type biFunctionType = Type.getMethodType(executorType, TYPE_CONTEXT, behaviorType);
        {
            Type biFunctionSignature = Type.getMethodType(executorType, genericContextType, behaviorType);

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", biFunctionType.getDescriptor(),
                    biFunctionSignature.getDescriptor(), null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(13, label0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInvokeDynamicInsn(executorAsmMethod.getName(), Type.getMethodType(executorType, behaviorType, TYPE_CONTEXT).getDescriptor(),
                    new Handle(
                            H_INVOKESTATIC,
                            TYPE_LAMBDA_METAFACTORY.getInternalName(),
                            METHOD_METAFACTORY.getName(),
                            METHOD_METAFACTORY.getDescriptor(),
                            false
                    ),
                    Type.getType(executorMethod),
                    new Handle(
                            H_INVOKESTATIC,
                            proxyType.getInternalName(),
                            lambdaMethod.getName(),
                            lambdaMethod.getDescriptor(),
                            false
                    ),
                    Type.getType(executorMethod)
            );
            mv.visitInsn(ARETURN);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLocalVariable("this", proxyType.getDescriptor(), null, label0, label1, 0);
            mv.visitLocalVariable("var1", TYPE_CONTEXT.getDescriptor(), genericContextType.getDescriptor(), label0, label1, 1);
            mv.visitLocalVariable("var0", behaviorType.getDescriptor(), null, label0, label1, 2);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "apply",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(14, label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, TYPE_CONTEXT.getInternalName());
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, behaviorType.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL, proxyType.getInternalName(), "apply", biFunctionType.getDescriptor(), false);
            mv.visitInsn(ARETURN);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLocalVariable("this", proxyType.getDescriptor(), null, label0, label1, 0);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, lambdaMethod.getName(),
                    lambdaMethod.getDescriptor(), null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(11, label0);
            // Load the lambda parameters
            for (int i = 0; i < lambdaArgTypes.length; i++) {
                mv.visitVarInsn(lambdaArgTypes[i].getOpcode(ILOAD), i);
            }
            mv.visitMethodInsn(INVOKEINTERFACE, behaviorType.getInternalName(), behaviorAsmMethod.getName(),
                    behaviorAsmMethod.getDescriptor(), true);
            mv.visitInsn(behaviorAsmMethod.getReturnType().getOpcode(IRETURN));
            Label label1 = new Label();
            mv.visitLabel(label1);
            // Set the lambda parameter variable names
            for (int i = 0; i < lambdaArgTypes.length; i++) {
                mv.visitLocalVariable("var" + i, lambdaArgTypes[i].getDescriptor(), null, label0, label1, i);
            }
            mv.visitMaxs(lambdaArgTypes.length, lambdaArgTypes.length);
            mv.visitEnd();
        }
        cw.visitEnd();

        Class<?> clazz = MethodHandles.lookup().defineClass(cw.toByteArray());
        return (BiFunction<Behavior<E>, F, E>) clazz.getConstructor().newInstance();
    }

    private static void validateProxyClasses(Class<?> behaviorClass, Class<?> executorClass) {
        String behaviorClassName = behaviorClass.getSimpleName();
        // Sanitise input classes
        checkArgument(behaviorClass.isInterface(), "Behavior type must be an interface");
        checkArgument(executorClass.isInterface(), "Executor type must be an interface");
        checkArgument(behaviorClass.getInterfaces().length == 0,
                "%s must implement no interfaces", behaviorClassName);
        checkArgument(behaviorClass.getDeclaredMethods().length == 1,
                "There should only be one method declared in the behavior interface");
        checkArgument(executorClass.getDeclaredMethods().length == 1,
                "There should only be one method declared in the executor interface");
        checkArgument(Arrays.stream(behaviorClass.getClasses()).anyMatch(aClass -> aClass == executorClass),
                "%s must be an inner class of the %s", executorClass.getName(), behaviorClassName);

        // Sanitise methods
        Method behaviorMethod = behaviorClass.getDeclaredMethods()[0];
        Method executorMethod = executorClass.getDeclaredMethods()[0];
        checkArgument(behaviorMethod.getReturnType().equals(executorMethod.getReturnType()), "Behavior and executor do not share the same return type");

        Class<?>[] behaviorParams = behaviorMethod.getParameterTypes();
        Class<?>[] executorParams = executorMethod.getParameterTypes();
        Class<?>[] expectedParams = Arrays.copyOfRange(behaviorParams, 1, behaviorParams.length);
        checkArgument(Arrays.equals(expectedParams, executorParams), "The executor and behavior do not share the correct parameters");
    }

    private static Type createGeneric(Type generic, Type... params) {
        return Type.getType("L" + generic.getInternalName() + "<" + Arrays.stream(params)
                .map(Type::getDescriptor)
                .collect(Collectors.joining(""))
                + ">;");
    }
}
