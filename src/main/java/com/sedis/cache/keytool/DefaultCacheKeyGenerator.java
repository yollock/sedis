package com.sedis.cache.keytool;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * Created by yollock on 2016/9/25.
 */
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {

    private static Logger logger = Logger.getLogger(DefaultCacheKeyGenerator.class);

    @Override
    public String generateKey(Method method, Object[] args, Object target, String key) {
        if (key != null) {
            return argsMatchKeyGenerate(args, key);
        } else {
            return classMethodArgsKeyGenerate(method, args, target);
        }
    }

    private String classMethodArgsKeyGenerate(Method interfaceMethod, Object[] args, Object target) {
        String key = null;
        Method implMethod = null;
        //使用具体的实现类作为key的前缀，而不使用接口最为前缀，因为接口有多个实现
        if (!Proxy.isProxyClass(target.getClass())) {
            try {
                implMethod = target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {
                logger.error("DefaultCacheKeyGeneratorError, interfaceMethod is " + interfaceMethod == null ? "null" : interfaceMethod.getName(), e);
            }
        }
        Method methodForKey = (implMethod != null ? implMethod : interfaceMethod);
        key = methodForKey.getDeclaringClass().getName() + "." + methodForKey.getName();
        if (args != null) {
            key += Arrays.toString(args);
        }
        return key;
    }

    private String argsMatchKeyGenerate(Object[] args, String key) {
        String result = key;
        if (result != null && result.indexOf("@args") > -1) {
            for (int i = 0; i < args.length; i++) {
                String placeholder = "@args" + i;
                String value = "@" + String.valueOf(args[i]);
                while (result.contains(placeholder)) {
                    result = result.replace(placeholder, value);
                }
            }
        }
        return result;
    }

}
