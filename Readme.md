# SysLog

This is a simple application that records various log types, and compresses them, currently as a tar file.  
The logs are saved under the primary external storage folders named by the date and time.  

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.tortel.syslog)
[<img src="https://f-droid.org/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.tortel.syslog/)

## Enabling Log Access via ADB (No root required)

To enable access to some logs (Logcat and Radio) to SysLog, you will need to grant the READ_LOGS permision using a computer with ADB.
ADB is part of the [Android platform-tools](https://developer.android.com/studio/releases/platform-tools), and you need to enable ADB
access on you device.

From a command line with adb available, you can grant the READ_LOGS permission via the following command:

```
adb shell pm grant com.tortel.syslog android.permission.READ_LOGS
```

This command tells the phone's package manager to grant the READ_LOGS permission to SysLog.

#### How exactly do I get adb working?

There is a short guide on the Android developer site [here](https://developer.android.com/studio/run/device).
More detailed guides can be found through a quick online search.

#### Do I need a computer to do this?

Yes. When you access a device over ADB, you have some privileged access. This can not be done on the device.

## Note about Root Access

Even with granting the READ_LOGS permission to the app via adb, it may still not be possible to get all logs without root access on Android 4.2 or higher.
The only logs that should work after granting permisssions are logcat and radio logs - the rest may still be restricted.
