package com.github.steven.agent;


import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * TestAgent.
 *
 * @author shidingfeng
 */
public class AgentLauncher {


	public static void agentmain(String args, Instrumentation inst) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

//		 将Spy添加到BootstrapClassLoader
		inst.appendToBootstrapClassLoaderSearch(
				new JarFile(AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
		);

		String[] splitArgs = args.split(";");

		String agentJar = splitArgs[0];
		String className = splitArgs[1];
		String action = splitArgs[2];

		final ClassLoader agentLoader = new AgentClassLoader(agentJar);
		final Class<?> adviceWeaverClass = agentLoader.loadClass("com.github.steven.core.AdviceWeaver");

		Spy.initForAgentLauncher(agentLoader,
				adviceWeaverClass.getMethod("methodOnBegin",
						String.class,
						String.class,
						String.class,
						Object.class,
						Object[].class),
				adviceWeaverClass.getMethod("methodOnEnd",
						Object.class));

		Set<Class<?>> transformSet = new HashSet<>();

		Class[] allLoadedClasses = inst.getAllLoadedClasses();
		for (Class<?> clazz : allLoadedClasses) {
			if (clazz == null) {
				continue;
			}

			if (clazz.getName().equals(className)) {
				transformSet.add(clazz);
			}
		}
		System.out.println("transformSet.size:" + transformSet.size());
		ClassFileTransformer enhancer;
		if (StringUtils.equals(action, "1")) {
//			enhancer = new Enhancer(transformSet);
			// Enhancer
			final Class<?> enhancerClass = agentLoader.loadClass("com.github.steven.core.Enhancer");
			enhancer = (ClassFileTransformer) enhancerClass.getConstructor(Set.class).newInstance(transformSet);
		} else {
			enhancer = (ClassFileTransformer) agentLoader.loadClass("com.github.steven.core.ResetEnhancer").getConstructor().newInstance();
		}

		inst.addTransformer(enhancer, true);
		try {
			for (Class<?> clazz : transformSet) {
				inst.retransformClasses(clazz);
			}
			System.out.println("Agent Load Done.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("agent load failed!");
		} finally {
			System.out.println("detach Load Done.");
			inst.removeTransformer(enhancer);
		}

	}
}
