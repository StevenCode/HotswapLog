package com.github.steven;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {

        VirtualMachine vm = VirtualMachine.attach("15652"); // 目标 JVM pid
        vm.loadAgent("D:\\IDEA\\GitHub\\HotswapLog\\target\\HotswapLog-1.0-SNAPSHOT-jar-with-dependencies.jar");
        vm.detach();
    }

}
