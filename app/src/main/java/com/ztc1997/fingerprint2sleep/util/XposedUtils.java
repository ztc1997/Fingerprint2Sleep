package com.ztc1997.fingerprint2sleep.util;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by thom on 15/10/13.
 */
public class XposedUtils {

    private XposedUtils() {

    }

    public static void disableXposed(Class<?> clazz) {
        try {
            Field field = clazz.getDeclaredField("sHookedMethodCallbacks");
            field.setAccessible(true);
            Map sHookedMethodCallbacks = (Map) field.get(null);
            Object doNothing = Class.forName("de.robv.android.xposed.XC_MethodReplacement", false, clazz.getClassLoader()).getField("DO_NOTHING").get(null);
            for (Object callbacks : sHookedMethodCallbacks.values()) {
                field = callbacks.getClass().getDeclaredField("elements");
                field.setAccessible(true);
                Object[] elements = (Object[]) field.get(callbacks);
                for (int i = 0; i < elements.length; ++i) {
                    elements[i] = doNothing;
                }
            }
        } catch (Throwable t) { // NOSONAR
            // do nothing
        }
    }

}
