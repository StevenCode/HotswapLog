package com.github.steven.core;


import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;

public class Main {
    public static final String agentName = "hotswaplog-agent-1.0-SNAPSHOT-jar-with-dependencies.jar";
    public static final String coreName = "hotswaplog-core-1.0-SNAPSHOT-jar-with-dependencies.jar";

    public static final Integer hotLogEnable = 1;

    public static void main(String[] args) throws Exception {
        String path = System.getProperty("user.dir");

		System.out.println("path"+path);

		String agentPath = path + File.separator + agentName;
        String corePath = path + File.separator + coreName;

        final OptionParser parser = new OptionParser();
        parser.accepts("pid").withRequiredArg().ofType(int.class).required();
        parser.accepts("className").withOptionalArg().ofType(String.class);
        parser.accepts("logEnable").withRequiredArg().ofType(int.class).required();

        final OptionSet os = parser.parse(args);
        final Configure configure = new Configure();

        configure.setJavaPid((Integer) os.valueOf("pid"));
        configure.setClassName((String) os.valueOf("className"));
        configure.setLogEnable((Integer) os.valueOf("logEnable"));

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");

		Object vmObj = null;
		try {
			vmObj = vmClass.getMethod("attach", String.class).invoke(null, "" + configure.getJavaPid());
			vmClass.getMethod("loadAgent", String.class, String.class).invoke(vmObj, agentPath,corePath+";"+configure.getClassName()+";"+configure.getLogEnable());
		} finally {
			if (null != vmObj) {
				vmClass.getMethod("detach", (Class<?>[]) null).invoke(vmObj, (Object[]) null);
			}
		}
    }
}
