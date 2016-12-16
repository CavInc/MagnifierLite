package cav.magnifierlite;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by cav on 14.12.16.
 */
public class PrefActivity extends PreferenceActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}
