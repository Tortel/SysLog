/* SysLog - A simple logging tool
 * Copyright (C) 2013-2023  Scott Warner <Tortel1210@gmail.com>
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

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tortel.syslog.databinding.ActivityLogcatBinding;
import com.tortel.syslog.dialog.AboutLogcatDialog;
import com.tortel.syslog.utils.Prefs;

import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

/**
 * Activity for viewing the love logcat
 */
public class LiveLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLogcatBinding binding = ActivityLogcatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == android.R.id.home) {
                finish();
                return true;
            } else if (item.getItemId() == R.id.stop_logcat) {
                LiveLogFragment fragment = getFragment();
                if(fragment != null) {
                    fragment.stop();
                }
                return true;
            } else if (item.getItemId() == R.id.restart_logcat) {
                restartLogcatFragment();
                return true;
            }
            return super.onOptionsItemSelected(item);
        });
        binding.toolbar.setNavigationOnClickListener((View v) -> {
            this.finish();
        });

        FragmentManager fragMan = getSupportFragmentManager();
        if (fragMan.findFragmentById(R.id.content_frame) == null) {
            restartLogcatFragment();
        }

        // Check to show the about dialog
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(Prefs.KEY_LIVE_LOGCAT_ABOUT, false)) {
            AboutLogcatDialog dialog = new AboutLogcatDialog(this);
            dialog.getDialog().show();

            // Save the preference
            prefs.edit().putBoolean(Prefs.KEY_LIVE_LOGCAT_ABOUT, true).apply();
        }
    }

    private void restartLogcatFragment(){
        FragmentManager fragMan = getSupportFragmentManager();
        Fragment frag = new LiveLogFragment();
        fragMan.beginTransaction().replace(R.id.content_frame, frag).commit();
    }

    private LiveLogFragment getFragment(){
        FragmentManager fragMan = getSupportFragmentManager();
        if(fragMan.findFragmentById(R.id.content_frame) != null) {
            return (LiveLogFragment) fragMan.findFragmentById(R.id.content_frame);
        }
        return null;
    }

    public static class LiveLogFragment extends Fragment {
        private final TermSession mTermSession = new TermSession();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        /**
         * Stop the logcat process
         */
        void stop(){
            mTermSession.stopLogcat();
            Toast.makeText(getActivity(), R.string.stopped_logcat, Toast.LENGTH_SHORT).show();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            return new EmulatorView(getActivity().getBaseContext(), mTermSession, metrics);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            try {
                mTermSession.finish();
            } catch(Exception e){
                // Just suppress it
            }
        }

    }
}
