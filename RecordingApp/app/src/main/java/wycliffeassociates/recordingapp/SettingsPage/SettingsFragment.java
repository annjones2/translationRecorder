package wycliffeassociates.recordingapp.SettingsPage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.login.core.Profile;

import org.json.JSONObject;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.SplashScreen;

/**
 * Created by leongv on 12/17/2015.
 */
public class SettingsFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener{

    Context context;

    public static final String KEY_PREF_LANG = "pref_lang";
    public static final String KEY_PREF_BOOK = "pref_book";
    public static final String KEY_PREF_CHAPTER = "pref_chapter";
    public static final String KEY_PREF_CHUNK = "pref_chunk";
    private static final String KEY_PREF_FILENAME = "pref_filename";
    private static final String KEY_PREF_TAKE = "pref_take";
    private static final String KEY_PREF_CHUNK_VERSE = "pref_chunk_verse";
    private static final String KEY_PREF_VERSE = "pref_verse";
//    sharedPref;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();

        context = getActivity();

        // Below is the code to clear the SharedPreferences. Use it wisely.
        // sharedPref.edit().clear().commit();

        // Register listener(s)
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        // Initial summary update to display the right values
        for (String k : sharedPref.getAll().keySet()) {
            System.out.println("UPDATING SUMMARY FOR: " + k);
            updateSummaryText(sharedPref, k);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get rid of the extra padding in the settings page body (where it loads this fragment)
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, 0, 0, 0);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        updateSummaryText(sharedPref, key);
        Settings.updateFilename(getActivity());
    }

    private void updateSummariesSetViaActivities(SharedPreferences sharedPref){
        String uristring = sharedPref.getString(Settings.KEY_PREF_SRC_LOC, "");
        Uri dir = Uri.parse(uristring);
        if(dir != null) {
            uristring = dir.getLastPathSegment();
            //This removes "primary:", though maybe this is helpful in identifying between sd card and internal storage.
            //uristring = uristring.substring(uristring.indexOf(":")+1, uristring.length());
            findPreference(Settings.KEY_PREF_SRC_LOC).setSummary(uristring);
        } else {
            findPreference(Settings.KEY_PREF_SRC_LOC).setSummary(sharedPref.getString(Settings.KEY_PREF_SRC_LOC, ""));
        }
    }

    public void updateSummaryText(SharedPreferences sharedPref, String key) {
        try {
            updateSummariesSetViaActivities(sharedPref);
            String text  = sharedPref.getString(key, "");
            if(findPreference(key) != null) {
                findPreference(key).setSummary(text);
            }
        } catch (ClassCastException err) {
            System.out.println("IGNORING SUMMARY UPDATE FOR " + key);
        }
    }
}