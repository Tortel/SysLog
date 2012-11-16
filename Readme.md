# SysLog

This is a simple application that records various log types, and compresses them, currently as a tar file.  
The logs are saved under the primary external storage folders named by the date and time.  

Some plans for improvement:
* Improve root-less operation. (Useless on 4.1+ without root, but I may start lowering the min API level and checking it down to 2.3)
* dmsg without root should work with pre-3.0 kernels, need to check it or always allow it
* Add last_kmsg support

Done:
* Readme/About dialog
* Switch to Zip compression
* Figure out when log files are done being written.

### Note about Root Access:
It may not be possible to get the logs without root access. To be investigated later.
