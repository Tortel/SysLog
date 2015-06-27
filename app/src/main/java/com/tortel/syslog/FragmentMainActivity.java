package com.tortel.syslog;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.tortel.syslog.dialog.AboutDialog;
import com.tortel.syslog.dialog.CustomPathDialog;
import com.tortel.syslog.dialog.FaqDialog;
import com.tortel.syslog.fragment.MainFragment;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Utils;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;

/**
 * Main activity, fragment version
 */
public class FragmentMainActivity extends AppCompatActivity {
    private static final int TILE_ID = 4321;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.findFragmentById(R.id.content_frame) == null){
            Log.d("Fragment null, replacing");
            MainFragment frag = new MainFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, frag).commit();
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, FragmentMainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        CustomTile tile = new CustomTile.Builder(this)
                .setIcon(R.drawable.ic_launcher)
                .setLabel(R.string.app_name)
                .setContentDescription("Do Something")
                .setOnClickIntent(pendingIntent)
                .build();

        CMStatusBarManager cmStatusBarManager = CMStatusBarManager.getInstance(this);
        cmStatusBarManager.publishTile(TILE_ID, tile);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.live_logcat:
                Intent intent = new Intent(this, LiveLogActivity.class);
                startActivity(intent);
                return true;
            case R.id.clean_uncompressed:
                new Utils.CleanUncompressedTask(getBaseContext()).execute();
                return true;
            case R.id.clean_all:
                new Utils.CleanAllTask(getBaseContext()).execute();
                return true;
            case R.id.change_path:
                showPathDialog();
                return true;
            case R.id.about:
                showAboutDialog();
                return true;
            case R.id.faq:
                showFaqDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Shows the change path dialog
     */
    private void showPathDialog(){
        CustomPathDialog dialog = new CustomPathDialog();
        dialog.show(getSupportFragmentManager(), "path");
    }

    /**
     * Shows the About dialog box
     */
    private void showAboutDialog(){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "about");
    }

    /**
     * Shows the FAQ dialog box
     */
    private void showFaqDialog(){
        FaqDialog dialog = new FaqDialog();
        dialog.show(getSupportFragmentManager(), "faq");
    }
}
