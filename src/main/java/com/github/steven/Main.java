package com.github.steven;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.IOException;

public class Main {
    public static final String jarName = "HotswapLog-1.0-SNAPSHOT-jar-with-dependencies.jar";

    public static final Integer hotLogEnable = 1;

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
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

        VirtualMachine vm = VirtualMachine.attach(String.valueOf(configure.getJavaPid())); // 目标 JVM pid
        vm.loadAgent(configure.getJarPath(),configure.getClassName()+";"+configure.getLogEnable());
        vm.detach();
    }
}
