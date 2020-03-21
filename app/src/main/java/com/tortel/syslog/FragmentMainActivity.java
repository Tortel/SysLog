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
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.tortel.syslog.dialog.AboutDialog;
import com.tortel.syslog.dialog.AboutLogcatDialog;
import com.tortel.syslog.dialog.ClearBufferDialog;
import com.tortel.syslog.dialog.FaqDialog;
import com.tortel.syslog.fragment.MainFragment;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Prefs;
import com.tortel.syslog.utils.Utils;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Main activity, fragment version
 */
public class FragmentMainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    public static final String[] REQUIRED_PERMISSIONS = {};
    public static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 10;
    public static final String[] SCRUB_PERMISSIONS = {};
    public static final int SCRUB_PERMISSIONS_REQUEST_CODE = 11;

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

        checkRequiredPermissions();
    }

    /**
     * Check that we were granted runtime permissions
     */
    public boolean checkRequiredPermissions(){
        // Check for permissions
        boolean hasPermissions = EasyPermissions.hasPermissions(this, REQUIRED_PERMISSIONS);
        if(!hasPermissions){
            EasyPermissions.requestPermissions(this, getString(R.string.required_permission_detail), REQUIRED_PERMISSIONS_REQUEST_CODE, REQUIRED_PERMISSIONS);
        }
        return hasPermissions;
    }

    /**
     * Check that we were granted the permissions needed to scrub the logs
     */
    public boolean checkScrubPermissions(){
        // Check for permissions
        boolean hasPermissions = EasyPermissions.hasPermissions(this, SCRUB_PERMISSIONS);
        if(!hasPermissions){
            EasyPermissions.requestPermissions(this, getString(R.string.scrub_permission_detail), SCRUB_PERMISSIONS_REQUEST_CODE, SCRUB_PERMISSIONS);
        }
        return hasPermissions;
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
            case R.id.clean_all:
                new Utils.CleanAllTask(getBaseContext()).execute();
                return true;
            case R.id.clear_buffer:
                // Check if we should just do it, or show the dialog
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if(prefs.getBoolean(Prefs.KEY_NO_BUFFER_WARN, false)) {
                    // Just run the task
                    new Utils.ClearLogcatBufferTask(getApplicationContext()).execute();
                } else {
                    // Show the dialog
                    showClearBufferConfirmation();
                }
                return true;
            case R.id.about:
                showAboutDialog();
                return true;
            case R.id.about_live:
                showAboutLiveLogcatDialog();
                return true;
            case R.id.faq:
                showFaqDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(resultCode, permissions, grantResults);
        // Pass everything to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(resultCode, permissions, grantResults, this);
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
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "about");
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

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }
}
