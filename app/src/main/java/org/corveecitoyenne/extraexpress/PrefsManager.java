package org.corveecitoyenne.extraexpress;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by saiimons on 18/10/14.
 */
public class PrefsManager {
    private Context mContext;
    private static final String PREFS_MAIL = "mail";
    private static final String KEY_MAIL_PACKAGE = "pkg";
    private static final String KEY_MAIL_CLASS = "cls";
    private String mDefPkg;
    private String mDefName;

    public PrefsManager(Context context) {
        mContext = context;
        getDefaultMailClient();
    }

    public String getMailClientPkg() {
        return mContext.getSharedPreferences(PREFS_MAIL, Context.MODE_PRIVATE).getString(KEY_MAIL_PACKAGE, mDefPkg);
    }

    public String getMailClientName() {
        return mContext.getSharedPreferences(PREFS_MAIL, Context.MODE_PRIVATE).getString(KEY_MAIL_CLASS, mDefName);
    }

    public void updateMailClient(String pkg, String name) {
        mContext.getSharedPreferences(PREFS_MAIL, Context.MODE_PRIVATE).edit().putString(KEY_MAIL_PACKAGE, pkg).putString(KEY_MAIL_CLASS, name).commit();
    }

    public void getDefaultMailClient() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");
        final PackageManager pm = mContext.getPackageManager();
        final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
        ResolveInfo best = null;
        if (matches.size() > 0) {
            best = matches.get(0);
            for (final ResolveInfo info : matches) {
                Log.d("Pref", info.toString());
                if (info.activityInfo.packageName.endsWith(".gm") ||
                        info.activityInfo.name.toLowerCase().contains("gmail")) best = info;
            }
            mDefPkg = best.activityInfo.packageName;
            mDefName = best.activityInfo.name;
        }
    }
}
