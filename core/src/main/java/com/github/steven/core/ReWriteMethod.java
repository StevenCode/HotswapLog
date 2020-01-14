package com.github.steven.core;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class ReWriteMethod extends AdviceAdapter {
    private final Type[] argumentTypeArray;

    protected ReWriteMethod(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
        this.argumentTypeArray = Type.getArgumentTypes(desc);
    }

    /**
     * 保存参数数组
     */
    final protected void storeArgArray() {
        System.out.println("length:"+argumentTypeArray.length);
        for (int i = 0; i < argumentTypeArray.length; i++) {
            dup();
            push(i);
            arrayLoad(Type.getType(Object.class));
            unbox(argumentTypeArray[i]);
            storeArg(i);
        }
    }
}
