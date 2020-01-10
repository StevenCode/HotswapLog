package com.github.steven.core;


import com.github.steven.core.util.LogUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.Set;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeStaticMethod;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static com.github.steven.core.util.ReflectUtils.defineClass;

/**
 * TestTransformer.
 *
 * @author shidingfeng
 */
public class Enhancer implements ClassFileTransformer {
	private final static Logger logger = LogUtil.getLogger();

	private final Set<Class<?>> enhanceMap;
	public static final String SPY_CLASSNAME = "com.github.steven.agent.Spy";

	public Enhancer(Set<Class<?>> enhanceMap) {
		this.enhanceMap = enhanceMap;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			// 过滤掉不在增强集合范围内的类
			if (!enhanceMap.contains(classBeingRedefined)) {
				return null;
			}
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter classWriter = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | COMPUTE_MAXS) {
				@Override
				protected String getCommonSuperClass(String type1, String type2) {
					Class<?> c, d;
					try {
						c = Class.forName(type1.replace('/', '.'), false, loader);
						d = Class.forName(type2.replace('/', '.'), false, loader);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					if (c.isAssignableFrom(d)) {
						return type1;
					}
					if (d.isAssignableFrom(c)) {
						return type2;
					}
					if (c.isInterface() || d.isInterface()) {
						return "java/lang/Object";
					} else {
						do {
							c = c.getSuperclass();
						} while (!c.isAssignableFrom(d));
						return c.getName().replace('.', '/');
					}
				}

			};
			ClassVisitor classVisitor = new AdviceWeaver(reader.getClassName(), Opcodes.ASM5, classWriter);
			reader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
			// 排遣间谍
			try {
				spy(loader);
			} catch (Throwable t) {
				logger.error("print spy failed. classname={};loader={};" ,className ,loader , t);
				throw t;
			}

			return classWriter.toByteArray();
		} catch (Throwable t) {

			logger.error("transform loader[{}]:class[{}] failed.", loader,className,t);
		}
		return null;
	}


	/*
	 * 派遣间谍混入对方的classLoader中
	 */
	private void spy(final ClassLoader targetClassLoader)
			throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		// 如果对方是bootstrap就算了
		if (null == targetClassLoader) {
			return;
		}


		// Enhancer类只可能从agentClassLoader中加载
		// 所以找他要ClassLoader是靠谱的
		final ClassLoader agentClassloader = Enhancer.class.getClassLoader();

		final String spyClassName = SPY_CLASSNAME;

		// 从AgentClassLoader中加载Spy
		final Class<?> spyClassFromAgentClassLoader = loadSpyClassFromagentClassLoader(agentClassloader, spyClassName);
		if (null == spyClassFromAgentClassLoader) {
			return;
		}

		// 从目标ClassLoader中尝试加载或定义ClassLoader
		Class<?> spyClassFromTargetClassLoader = null;
		try {

			// 去目标类加载器中找下是否已经存在间谍
			// 如果间谍已经存在就算了
			spyClassFromTargetClassLoader = targetClassLoader.loadClass(spyClassName);
			logger.info("Spy already in targetClassLoader : ", targetClassLoader);

		}

		// 看来间谍不存在啊
		catch (ClassNotFoundException cnfe) {

			try {// 在目标类加载器中混入间谍
				spyClassFromTargetClassLoader = defineClass(
						targetClassLoader,
						spyClassName,
						toByteArray(Enhancer.class.getResourceAsStream("/" + spyClassName.replace('.', '/') + ".class"))
				);
			} catch (InvocationTargetException ite) {
				if (ite.getCause() instanceof java.lang.LinkageError) {
					// CloudEngine 由于 loadClass 不到,会导致 java.lang.LinkageError: loader (instance of  com/alipay/cloudengine/extensions/equinox/KernelAceClassLoader): attempted  duplicate class definition for name: "com/taobao/arthas/core/advisor/Spy"
					// 这里尝试忽略
					logger.error("resolve #112 issues" , ite);
				} else {
					throw ite;
				}
			}

		}


		// 无论从哪里取到spyClass，都需要重新初始化一次
		// 用以兼容重新加载的场景
		// 当然，这样做会给渲染的过程带来一定的性能开销，不过能简化编码复杂度
		finally {

			if (null != spyClassFromTargetClassLoader) {
				// 初始化间谍
				invokeStaticMethod(
						spyClassFromTargetClassLoader,
						"init",
						agentClassloader,
						getField(spyClassFromAgentClassLoader, "ON_BEFORE_METHOD").get(null),
						getField(spyClassFromAgentClassLoader, "ON_RETURN_METHOD").get(null)
				);
			}

		}

	}

	private Class<?> loadSpyClassFromagentClassLoader(final ClassLoader agentClassLoader, final String spyClassName) {
		try {
			return agentClassLoader.loadClass(spyClassName);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
