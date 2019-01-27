v2.2.0 (January 26, 2019)
* Updated dependencies
* Added instructions for rootless operation

v2.1.6 (August 30, 2018)
* Fix crashes while in landscape

v2.1.5 (July 18, 2018)
* Added support for grabbing some pstore log files

v2.1.4 (April 24, 2018)
* Added common log type names
* Adaptive icon

v2.1.1 (July 14, 2017)
* Fixed issues with Android 7

v2.1.0 (April 30, 2016)
* Fixed an issue while creating the log directories
* Re-wrote how logs are captured.
  * Instead of shell redirection, the output is run through Java to be saved
  * Changed how the log grabbing thread is started
  * The 'Root Path' is no longer needed
* The running dialog now displays what it is currently doing
* Re-wrote how the live logcat view gets the logcat text
  * It is significantly faster
  * Buffer issues have been resolved
  * Assorted reported crashes have been fixed

v2.0.2 (February 23, 2016)
* Working on live logcat crashes
* If it does crash when trying to open Live Logcat, please send the report. Thanks!

v2.0.1 (February 7, 2016)
* Removed colored logcat output - fixes the invalid -C option

v2.0.0 (February 6, 2016)
* New live logcat view - Now you can see you logs in near real time!
* Runtime permissions for Marshmallow
* Fixed issues with adopted storage on Marshmallow
* Fix for empty shared ZIP files (Thanks Luca Stefani!)
* Assorted bug fixes
