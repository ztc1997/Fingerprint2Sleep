/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.marshmallow.gravitybox.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.DrawableRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.ceco.marshmallow.gravitybox.Utils;
import com.ceco.marshmallow.gravitybox.adapters.BasicIconListItem;
import com.ceco.marshmallow.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.marshmallow.gravitybox.adapters.IconListAdapter;
import com.ztc1997.fingerprint2sleep.R;
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AppPickerPreference extends DialogPreference
        implements OnItemClickListener,
        OnItemSelectedListener,
        View.OnClickListener,
        View.OnLongClickListener {
    private static final String TAG = "GB:AppPickerPreference";
    public static final String SEPARATOR = "#C3C0#";
    public static final String PREF_KEY_FINGERPRINT_LAUNCHER_FINGER = "pref_fingerprint_launcher_finger";
    public static final String EXTRA_ALLOW_UNLOCK_ACTION = "allowUnlockAction";
    public static final String EXTRA_LAUNCHES_FROM_LOCKSCREEN = "launchesFromLockscreen";
    public static final String ACTION_LAUNCH_ACTION = "gravitybox.intent.action.LAUNCH_ACTION";

    public static final int MODE_APP = 0;
    public static final int MODE_SHORTCUT = 1;

    public static SettingsActivity.SettingsFragment settingsFragment;
    private static IconListAdapter sIconPickerAdapter;

    private Context mContext;
    private ListView mListView;
    private EditText mSearch;
    private ProgressBar mProgressBar;
    private AsyncTask<Void, Void, ArrayList<IIconListAdapterItem>> mAsyncTask;
    private String mDefaultSummaryText;
    private int mAppIconSizePx;
    private PackageManager mPackageManager;
    private Resources mResources;
    private int mMode;
    private Spinner mModeSpinner;
    private ImageButton mBtnAppIcon;
    private AppInfo mAppInfo;
    private int mAppIconPreviewSizePx;
    private Dialog mIconPickerDialog;
    private boolean mIconPickerEnabled = true;
    private int mIconPickSizePx;
    private boolean mNullItemEnabled = true;
    private String mValue;
    private boolean mAllowUnlockAction;
    private boolean mLaunchesFromLockscreen;
    private boolean mForceCustomIcon;
    private boolean mAllowGravityBoxActions;
    private Bundle mExtraData;

    private static LruCache<String, BitmapDrawable> sAppIconCache;

    static {
        final int cacheSize = Math.min((int) Runtime.getRuntime().maxMemory() / 6, 4194304);
        sAppIconCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable d) {
                return d.getBitmap().getByteCount();
            }
        };
    }

    public static void cleanupAsync(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    List<String> usedFileNameList = new ArrayList<String>();
                    final String prefsName = context.getPackageName() + "_preferences";
                    SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE);
                    // populate list of used icon files
                    Map<String, ?> keys = prefs.getAll();
                    for (Map.Entry<String, ?> entry : keys.entrySet()) {
                        Object val = entry.getValue();
                        if (entry.getKey().startsWith(PREF_KEY_FINGERPRINT_LAUNCHER_FINGER)) {
                            Set<String> set = (Set<String>) entry.getValue();
                            for (String item : set) {
                                if (item.startsWith("app:")) {
                                    val = item.split(":", 2)[1];
                                }
                            }
                        }
                        if ((val instanceof String) && ((String) val).contains("#Intent")) {
                            try {
                                Intent intent = Intent.parseUri((String) val, 0);
                                String fileName = intent.getStringExtra("icon");
                                if (fileName != null) {
                                    File iconFile = new File(fileName);
                                    if (iconFile.exists()) {
                                        usedFileNameList.add(iconFile.getName());
                                    }
                                }
                            } catch (URISyntaxException ue) {
                                continue;
                            }
                        }
                    }
                    // delete all unused icon files
                    File appPickerDir = new File(context.getFilesDir() + "/app_picker");
                    if (appPickerDir.exists() && appPickerDir.isDirectory()) {
                        File[] files = appPickerDir.listFiles();
                        for (File f : files) {
                            if (!usedFileNameList.contains(f.getName())) {
                                f.delete();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    class AppInfo {
        String name;
        Drawable icon;

        public AppInfo() {
            name = mDefaultSummaryText;
        }
    }

    public AppPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mResources = mContext.getResources();
        mDefaultSummaryText = (String) getSummary();
        mAppIconSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                mResources.getDisplayMetrics());
        mAppIconPreviewSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60,
                mResources.getDisplayMetrics());
        mIconPickSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50,
                mResources.getDisplayMetrics());
        mPackageManager = mContext.getPackageManager();
        mMode = MODE_APP;
        mAppInfo = new AppInfo();
        mAllowGravityBoxActions = true;

        if (attrs != null) {
            mIconPickerEnabled = attrs.getAttributeBooleanValue(null, "iconPickerEnabled", true);
            mNullItemEnabled = attrs.getAttributeBooleanValue(null, "nullItemEnabled", true);
            mForceCustomIcon = attrs.getAttributeBooleanValue(null, "forceCustomIcon", false);
            mAllowGravityBoxActions = attrs.getAttributeBooleanValue(null, "allowGravityBoxActions", true);
        }

        setDialogLayoutResource(R.layout.app_picker_preference);
        setPositiveButtonText(null);

        if (sIconPickerAdapter == null) {
            initializeIconPickerAdapter();
        }
    }

    private void initializeIconPickerAdapter() {
        String[] labels = mResources.getStringArray(R.array.shortcut_icon_picker_labels);
        TypedArray icons = mResources.obtainTypedArray(R.array.shortcut_icon_picker_icons);
        if (labels.length != icons.length()) {
            icons.recycle();
            return;
        }

        ArrayList<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            BasicIconListItem item = new BasicIconListItem(labels[i], null,
                    icons.getResourceId(i, 0), 0, mResources);
            list.add(item);
        }
        sIconPickerAdapter = new IconListAdapter(mContext, list);
        sIconPickerAdapter.setAutoTintIcons(true);
        icons.recycle();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        LinearLayout widgetFrameView = ((LinearLayout) view.findViewById(android.R.id.widget_frame));
        mBtnAppIcon = new ImageButton(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mAppIconPreviewSizePx, mAppIconPreviewSizePx);
        lp.gravity = Gravity.CENTER;
        mBtnAppIcon.setLayoutParams(lp);
        mBtnAppIcon.setScaleType(ScaleType.CENTER_CROP);
        mBtnAppIcon.setImageDrawable(mAppInfo.icon);
        mBtnAppIcon.setFocusable(false);
        if (mIconPickerEnabled) {
            mBtnAppIcon.setOnClickListener(this);
            mBtnAppIcon.setOnLongClickListener(this);
        } else {
            mBtnAppIcon.setEnabled(false);
        }
        widgetFrameView.addView(mBtnAppIcon);
        widgetFrameView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mListView = (ListView) view.findViewById(R.id.icon_list);
        mListView.setOnItemClickListener(this);

        mSearch = (EditText) view.findViewById(R.id.input_search);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                if (mListView.getAdapter() == null)
                    return;

                ((IconListAdapter) mListView.getAdapter()).getFilter().filter(arg0);
            }
        });

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        mModeSpinner = (Spinner) view.findViewById(R.id.mode_spinner);
        ArrayAdapter<String> mModeSpinnerAdapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>(Arrays.asList(
                        mContext.getString(R.string.app_picker_applications),
                        mContext.getString(R.string.app_picker_shortcuts))));
        mModeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mModeSpinner.setAdapter(mModeSpinnerAdapter);
        mModeSpinner.setOnItemSelectedListener(this);
        mMode = mModeSpinner.getSelectedItemPosition();

        setData();
    }

    public void show() {
        showDialog(null);
    }

    public void setAllowUnlockAction(boolean allow) {
        mAllowUnlockAction = allow;
    }

    public void setLaunchesFromLockscreen(boolean value) {
        mLaunchesFromLockscreen = value;
    }

    public void setIconPickerEnabled(boolean value) {
        mIconPickerEnabled = value;
    }

    public Bundle getExtraData() {
        if (mExtraData == null) {
            mExtraData = new Bundle();
        }
        return mExtraData;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = null;

        if (mIconPickerDialog != null && mIconPickerDialog.isShowing()) {
            mIconPickerDialog.dismiss();
        }
        mIconPickerDialog = null;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            String value = getPersistedString(null);
            if (value != null && value.contains(SEPARATOR)) {
                value = convertOldValueFormat(value);
            }
            mAppInfo = getAppInfoFromValue(value);
            setSummary(mAppInfo.name);
        } else {
            setValue(null);
            setSummary(mDefaultSummaryText);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != mBtnAppIcon ||
                sIconPickerAdapter == null ||
                getPersistedString(null) == null) return;

        if (mIconPickerDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.icon_picker_choose_icon_title)
                    .setAdapter(sIconPickerAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            try {
                                BasicIconListItem item = (BasicIconListItem) sIconPickerAdapter.getItem(which);
                                Intent intent = Intent.parseUri(getPersistedString(null), 0);
                                if (intent.hasExtra("icon")) {
                                    intent.removeExtra("icon");
                                }
                                intent.putExtra("iconResName", mResources.getResourceEntryName(
                                        item.getIconLeftId()));
                                setValue(intent.toUri(0));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            mIconPickerDialog = builder.create();
        }

        mIconPickerDialog.show();
    }

    @Override
    public boolean onLongClick(View v) {
        /*if (v != mBtnAppIcon || sPrefsFragment == null ||
                getPersistedString(null) == null) return true;

        sPrefsFragment.pickIcon(mIconPickSizePx, new IconPickHandler() {
            @Override
            public void onIconPicked(Bitmap icon) {
                try {
                    Intent intent = Intent.parseUri(getPersistedString(null), 0);
                    final Context context = AppPickerPreference.this.mContext;
                    final String dir = context.getFilesDir() + "/app_picker";
                    final String fileName = dir + "/" + UUID.randomUUID().toString();
                    File d = new File(dir);
                    d.mkdirs();
                    d.setReadable(true, false);
                    d.setExecutable(true, false);
                    File f = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(f);
                    if (icon.compress(CompressFormat.PNG, 100, fos)) {
                        if (intent.hasExtra("iconResName")) {
                            intent.removeExtra("iconResName");
                        }
                        intent.putExtra("icon", f.getAbsolutePath());
                        f.setReadable(true, false);
                    }
                    fos.close();
                    setValue(intent.toUri(0));
                    sPrefsFragment.onSharedPreferenceChanged(getSharedPreferences(), getKey());
                } catch (Exception e) {
                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onIconPickCancelled() {
                Toast.makeText(mContext, R.string.app_picker_icon_pick_cancelled, Toast.LENGTH_SHORT).show();
            }
        });*/

        return true;
    }

    private String convertOldValueFormat(String oldValue) {
        try {
            String[] splitValue = oldValue.split(SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(cn);
            intent.putExtra("mode", MODE_APP);
            String newValue = intent.toUri(0);
            setValue(newValue);
            Log.d(TAG, "Converted old AppPickerPreference value: " + newValue);
            return newValue;
        } catch (Exception e) {
            Log.e(TAG, "Error converting old AppPickerPreference value: " + e.getMessage());
            return null;
        }
    }

    public void setDefaultSummary(String summary) {
        mDefaultSummaryText = summary;
    }

    private void setData() {
        mAsyncTask = new AsyncTask<Void, Void, ArrayList<IIconListAdapterItem>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mListView.setVisibility(View.INVISIBLE);
                mSearch.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected ArrayList<IIconListAdapterItem> doInBackground(Void... arg0) {
                ArrayList<IIconListAdapterItem> itemList = new ArrayList<IIconListAdapterItem>();
                List<ResolveInfo> appList = new ArrayList<ResolveInfo>();

                List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);
                Intent mainIntent = new Intent();
                if (mMode == MODE_APP) {
                    mainIntent.setAction(Intent.ACTION_MAIN);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                } else if (mMode == MODE_SHORTCUT) {
                    mainIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                }
                for (PackageInfo pi : packages) {
                    if (this.isCancelled()) break;
                    if (mMode == MODE_SHORTCUT &&
                            pi.packageName.equals(mContext.getPackageName()) &&
                            !mAllowGravityBoxActions)
                        continue;
                    mainIntent.setPackage(pi.packageName);
                    List<ResolveInfo> activityList = mPackageManager.queryIntentActivities(mainIntent, 0);
                    for (ResolveInfo ri : activityList) {
                        appList.add(ri);
                    }
                }

                Collections.sort(appList, new ResolveInfo.DisplayNameComparator(mPackageManager));
                if (mNullItemEnabled) {
                    itemList.add(mMode == MODE_SHORTCUT ?
                            new ShortcutItem(mContext.getString(R.string.app_picker_none), null) :
                            new AppItem(mContext.getString(R.string.app_picker_none), null));
                }
                for (ResolveInfo ri : appList) {
                    if (this.isCancelled()) break;
                    String appName = ri.loadLabel(mPackageManager).toString();
                    IIconListAdapterItem ai = mMode == MODE_SHORTCUT ?
                            new ShortcutItem(appName, ri) : new AppItem(appName, ri);
                    itemList.add(ai);
                }

                return itemList;
            }

            @Override
            protected void onPostExecute(ArrayList<IIconListAdapterItem> result) {
                mProgressBar.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                mListView.setAdapter(new IconListAdapter(mContext, result));
                ((IconListAdapter) mListView.getAdapter()).notifyDataSetChanged();
                mListView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    public void setValue(String value) {
        if (!callChangeListener(value))
            return;

        mValue = value;
        mAppInfo = getAppInfoFromValue(value);
        setSummary(mAppInfo.name);
        if (mBtnAppIcon != null) {
            mBtnAppIcon.setImageDrawable(mAppInfo.icon);
        }
        persistString(value);
    }

    public String getValue() {
        return mValue;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IIconListAdapterItem item = (IIconListAdapterItem) parent.getItemAtPosition(position);
        if (mMode == MODE_APP) {
            AppItem ai = (AppItem) item;
            setValue(ai.getValue());
            getDialog().dismiss();
        } else if (mMode == MODE_SHORTCUT) {
            ShortcutItem si = (ShortcutItem) item;
            if (si.getCreateShortcutIntent() == null) {
                setValue(null);
                getDialog().dismiss();
            } else {
                si.setShortcutCreatedListener(new ShortcutCreatedListener() {
                    @Override
                    public void onShortcutCreated(ShortcutItem sir) {
                        setValue(sir.getValue());
                        // we have to call this explicitly for some yet unknown reason...
                        settingsFragment.onSharedPreferenceChanged(getSharedPreferences(), getKey());
                        getDialog().dismiss();
                    }
                });
                settingsFragment.obtainShortcut((ShortcutItem) item);
            }
        }
    }

    private AppInfo getAppInfoFromValue(String value) {
        AppInfo appInfo = new AppInfo();
        if (value == null) return appInfo;

        try {
            Intent intent = Intent.parseUri(value, 0);

            @DrawableRes
            int iconResId = intent.getStringExtra("iconResName") != null ?
                    mResources.getIdentifier(intent.getStringExtra("iconResName"),
                            "drawable", mContext.getPackageName()) : 0;
            if (iconResId != 0) {
                appInfo.icon = mResources.getDrawable(iconResId);
            } else if (intent.hasExtra("icon")) {
                final String appIconPath = intent.getStringExtra("icon");
                if (appIconPath != null) {
                    File f = new File(appIconPath);
                    if (f.exists() && f.canRead()) {
                        FileInputStream fis = new FileInputStream(f);
                        appInfo.icon = new BitmapDrawable(mResources, BitmapFactory.decodeStream(fis));
                        fis.close();
                    }
                }
            }

            int mode = intent.getIntExtra("mode", MODE_APP);
            if (mode == MODE_APP) {
                ComponentName cn = intent.getComponent();
                ActivityInfo ai = mPackageManager.getActivityInfo(cn, 0);
                appInfo.name = (ai.loadLabel(mPackageManager).toString());
                if (appInfo.icon == null) {
                    appInfo.icon = ai.loadIcon(mPackageManager);
                }
            } else if (mode == MODE_SHORTCUT) {
                appInfo.name = intent.getStringExtra("prefLabel");
            }
            return appInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return appInfo;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mMode = position;
        setData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    class AppItem implements IIconListAdapterItem {
        protected String mAppName;
        protected BitmapDrawable mAppIcon;
        protected ResolveInfo mResolveInfo;
        protected Intent mIntent;

        private AppItem() {
        }

        public AppItem(String appName, ResolveInfo ri) {
            mAppName = appName;
            mResolveInfo = ri;
            if (mResolveInfo != null) {
                mIntent = new Intent(Intent.ACTION_MAIN);
                mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                ComponentName cn = new ComponentName(mResolveInfo.activityInfo.packageName,
                        mResolveInfo.activityInfo.name);
                mIntent.setComponent(cn);
                mIntent.putExtra("mode", MODE_APP);
                if (mForceCustomIcon) {
                    mIntent.putExtra("iconResName", "ic_shortcut_help");
                }
            }
        }

        public String getAppName() {
            return mAppName;
        }

        public String getValue() {
            return (mIntent == null ? null : mIntent.toUri(0));
        }

        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public String getText() {
            return mAppName;
        }

        @Override
        public String getSubText() {
            return null;
        }

        protected String getKey() {
            return getValue();
        }

        @Override
        public Drawable getIconLeft() {
            if (mResolveInfo == null) return null;

            if (mAppIcon == null) {
                final String key = getKey();
                mAppIcon = sAppIconCache.get(key);
                if (mAppIcon == null) {
                    Bitmap bitmap = Utils.drawableToBitmap(mResolveInfo.loadIcon(mPackageManager));
                    bitmap = Bitmap.createScaledBitmap(bitmap, mAppIconSizePx, mAppIconSizePx, false);
                    mAppIcon = new BitmapDrawable(mResources, bitmap);
                    sAppIconCache.put(key, mAppIcon);
                }
            }
            return mAppIcon;
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }
    }

    interface ShortcutCreatedListener {
        void onShortcutCreated(ShortcutItem item);
    }

    class ShortcutItem extends AppItem implements ShortcutHandler {
        private Intent mCreateShortcutIntent;
        private ShortcutCreatedListener mShortcutCreatedListener;

        public ShortcutItem(String appName, ResolveInfo ri) {
            mAppName = appName;
            mResolveInfo = ri;
            if (mResolveInfo != null) {
                mCreateShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                ComponentName cn = new ComponentName(mResolveInfo.activityInfo.packageName,
                        mResolveInfo.activityInfo.name);
                mCreateShortcutIntent.setComponent(cn);
                // mark intent so we can later identify it comes from GB
                mCreateShortcutIntent.putExtra("gravitybox", true);
                if (mAllowUnlockAction) {
                    mCreateShortcutIntent.putExtra(EXTRA_ALLOW_UNLOCK_ACTION, true);
                }
                if (mLaunchesFromLockscreen) {
                    mCreateShortcutIntent.putExtra(EXTRA_LAUNCHES_FROM_LOCKSCREEN, true);
                }
            }
        }

        public void setShortcutCreatedListener(ShortcutCreatedListener listener) {
            mShortcutCreatedListener = listener;
        }

        @Override
        protected String getKey() {
            return mCreateShortcutIntent.toUri(0);
        }

        @Override
        public Intent getCreateShortcutIntent() {
            return mCreateShortcutIntent;
        }

        @Override
        public void onHandleShortcut(Intent intent, String name, String localIconResName, Bitmap icon) {
            if (intent == null) {
                Toast.makeText(mContext, R.string.app_picker_shortcut_null_intent, Toast.LENGTH_LONG).show();
                return;
            }

            mIntent = intent;
            mIntent.putExtra("mode", MODE_SHORTCUT);

            // generate label
            if (name != null) {
                mIntent.putExtra("label", name);
                mIntent.putExtra("prefLabel", mAppName + ": " + name);
            } else {
                mIntent.putExtra("label", mAppName);
                mIntent.putExtra("prefLabel", mAppName);
            }

            // process icon
            if (mForceCustomIcon) {
                mIntent.putExtra("iconResName", "ic_shortcut_help");
            } else if (localIconResName != null) {
                mIntent.putExtra("iconResName", localIconResName);
            } else if (icon != null) {
                try {
                    final Context context = AppPickerPreference.this.mContext;
                    final String dir = context.getFilesDir() + "/app_picker";
                    final String fileName = dir + "/" + UUID.randomUUID().toString();
                    File d = new File(dir);
                    d.mkdirs();
                    d.setReadable(true, false);
                    d.setExecutable(true, false);
                    File f = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(f);
                    final boolean iconSaved = icon.compress(CompressFormat.PNG, 100, fos);
                    if (iconSaved) {
                        mIntent.putExtra("icon", f.getAbsolutePath());
                        f.setReadable(true, false);
                    }
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // callback to shortcut created listener if set
            if (mShortcutCreatedListener != null) {
                mShortcutCreatedListener.onShortcutCreated(this);
            }
        }

        @Override
        public void onShortcutCancelled() {
            Toast.makeText(mContext, R.string.app_picker_shortcut_cancelled, Toast.LENGTH_SHORT).show();
        }
    }

    public interface ShortcutHandler {
        Intent getCreateShortcutIntent();

        void onHandleShortcut(Intent intent, String name,
                              String localIconResName, Bitmap icon);

        void onShortcutCancelled();
    }
}
