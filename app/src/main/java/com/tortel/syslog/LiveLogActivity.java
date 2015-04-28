package com.tortel.syslog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.tortel.syslog.utils.InputStreamWrapper;

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
            Fragment frag = new LiveLogFragemnt();
            fragMan.beginTransaction().replace(R.id.content_frame, frag).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class LiveLogFragemnt extends Fragment {
        private EmulatorView mEmulatorView;
        private TermSession mTermSession;
        private Process mTermProcess;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            try {
                mTermProcess = Runtime.getRuntime().exec("su -c logcat");

                mTermSession = new TermSession();

                InputStream is = new InputStreamWrapper(mTermProcess.getInputStream());
                mTermSession.setTermIn(is);
                mTermSession.setTermOut(mTermProcess.getOutputStream());
                mTermSession.setDefaultUTF8Mode(true);
            } catch (Throwable e1) {
                try{
                    mTermProcess.destroy();
                } catch (Exception e2) {
                    // Ignore
                }
                e1.printStackTrace();
            }
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
            mTermProcess.destroy();
        }

    }
}
