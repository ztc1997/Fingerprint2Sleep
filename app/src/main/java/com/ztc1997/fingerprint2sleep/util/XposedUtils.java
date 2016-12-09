package com.ztc1997.fingerprint2sleep.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import rx.functions.Func1;

/**
 * Created by thom on 15/10/13.
 */
public class XposedUtils {

    private XposedUtils() {

    }

    public static void disableXposedModules(Func1<String, Boolean> filter) {
        try {
            Class<?> clazz = Class.forName("de.robv.android.xposed.XposedBridge", false, ClassLoader.getSystemClassLoader());
            Field field = clazz.getDeclaredField("sHookedMethodCallbacks");
            field.setAccessible(true);
            Map sHookedMethodCallbacks = (Map) field.get(null);
            for (Object callbacks : sHookedMethodCallbacks.values()) {
                field = callbacks.getClass().getDeclaredField("elements");
                field.setAccessible(true);
                Object[] elements = (Object[]) field.get(callbacks);
                HashSet<?> newElements = new HashSet<>(Arrays.asList(elements));
                for (Object element : elements) {
                    if (filter.call(element.getClass().getName())) newElements.remove(element);
                }
                field.set(callbacks, newElements.toArray());
            }
        } catch (Throwable ignored) {
        }
    }

}
