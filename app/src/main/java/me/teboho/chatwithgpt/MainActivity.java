package me.teboho.chatwithgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import me.teboho.chatwithgpt.databinding.ActivityMainBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This is the main activity of the app
 * It handles the UI and the logic of the app
 * @author teboho
 */
public class MainActivity extends AppCompatActivity {
    static ActivityMainBinding binding;
    ActionBarDrawerToggle toggle; // Toggle button for the drawer

    ChatFragment chatFragment;
    NoHistoryFragment noHistoryFragment;
    SettingsFragment settingsFragment;
    ImageGenFragment imageGenFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // set the toolbar as the action bar of the activity to support using the action bar toggle to pull out the drawer layout for menu
        setSupportActionBar(binding.toolbar);
        toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setTitle("Chat");

        chatFragment = new ChatFragment();
        noHistoryFragment = new NoHistoryFragment();
        settingsFragment = new SettingsFragment();
        imageGenFragment = new ImageGenFragment();

        setFragment(chatFragment);
        getSupportActionBar().setSubtitle("Chat With GPT");
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadPreferences();
        // support drawer menu item selections
        binding.navView.setNavigationItemSelectedListener(this::onOptionsItemSelected);
    }

    protected void loadPreferences() {
        // Accommodating SharedPreferences for the dark mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // set the default value of the theme to light
        boolean isDarkMode = sharedPreferences.getBoolean("pref_dark_mode", false);
        String username = sharedPreferences.getString("pref_name", "User");

        // load the preferences and watch for changes
        AppCompatDelegate
                .setDefaultNightMode(isDarkMode? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        TextView tvName = binding.navView.getHeaderView(0).findViewById(R.id.tv_name);
        tvName.setText(username);
    }

    private void __onPreferenceChangeListener(SharedPreferences sharedPreferences, String key) {
        TextView tvName = binding.navView.getHeaderView(0).findViewById(R.id.tv_name);

        if (key.equals("pref_dark_mode")) {
            System.out.println("Dark mode changed");
            boolean _isDarkMode = sharedPreferences.getBoolean("pref_dark_mode", false);
            AppCompatDelegate
                    .setDefaultNightMode(_isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            finish();
            startActivity(getIntent());
        }
        if (key.equals("pref_name")) {
            System.out.println("Name changed");
            String _username = sharedPreferences.getString("pref_name", "User");
            tvName.setText(_username);
            tvName.invalidate();
            binding.drawerLayout.invalidate();
            // get the preference and set the summary to the current value
            androidx.preference.EditTextPreference editTextPreference = (androidx.preference.EditTextPreference) settingsFragment.findPreference("pref_name");
            editTextPreference.setSummary(editTextPreference.getText());

            // show a snackbar to notify the user that the name has changed
            showSnackbar("Name changed to " + _username + "\n changes will take wide effect after restarting the app");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_chat) {
            setFragment(chatFragment);
            return true;
        }
        if (item.getItemId() == R.id.item_no_history) {

            setFragment(noHistoryFragment);
            return true;
        }
        if (item.getItemId() == R.id.item_settings) {
            setFragment(settingsFragment);
            return true;
        }

        if (item.getItemId() == R.id.item_image_gen) {
            setFragment(imageGenFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // overriding the onCreateOptions menu to inflate the menu also into the toolbar default menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawer_menu, menu);
        return true;
    }

    /**
     * This method shows a snackbar with the given message
     * @param message the message to show in the snackbar
     */
    public static void showSnackbar(String message) {
        Snackbar sb = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        sb.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
        sb.show();
    }

    private void setFragment(Fragment fragment) {
        getSupportActionBar().setTitle(fragment.getClass().getSimpleName().replace("Fragment", ""));

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.slide_out)
                .replace(R.id.fragment_container, fragment)
                .commit();

        if (binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawers();
    }
}