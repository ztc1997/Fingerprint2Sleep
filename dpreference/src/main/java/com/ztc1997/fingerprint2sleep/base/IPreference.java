package com.ztc1997.fingerprint2sleep.base;

import java.util.Set;

public interface IPreference {
    void setPrefString(String key, String value);

    String getPrefString(String key, String defaultValue);

    void setPrefBoolean(String key, boolean value);

    boolean getPrefBoolean(String key, boolean defaultValue);

    void setPrefStringSet(String key, Set<String> value);

    Set<String> getPrefStringSet(String key, Set<String> defaultValue);
}
