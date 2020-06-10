* _[New]_ binjr can now be used as a "portable" application on all supported platforms. 
  Portable apps can be unpacked to and used from a detachable drive or a file share.  
  Portable bundles are available in the following formats:
  * `tar.gz` for Linux 
  * `tar.gz` for macOS
  * `zip` for Windows
* _[New]_ Alternatively, it can be used as an "installable" application on all supported platforms. 
  Installable apps integrates with the host OS to provide menu shortcuts, file associations and per user settings.   
  Installers are available in the following formats:
  * `deb` for Debian & Ubuntu
  * `rpm` for RHEL, Centos & Fedora
  * `dmg` for macOS
  * `msi` for Windows

> **IMPORTANT NOTE**: When upgrading an existing copy of the Linux `tar.gz` distribution to version 2.16.0 or later, any previously set preferences will be reset, since it now defaults in "portable" mode and settings are stored directly into the application folder.  
You can override this behaviour by adding the command line option ` -Dbinjr.portable=false` when starting the application. You can also use the built-in settings import/export functions to migrate settings from one mode to another.