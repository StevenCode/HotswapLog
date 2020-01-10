package com.github.steven.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ReflectUtils.
 *
 * @author shidingfeng
 */
public class ReflectUtils {
	/**
	 * 定义类
	 *
	 * @param targetClassLoader 目标classLoader
	 * @param className         类名称
	 * @param classByteArray    类字节码数组
	 * @return 定义的类
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public static Class<?> defineClass(
			final ClassLoader targetClassLoader,
			final String className,
			final byte[] classByteArray) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		final Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
				"defineClass",
				String.class,
				byte[].class,
				int.class,
				int.class
		);

		synchronized (defineClassMethod) {
			final boolean acc = defineClassMethod.isAccessible();
			try {
				defineClassMethod.setAccessible(true);
				return (Class<?>) defineClassMethod.invoke(
						targetClassLoader,
						className,
						classByteArray,
						0,
						classByteArray.length
				);
			} finally {
				defineClassMethod.setAccessible(acc);
			}
		}

	}
}
