package com.github.steven.agent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 间谍类<br/>
 * 藏匿在各个ClassLoader中
 */
public class Spy {


    // -- 各种Advice的钩子引用 --
    public static volatile Method ON_BEFORE_METHOD;
    public static volatile Method ON_RETURN_METHOD;


    /*
     * 用于普通的间谍初始化
     */
    public static void init(
            @Deprecated
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod) {
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;

    }

    /*
     * 用于启动线程初始化
     */
    public static void initForAgentLauncher(
            @Deprecated
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod) {
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
    }


    public static void printTest() {
        if (ON_BEFORE_METHOD != null) {
            System.out.println("beforeMethod is not null");
        }
        if (ON_RETURN_METHOD != null) {
            try {
                ON_RETURN_METHOD.invoke(null, "testReturn");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

}
