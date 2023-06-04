package me.teboho.chatwithgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

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

    static final ChatFragment chatFragment = new ChatFragment();
    static final SettingsFragment settingsFragment = new SettingsFragment();

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
        getSupportActionBar().setTitle("Chat With GPT");

        // set the chat fragment as the default fragment to show
        setFragment(chatFragment);
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

        // watch for changes in the preferences and check which preference changed then read the new value and apply it
        sharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences1, key) -> {
            if (key.equals("pref_dark_mode")) {
                boolean _isDarkMode = sharedPreferences1.getBoolean("pref_dark_mode", false);
                AppCompatDelegate
                        .setDefaultNightMode(_isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            }
            if (key.equals("pref_name")) {
                String _username = sharedPreferences1.getString("pref_name", "User");
                tvName.setText(_username);
                tvName.invalidate(); // invalidate the view to force it to redraw
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_chat) {
            setFragment(chatFragment);
            return true;
        }
        if (item.getItemId() == R.id.item_settings) {
            setFragment(settingsFragment);
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
        Snackbar sb = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).setTextColor(Color.CYAN);
        sb.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
        sb.show();
    }

    private void setFragment(Fragment fragment) {
        getSupportActionBar().setTitle(fragment instanceof ChatFragment ? "Chat" : "Settings");

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        if (binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawers();
    }
}