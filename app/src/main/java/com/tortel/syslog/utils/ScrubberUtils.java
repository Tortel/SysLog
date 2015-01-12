/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tortel.syslog.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ScrubberUtils {

    private static final boolean DEBUG = false;
    private static final String TAG = ScrubberUtils.class.getSimpleName();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*(@|%40)(?!([a-zA-Z0-9]*\\.[a-zA-Z0-9]*\\.[a-zA-Z0-9]*\\.))(?:[A-Za-z0-9](?:[a-zA-Z0-9-]*[A-Za-z0-9])?\\.)+[a-zA-Z](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$");
    private static final Pattern WEB_URL_PATTERN = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    private static final Pattern IPADDRESS_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern PHONE_INFO_PATTERN = Pattern.compile("(msisdn=|mMsisdn=|iccid=|iccid: |mImsi=)[a-zA-Z0-9]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern USER_INFO_PATTERN = Pattern.compile("(UserInfo\\{\\d:)[a-zA-Z0-9\\s]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_INFO_PATTERN = Pattern.compile("(Account \\{name=)[a-zA-Z0-9]*", Pattern.CASE_INSENSITIVE);

    public static final String IGNORE_DATA_RESOURCE_CACHE = "/data/resource-cache";
    public static final String IGNORE_DATA_DALVIK_CACHE = "/data/dalvik-cache";
    public static final String IGNORE_CACHE_DALVIK_CACHE = "/cache/dalvik-cache";

    public static String scrubLine(String line, Pattern extraPattern) {
        if (line.contains(IGNORE_DATA_RESOURCE_CACHE)
                || line.contains(IGNORE_DATA_DALVIK_CACHE)
                || line.contains(IGNORE_CACHE_DALVIK_CACHE)) {
            // ugly work around :/
            return line;
        }
        line = IPADDRESS_PATTERN.matcher(line).replaceAll("<IP address omitted>");
        line = EMAIL_PATTERN.matcher(line).replaceAll("<email omitted>");
        line = PHONE_NUMBER_PATTERN.matcher(line).replaceAll("<phone number omitted>");
        line = WEB_URL_PATTERN.matcher(line).replaceAll("<web url omitted>");
        line = PHONE_INFO_PATTERN.matcher(line).replaceAll("<omitted>");
        line = USER_INFO_PATTERN.matcher(line).replaceAll("<omitted>");
        line = ACCOUNT_INFO_PATTERN.matcher(line).replaceAll("<omitted>");

        if (extraPattern != null) {
            line = extraPattern.matcher(line).replaceAll("<private info omitted>");
        }
        return line;
    }


    /**
     * @param input  the file to scrub
     * @param output the file to write results to
     */
    public static void scrubFile(Context context, File input, File output) throws IOException {
        long startScrubTime = System.currentTimeMillis();

        ArrayList<String> extraFilters = new ArrayList<String>();
        TelephonyManager tm = (TelephonyManager) context.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null && tm.getDeviceId() != null) {
            extraFilters.add(tm.getDeviceId());
        }
        extraFilters.add(getSerialNumber());
        String extraRegex = "";
        for (String regex : extraFilters) {
            extraRegex += regex + "|";
        }
        extraRegex = extraRegex.substring(0, extraRegex.length()-1);
        if (DEBUG) {
            Log.w(TAG, "extra regex: " + extraRegex);
        }
        Pattern privateInfoPattern = Pattern.compile(extraRegex);

        BufferedWriter bw = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(input));
            bw = new BufferedWriter(new FileWriter(output));

            if (DEBUG) Log.i(TAG, "starting task!");
            String line, scrubbedLine;
            while ((line = br.readLine()) != null) {
                scrubbedLine = ScrubberUtils.scrubLine(line, privateInfoPattern);
                if (!scrubbedLine.equals(line)) {
                    if (DEBUG) Log.d(TAG, "original line: " + line);
                    if (DEBUG) Log.w(TAG, "scrubbed line: " + scrubbedLine);
                }
                bw.write(scrubbedLine);
                bw.newLine();
            }
            bw.flush();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                // ignore
            }
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                // ignore
            }

            long endScrubTime = System.currentTimeMillis();
            if (DEBUG) Log.w(TAG, "scrubFile() took: " + (endScrubTime - startScrubTime) + "ms");
        }
    }

    private static String getSerialNumber() {
        // This section uses org.cyanogemod.hw, which is not included in this app
//        try {
//            if (SerialNumber.isSupported()) {
//                return SerialNumber.getSerialNumber();
//            }
//        } catch (NoClassDefFoundError e) {
//            // Hardware abstraction framework not installed; fall through
//        }

        return Build.SERIAL;
    }

}
