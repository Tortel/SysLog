/* SysLog - A simple logging tool
 * Copyright (C) 2013-2020  Scott Warner <Tortel1210@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tortel.syslog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.view.MenuItem;

import com.tortel.syslog.databinding.ActivityMainBinding;
import com.tortel.syslog.dialog.AboutDialog;
import com.tortel.syslog.dialog.AboutLogcatDialog;
import com.tortel.syslog.dialog.ClearBufferDialog;
import com.tortel.syslog.dialog.FaqDialog;
import com.tortel.syslog.fragment.MainFragment;
import com.tortel.syslog.utils.FileUtils;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Prefs;
import com.tortel.syslog.utils.Utils;

/**
 * Main activity, fragment version
 */
public class FragmentMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == R.id.live_logcat) {
                startActivity(new Intent(this, LiveLogActivity.class));
                return true;
            } else if (item.getItemId() == R.id.clean_all) {
                FileUtils.cleanAllLogs(this);
                return true;
            } else if (item.getItemId() == R.id.clear_buffer) {
                // Check if we should just do it, or show the dialog
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (prefs.getBoolean(Prefs.KEY_NO_BUFFER_WARN, false)) {
                    // Just run the task
                    Utils.clearLogcatBuffer(this);
                } else {
                    // Show the dialog
                    showClearBufferConfirmation();
                }
                return true;
            } else if (item.getItemId() == R.id.about) {
                showAboutDialog();
                return true;
            } else if (item.getItemId() == R.id.about_live) {
                showAboutLiveLogcatDialog();
                return true;
            } else if (item.getItemId() == R.id.faq) {
                showFaqDialog();
                return true;
            } else if (item.getItemId() == R.id.license) {
                startActivity(new Intent(this, LicenseActivity.class));
            }
            return super.onOptionsItemSelected(item);
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.findFragmentById(R.id.content_frame) == null){
            Log.d("Fragment null, replacing");
            MainFragment frag = new MainFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, frag).commit();
        }
    }

    /**
     * Show a dialog warning about clearing the buffer
     */
    private void showClearBufferConfirmation(){
        ClearBufferDialog dialog = new ClearBufferDialog();
        dialog.show(getSupportFragmentManager(), "buffer");
    }

    /**
     * Shows the About dialog box
     */
    private void showAboutDialog(){
        AboutDialog dialog = new AboutDialog(this);
        dialog.getDialog().show();
        // dialog.show(getSupportFragmentManager(), "about");
    }

    /**
     * Shows the About dialog box
     */
    private void showAboutLiveLogcatDialog(){
        AboutLogcatDialog dialog = new AboutLogcatDialog();
        dialog.show(getSupportFragmentManager(), "about_logcat");
    }

    /**
     * Shows the FAQ dialog box
     */
    private void showFaqDialog(){
        FaqDialog dialog = new FaqDialog();
        dialog.show(getSupportFragmentManager(), "faq");
    }
}
