package org.corveecitoyenne.extraexpress;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class SubmitActivity extends Activity {
    public static final String EXTRA_PICTURE = "picture";
    private SubmitFragment mFragment;
    private ListView mailClients;
    private PrefsManager prefs;
    private MailClientAdapter mailClientAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        mFragment = SubmitFragment.newInstance();
        mFragment.getArguments().putByteArray(SubmitFragment.ARG_IMG_BTYES, getIntent().getByteArrayExtra(EXTRA_PICTURE));
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFragment)
                    .commit();
        }
        mailClients = ((ListView) findViewById(R.id.mail_clients));
        mailClientAdapter = new MailClientAdapter();
        mailClients.setAdapter(mailClientAdapter);
        prefs = new PrefsManager(this);
        mailClients.setSelector(R.drawable.mail_client_selector);
        mailClients.setItemChecked(mailClientAdapter.getPositionFor(prefs.getMailClientPkg(), prefs.getMailClientName()), true);
        mailClients.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActivityInfo infos = mailClientAdapter.getItem(position).activityInfo;
                prefs.updateMailClient(infos.packageName, infos.name);
            }
        });
    }

    private List<ResolveInfo> getMailClients() {
        List<ResolveInfo> apps = new ArrayList<ResolveInfo>();
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo app : matches) {
            if ( //
                    app.activityInfo.packageName.contains("mail") //
                            || app.activityInfo.packageName.contains("Mail") //
                            || app.activityInfo.name.contains("mail") //
                            || app.activityInfo.name.contains("Mail") //
                    ) {
                apps.add(app);
            }
        }
        return apps;
    }

    private class MailClientAdapter extends BaseAdapter {
        public List<ResolveInfo> mApps;

        public MailClientAdapter() {
            mApps = getMailClients();
        }

        @Override
        public int getCount() {
            return mApps.size();
        }

        @Override
        public ResolveInfo getItem(int position) {
            return mApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.mail_client_view, parent, false);
            }
            try {
                PackageManager pm = getPackageManager();
                ((ImageView) convertView.findViewById(R.id.mail_client_icon)).setImageDrawable(//
                        pm.getApplicationIcon(getItem(position).activityInfo.packageName)//
                );
                ((TextView) convertView.findViewById(R.id.mail_client_name)).setText(//
                        pm.getApplicationLabel(pm.getApplicationInfo(getItem(position).activityInfo.packageName, 0))//
                );
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return convertView;
        }

        public int getPositionFor(String pkg, String name) {
            ResolveInfo app;
            for (int i = 0; i < mApps.size(); i++) {
                app = mApps.get(i);
                if (app.activityInfo.packageName.equals(pkg) && app.activityInfo.name.equals(name)) {
                    return i;
                }
            }
            return 0;
        }
    }


}
