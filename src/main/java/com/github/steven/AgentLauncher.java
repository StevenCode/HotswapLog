package com.github.steven;

import com.github.steven.core.Enhancer;
import com.github.steven.core.ResetEnhancer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

/**
 * TestAgent.
 *
 * @author shidingfeng
 */
public class AgentLauncher {

	public static void agentmain(String args, Instrumentation inst) throws IOException {

		String[] splitArgs = args.split(";");

		String className =splitArgs[0];
		String action = splitArgs[1];

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
		System.out.println("transformSet.size:"+transformSet.size());
		ClassFileTransformer enhancer;
		if (StringUtils.equals(action, Main.hotLogEnable.toString())) {
			enhancer = new Enhancer(transformSet);
		}else {
			enhancer = new ResetEnhancer();
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
		}
		finally {
			System.out.println("detach Load Done.");
			inst.removeTransformer(enhancer);
		}

	}
}
