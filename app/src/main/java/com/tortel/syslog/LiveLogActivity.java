package com.tortel.syslog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tortel.syslog.dialog.AboutLogcatDialog;
import com.tortel.syslog.utils.InputStreamWrapper;
import com.tortel.syslog.utils.Prefs;

import java.io.InputStream;

import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

/**
 * Activity for viewing the love logcat
 */
public class LiveLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fragMan = getSupportFragmentManager();
        if(fragMan.findFragmentById(R.id.content_frame) == null){
            restartLogcatFragment();
        }

        // Check to show the about dialog
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean(Prefs.KEY_LIVE_LOGCAT_ABOUT, false)){
            AboutLogcatDialog dialog = new AboutLogcatDialog();
            dialog.show(getSupportFragmentManager(), "logcat_about");

            // Save the preference
            prefs.edit().putBoolean(Prefs.KEY_LIVE_LOGCAT_ABOUT, true).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logcat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            case R.id.stop_logcat:
                LiveLogFragment fragment = getFragment();
                if(fragment != null) {
                    fragment.stop();
                }
                return true;
            case R.id.restart_logcat:
                restartLogcatFragment();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        private EmulatorView mEmulatorView;
        private TermSession mTermSession = new TermSession();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        /**
         * Stop the logcat process
         */
        public void stop(){
            mTermSession.stopLogcat();
            Toast.makeText(getActivity(), R.string.stopped_logcat, Toast.LENGTH_SHORT).show();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mEmulatorView = new EmulatorView(getActivity().getBaseContext(), mTermSession, metrics);

            return mEmulatorView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mTermSession.finish();
        }

    }
}
