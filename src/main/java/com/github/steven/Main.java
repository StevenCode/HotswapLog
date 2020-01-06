package com.github.steven;


import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;

public class Main {
    public static final String jarName = "HotswapLog-1.0-SNAPSHOT-jar-with-dependencies.jar";

    public static final Integer hotLogEnable = 1;

    public static void main(String[] args) throws Exception {
        String path = System.getProperty("user.dir");
        String jarFilePath = path + File.separator + jarName;

        final OptionParser parser = new OptionParser();
        parser.accepts("pid").withRequiredArg().ofType(int.class).required();
        parser.accepts("className").withOptionalArg().ofType(String.class);
        parser.accepts("logEnable").withRequiredArg().ofType(int.class).required();

        final OptionSet os = parser.parse(args);
        final Configure configure = new Configure();

        configure.setJavaPid((Integer) os.valueOf("pid"));
        configure.setClassName((String) os.valueOf("className"));
        configure.setLogEnable((Integer) os.valueOf("logEnable"));
        configure.setJarPath(jarFilePath);

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Class<?> vmdClass = loader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
        final Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");

        Object vmObj = null;
        try {
            // 使用 attach(String pid) 这种方式
                vmObj = vmClass.getMethod("attach", String.class).invoke(null, "" + configure.getJavaPid());

            vmClass.getMethod("loadAgent", String.class, String.class).invoke(vmObj, configure.getJarPath(),configure.getClassName()+";"+configure.getLogEnable());
        } finally {
            if (null != vmObj) {
                vmClass.getMethod("detach", (Class<?>[]) null).invoke(vmObj, (Object[]) null);
            }
        }
    }
}
