/* SysLog - A simple logging tool
 * Copyright (C) 2020  Scott Warner <Tortel1210@gmail.com>
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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pcchin.licenseview.LicenseType;
import com.tortel.syslog.databinding.ActivityLicenseBinding;

/**
 * Activity that displays the licenses for 3rd party libraries
 */
public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLicenseBinding binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == android.R.id.home) {
                finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        });
        binding.toolbar.setNavigationOnClickListener((View v) -> {
            this.finish();
        });

        binding.licenseView.setAlertDialogStyle(R.style.Theme_AppCompat_Dialog_Alert);

        // Add all the deps
        binding.licenseView.addLicense(getString(R.string.lib_androidx), LicenseType.APACHE_2);
        binding.licenseView.addLicense(getString(R.string.lib_material), LicenseType.APACHE_2);
        binding.licenseView.addLicense(getString(R.string.lib_libsuperuser), LicenseType.APACHE_2);
        binding.licenseView.addLicense(getString(R.string.lib_termview), LicenseType.APACHE_2);
        binding.licenseView.addLicense(getString(R.string.lib_licenseview), LicenseType.APACHE_2);
    }

}
