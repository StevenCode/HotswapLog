package com.github.steven.core;


import com.github.steven.core.http.WrapperRequest;
import com.github.steven.core.http.WrapperResponseCopier;
import com.github.steven.core.http.WrapperTransModel;
import com.github.steven.core.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;


/**
 * AdviceWeaver.
 *
 * @author shidingfeng
 */
public class AdviceWeaver extends ClassVisitor implements Opcodes {

    private final String javaClassName;
    private final static Logger logger = LogUtil.getLogger();
    private static final ThreadLocal<WrapperTransModel> wtmRef = new ThreadLocal<WrapperTransModel>();
    private static Method asmBeforeMethod = null;

    static {
        try {
            Class clazz = AdviceWeaver.class.getClassLoader().loadClass("com.github.steven.agent.Spy");
            asmBeforeMethod = Method.getMethod(clazz.getDeclaredMethod("spyMethodOnBefore", Object[].class, String.class, String.class, String.class,
                    Object.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (asmBeforeMethod != null) {
            System.out.println("not null");
        }
    }


    public static void methodOnEnd(
            Object returnObject) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        if (returnObject != null) {
            logger.info("return ----------");
            logger.info(returnObject.toString());


        }
        WrapperTransModel wtm = wtmRef.get();

        logger.info("URL:");
        logger.info(wtm.getRequestURL());

        logger.info("Header:");
        Map<String, String> headers = wtm.getHeaders();
        for (Map.Entry<String, String> map : headers.entrySet()) {
            logger.info(map.getKey() + " " + map.getValue());
        }

        logger.info("Body:");
        logger.info(wtm.getBody());

        logger.info("Response:");
        wtm.setResponse(new String(wtm.copier.getResponseData(), wtm.copier.getCharacterEncoding()));
        logger.info(wtm.getResponse());

    }

    public static void methodOnBegin(
             String className, String methodName, String methodDesc,
            Object target,Object[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, NoSuchFieldException {


        Object request = args[0];
        Object response = args[1];
        if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        WrapperResponseCopier wrapperRes = new WrapperResponseCopier(resp);
        WrapperRequest wrapperReq;
        try {
            wrapperReq = new WrapperRequest(req, wrapperRes);
        } catch (IOException e) {
            return;
        }

        WrapperTransModel wtm = WrapperTransModel.build(wrapperReq);
        wtm.setBody(wrapperReq.getBody());
        wtm.copier = wrapperRes;
        wtm.request = wrapperReq;

        wtmRef.set(wtm);

        args[0] = wrapperReq;
        args[1] = wrapperRes;
    }


    AdviceWeaver(final String internalClassName, int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.javaClassName = tranClassName(internalClassName);
    }


    /**
     * 翻译类名称<br/>
     * 将 java/lang/String 的名称翻译成 java.lang.String
     *
     * @param className 类名称 java/lang/String
     * @return 翻译后名称 java.lang.String
     */
    public static String tranClassName(String className) {
        return StringUtils.replace(className, "/", ".");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }


    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (!StringUtils.equals(name, "service")) {
            return mv;
        }
        return new ReWriteMethod(ASM5, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {

            private final Type ASM_TYPE_SPY = Type.getType("Lcom/github/steven/agent/Spy;");
            private final Type ASM_TYPE_OBJECT = Type.getType(Object.class);
            private final Type ASM_TYPE_OBJECT_ARRAY = Type.getType(Object[].class);
            private final Type ASM_TYPE_CLASS = Type.getType(Class.class);
            private final Type ASM_TYPE_INTEGER = Type.getType(Integer.class);
            private final Type ASM_TYPE_CLASS_LOADER = Type.getType(ClassLoader.class);
            private final Type ASM_TYPE_STRING = Type.getType(String.class);
            private final Type ASM_TYPE_THROWABLE = Type.getType(Throwable.class);
            private final Type ASM_TYPE_INT = Type.getType(int.class);
            private final Type ASM_TYPE_METHOD = Type.getType(java.lang.reflect.Method.class);
            private final Method ASM_METHOD_METHOD_INVOKE = Method.getMethod("Object invoke(Object,Object[])");


            @Override
            protected void onMethodEnter() {
                final StringBuilder append = new StringBuilder();
                _debug(append, "debug:onMethodEnter()-----------------------");
                loadArgArray();
                dup();
                dup();
                push(tranClassName(javaClassName));
                push(name);
                push(desc);
                pushNull();
                invokeStatic(ASM_TYPE_SPY, asmBeforeMethod);
                swap();
                storeArgArray();
                pop();

                _debug(append, "invokeVirtual()");
            }

            @Override
            protected void onMethodExit(final int opcode) {

                final StringBuilder append = new StringBuilder();
                _debug(append, "debug:onMethodExit()");

                // 加载返回对象
                loadReturn(opcode);
                _debug(append, "loadReturn()");

                // 加载before方法
                getStatic(ASM_TYPE_SPY, "ON_RETURN_METHOD", ASM_TYPE_METHOD);
                _debug(append, "loadAdviceMethod()");

                // 推入Method.invoke()的第一个参数
                pushNull();

                // 加载return通知参数数组
                loadReturnArgs();
                _debug(append, "loadReturnArgs()");

                invokeVirtual(ASM_TYPE_METHOD, ASM_METHOD_METHOD_INVOKE);
                pop();
                _debug(append, "invokeVirtual()");

            }

            /**
             * 加载ClassLoader<br/>
             * 这里分开静态方法中ClassLoader的获取以及普通方法中ClassLoader的获取
             * 主要是性能上的考虑
             */
            private void loadClassLoader() {

                if (this.isStaticMethod()) {

//                    // fast enhance
//                    if (GlobalOptions.isEnableFastEnhance) {
//                        visitLdcInsn(Type.getType(String.format("L%s;", internalClassName)));
//                        visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
//                    }

                    // normal enhance
//                    else {

                    // 这里不得不用性能极差的Class.forName()来完成类的获取,因为有可能当前这个静态方法在执行的时候
                    // 当前类并没有完成实例化,会引起JVM对class文件的合法性校验失败
                    // 未来我可能会在这一块考虑性能优化,但对于当前而言,功能远远重要于性能,也就不打算折腾这么复杂了
                    visitLdcInsn(javaClassName);
                    invokeStatic(ASM_TYPE_CLASS, Method.getMethod("Class forName(String)"));
                    invokeVirtual(ASM_TYPE_CLASS, Method.getMethod("ClassLoader getClassLoader()"));
//                    }

                } else {
                    loadThis();
                    invokeVirtual(ASM_TYPE_OBJECT, Method.getMethod("Class getClass()"));
                    invokeVirtual(ASM_TYPE_CLASS, Method.getMethod("ClassLoader getClassLoader()"));
                }

            }


            /*
             * 加载return通知参数数组
             */
            private void loadReturnArgs() {
                dup2X1();
                pop2();
                push(1);
                newArray(ASM_TYPE_OBJECT);
                dup();
                dup2X1();
                pop2();
                push(0);
                swap();
                arrayStore(ASM_TYPE_OBJECT);

            }

            /**
             * 加载返回值
             * @param opcode 操作吗
             */
            private void loadReturn(int opcode) {
                switch (opcode) {

                    case RETURN: {
                        pushNull();
                        break;
                    }

                    case ARETURN: {
                        dup();
                        break;
                    }

                    case LRETURN:
                    case DRETURN: {
                        dup2();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                    default: {
                        dup();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                }
            }

            /**
             * 是否静态方法
             * @return true:静态方法 / false:非静态方法
             */
            private boolean isStaticMethod() {
                return (methodAccess & ACC_STATIC) != 0;
            }

            /**
             * 加载before通知参数数组
             */
            private void loadArrayForBefore() {
                push(6);
                newArray(ASM_TYPE_OBJECT);

                dup();
                push(0);
                loadClassLoader();
                arrayStore(ASM_TYPE_CLASS_LOADER);

                dup();
                push(1);
                push(tranClassName(javaClassName));
                arrayStore(ASM_TYPE_STRING);

                dup();
                push(2);
                push(name);
                arrayStore(ASM_TYPE_STRING);

                dup();
                push(3);
                push(desc);
                arrayStore(ASM_TYPE_STRING);

                dup();
                push(4);
                loadThisOrPushNullIfIsStatic();
                arrayStore(ASM_TYPE_OBJECT);

                dup();
                push(5);
                loadArgArray();
                arrayStore(ASM_TYPE_OBJECT_ARRAY);
            }

            /**
             * 加载this/null
             */
            private void loadThisOrPushNullIfIsStatic() {
                if (isStaticMethod()) {
                    pushNull();
                } else {
                    loadThis();
                }
            }

            /**
             * 将NULL推入堆栈
             */
            private void pushNull() {
                push((Type) null);
            }

            private void _debug(final StringBuilder append, final String msg) {

                // println msg
                visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                if (StringUtils.isBlank(append.toString())) {
                    visitLdcInsn(append.append(msg).toString());
                } else {
                    visitLdcInsn(append.append(" >> ").append(msg).toString());
                }

                visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }

        };
    }


}
