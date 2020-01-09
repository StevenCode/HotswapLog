package com.github.steven.core;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * TestTransformer.
 *
 * @author shidingfeng
 */
public class Enhancer implements ClassFileTransformer {
	private final Set<Class<?>> enhanceMap;

	public Enhancer(Set<Class<?>> enhanceMap) {
		this.enhanceMap = enhanceMap;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		// 过滤掉不在增强集合范围内的类
		if (!enhanceMap.contains(classBeingRedefined)) {
			return null;
		}
		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter classWriter = new ClassWriter(reader,ClassWriter.COMPUTE_FRAMES | COMPUTE_MAXS){
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
		return classWriter.toByteArray();
	}


}
