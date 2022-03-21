package nz.mentalinc.episodeWatcher;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class SettingScreenFrag extends PreferenceFragmentCompat {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey);

    }
}

