package com.ztc1997.fingerprint2sleep.aidl;

interface IFPQAService {
    void onPrefChanged(String key);
    boolean isRunning();
}
