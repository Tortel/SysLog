# Privacy Policy

No personal information is collected by SysLog.

The SysLog application itsself does not transmit any informaion of any kind, it only gathers logs and lets the user send them.

SysLog does collect system logs and prepares them for sending - be careful who you share the logs with, as they may contain
sensitive information. SysLog does have an option to remove some personal information from the logs, but it may not remove
everything.

## Why does SysLog need the permissions it requests?

#### Write External Storage

This allows SysLog to actually save the log files.
This permission is required for the app to function.

#### Read Logs

This is an outdated permission which is not even visible in newer versions of Android.
SysLog declares it because it does in fact read log files, and this permission is needed for
non-root usage on older versions of Android.

#### Phone State/ID

This permission is used to get the device's IMEI for scrubbing it from the logs.
It is only requested/used if the user enabled scrubbing.
