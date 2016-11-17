package com.ztc1997.fingerprint2sleep.util;

import com.ztc1997.fingerprint2sleep.BuildConfig;

public class XposedProbe {
    public static int activatedModuleVersion() {
        return -1;
    }

    public static boolean isModuleActivated() {
        return activatedModuleVersion() > 0;
    }

    public static boolean isModuleVersionMatched() {
        return activatedModuleVersion() == BuildConfig.VERSION_CODE;
    }
}
