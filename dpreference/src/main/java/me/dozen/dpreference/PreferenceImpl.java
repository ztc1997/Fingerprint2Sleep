package me.dozen.dpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Set;

/**
 * Created by wangyida on 15/12/18.
 */
@SuppressLint("WorldReadableFiles")
class PreferenceImpl implements IPrefImpl {

    private static final int MODE = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ?
            Context.MODE_WORLD_READABLE : Context.MODE_PRIVATE;

    private Context mContext;

    private String mPrefName;

    public PreferenceImpl(Context context, String prefName) {
        mContext = context;
        mPrefName = prefName;
    }

    public String getPrefString(final String key,
                                final String defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getString(key, defaultValue);
    }

    public void setPrefString(final String key, final String value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putString(key, value).apply();
    }

    public boolean getPrefBoolean(final String key,
                                  final boolean defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getBoolean(key, defaultValue);
    }

    public boolean hasKey(final String key) {
        return mContext.getSharedPreferences(mPrefName, MODE)
                .contains(key);
    }

    public void setPrefBoolean(final String key, final boolean value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putBoolean(key, value).apply();
    }

    public void setPrefInt(final String key, final int value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putInt(key, value).apply();
    }

    @Override
    public Set<String> getPrefStringSet(String key, Set<String> defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getStringSet(key, defaultValue);
    }

    @Override
    public void setPrefStringSet(String key, Set<String> value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putStringSet(key, value).apply();
    }

    public void increasePrefInt(final String key) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        increasePrefInt(settings, key);
    }

    public void increasePrefInt(final SharedPreferences sp, final String key) {
        final int v = sp.getInt(key, 0) + 1;
        sp.edit().putInt(key, v).apply();
    }

    public void increasePrefInt(final SharedPreferences sp, final String key,
                                final int increment) {
        final int v = sp.getInt(key, 0) + increment;
        sp.edit().putInt(key, v).apply();
    }

    public int getPrefInt(final String key, final int defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getInt(key, defaultValue);
    }

    public void setPrefFloat(final String key, final float value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putFloat(key, value).apply();
    }

    public float getPrefFloat(final String key, final float defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getFloat(key, defaultValue);
    }

    public void setPrefLong(final String key, final long value) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        settings.edit().putLong(key, value).apply();
    }

    public long getPrefLong(final String key, final long defaultValue) {
        final SharedPreferences settings =
                mContext.getSharedPreferences(mPrefName, MODE);
        return settings.getLong(key, defaultValue);
    }


    public void removePreference(final String key) {
        final SharedPreferences prefs =
                mContext.getSharedPreferences(mPrefName, MODE);
        prefs.edit().remove(key).apply();
    }

    public void clearPreference(final SharedPreferences p) {
        final SharedPreferences.Editor editor = p.edit();
        editor.clear();
        editor.apply();
    }

}
