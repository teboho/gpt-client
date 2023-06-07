package me.teboho.chatwithgpt;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.zip.Inflater;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        // bind the preference screen to the shared preferences
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);

        // get the preference and set the summary to the current value
        EditTextPreference editTextPreference = (EditTextPreference) findPreference("pref_name");
        editTextPreference.setSummary(editTextPreference.getText());

        // watch for changes in the preferences and check which preference changed then read the new value and apply it
        // watch the switch preference
        SwitchPreference switchPreference = (SwitchPreference) findPreference("pref_dark_mode");
        switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // set the default value of the theme to light
            boolean isDarkMode = (boolean) newValue;
            // load the preferences and watch for changes
            AppCompatDelegate
                    .setDefaultNightMode(isDarkMode? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        });
        // watch the edit text preference
        EditTextPreference editTextPreference1 = (EditTextPreference) findPreference("pref_name");
        editTextPreference1.setOnPreferenceChangeListener((preference, newValue) -> {
            // set the default value of the theme to light
            String name = (String) newValue;
            // load the preferences and watch for changes
            editTextPreference1.setSummary(name);

            // modify the navigation header
            // get the navigation header
            TextView tv_name = (getActivity().findViewById(R.id.drawerLayout).findViewById(R.id.tv_name));
            tv_name.setText(name);

            return true;
        });
    }
}