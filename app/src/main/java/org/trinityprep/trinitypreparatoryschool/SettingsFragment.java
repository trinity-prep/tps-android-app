package org.trinityprep.trinitypreparatoryschool;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by bmndo on 6/13/2016.
 */
public class SettingsFragment extends PreferenceFragment{
    public static final String KEY_SCHOOL_PREF= "pref_school_level";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
