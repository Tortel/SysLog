Version 2.5.0  - January 23, 2024
- Set up fastlane for publishing a new version
- Add a fastlane Android version plugin
- Working on updating the fastfile
- Setup Fastlane
- Build and dependency updates
- Changelog: Add missing changelog for 2.4.2
- fastlane: Setup fastlane for F-droid
- Update build tools and gradle
- Dependency updates
- Update build tools
- Make all dialogs use a Material theme, cleanup
- Update Material library and bump target SDK version
- Update some dependencies
- Some cleanup and error handling
- Move all the basic application dialog logic into a single class
- Update copyrights
- Move dialog strings to resources
- Use dynamic colors
- Tweak the vector drawables to be white so that tinting applies correctly
- Properly tint the actionbar items during live logcat
- Tweak the stlyes resources
- Remove -v23 resource files
- Bump min SDK to 23
- Bump gradle build tools version
- Updated status bar color in dark mode
- Added margin to scroll view
- Updated Layouts to Material You
- Enhanced switch case
- Upgraded dialogs to material you theme
- Initial Material You upgrade

v2.4.2 (Aug 25, 2023)
* Target Android 14
* Update dependencies and build tools

v2.4.1 (May 27, 2020)
* Updated FAQs and About text

v2.4.0 (May 27, 2020)
* Darker theme
* Change log storage to application's cache directory
* Remove no longer used permissions
* Logs collected can now be attached in other apps - select the SysLog app from the document picker to view collected log files

v2.3.0 (February 17, 2020)
* Dark theme
* Fixes for Android 10
* Improve error handling

v2.2.1 (October 13, 2019)
* Updated dependencies
* Updated to API 29 per Play Store guidelines

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
