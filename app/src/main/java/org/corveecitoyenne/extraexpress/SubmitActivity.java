package org.corveecitoyenne.extraexpress;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;


public class SubmitActivity extends Activity {
    public static final String EXTRA_PICTURE = "picture";
    private SubmitFragment mFragment;

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
    }


}
