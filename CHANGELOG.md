## [binjr v3.22.0](https://github.com/binjr/binjr/releases/tag/v3.22.0)
Released on Wed, 23 Apr 2025

* _[New] [UI]_ There is now an option to specify whether a split worksheet pane should be automatically closed or left open when the last tab is contains is closed (`Settings > Close empty tab panes automatically`).
* _[Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 24.0.1.
* _[Fixed] [UI]_ The new tab `[+]` button on split and detached tab pane does not have a context menu for choosing the type of worksheet to create.
* _[Fixed] [JFR]_  The JFR adapter fails to parse recordings that contain custom event types and fields without a label.

## [binjr v3.21.1](https://github.com/binjr/binjr/releases/tag/v3.21.1)
Released on Wed, 29 Jan 2025

* _[New] [UI]_ It is now possible to split the visualisation area in the main window or in a detached tab, to display many worksheets side-by-side.
* _[New] [GC logs Adapter]_ Added a new data adaptor to plot metrics extracted from JVM GC logs.
* _[New] [HTTP Adapters]_ Http adaptors now support basic authentication.
* _[Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 23.0.2
* _[Fixed] [Log files Adapter]_ JVM unified logs parsing rules sometime fail to capture log severity.
* _[Fixed] [Packaging]_ Cannot install .deb on Debian bookworm (unmet dependencies for libffi)

## [binjr v3.20.1](https://github.com/binjr/binjr/releases/tag/v3.20.1)
Released on Sun, 10 Nov 2024

* _[Dependencies]_ Rolled back embedded OpenJDK runtime to 21.0.5
* _[Fixed]_ Bad performances when using the CSV and Logs adapters caused by a regression in OpenJDK 23.0.1 when using Shenandoah GC.
* _[Fixed]_ An error is raised in the installer when clicking on the 'back' button on the install verification dialog.

## [binjr v3.20.0](https://github.com/binjr/binjr/releases/tag/v3.20.0)
Released on Wed, 23 Oct 2024

* _[New] [UI]_ The number of ticks between two graduations the Y axis now automatically adapts to best fit based on the available space and unit types.
* _[New] [UI]_ Added a keyboard shortcut (F9) to reset the time range of a worksheet to its default value.
* _[New] [CSV Adapter]_ Doing a hard refresh (Ctrl+F5) on a worksheet now forces all data read form CSV sources to be reloaded from the underlying files.
* _[New] [CSV Adapter]_ Added an option to ignore lines with unparsable time stamps.
* _[New] [CSV Adapter]_ Better error message when failing to parse a time stamp (provides column and line numbers).
* _{Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 23.0.1
* _{Dependencies]_ Updated to Lucene 10.
* _[Fixed]_ "Unable to find valid certification path to requested target" error when trying to establish an HTTPS connection on macOS.
* _[Fixed]_ When dropping more than one node from the source tree view onto the legend pane of a worksheet, only the last node is added to the current chart.
* _[Fixed]_ User preference for "Treat undefined Y values as 0" is ignored.
* _[Fixed]_ The title bar for the binjr window is larger (or smaller) than it should be when using multiple monitors on Windows.
* _[Fixed]_ Closing and reopening the application causes the main window to slightly grow (or shrink) each time when using multiple monitors on Windows.

## [binjr v3.19.0](https://github.com/binjr/binjr/releases/tag/v3.19.0)
Released on Fri, 19 Jul 2024

* _[New]_ It is now possible for the user to configure a default value for the stroke width used in different types of charts.
* _[Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 22.0.2
* _[Fixed]_  Event heatmap in log worksheet shows wrong data when displaying events older than 1970/01/01 00:00:00 UTC.
* _[Fixed]_ Do not interpolate Y values at interval boundaries for scatter point charts.
* _[Fixed]_ Wrapping text in log view does not always work when selecting the option.

## [binjr v3.18.0](https://github.com/binjr/binjr/releases/tag/v3.18.0)
Released on Wed, 24 Apr 2024

* _[New]_ Added a new "System" UI theme that inherits the OS color scheme preferences.  
* _[Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 22.0.1  
* _[Dependencies]_ Migrated Wix toolset config to version 5.0.0  
* _[New]_ Added an option to force using the embedded JVM certificate store instead of the host's on Windows and macOS.  
* _[Fixed]_ Broken parsing profile for unified JVM logs containing only elapsed time.  
* _[Fixed]_ Notification popup does not show if the text it contains is too long.  


## [binjr v3.17.0](https://github.com/binjr/binjr/releases/tag/v3.17.0)
Released on Wed, 21 Mar 2024

* _[New]_ Support for new package managers: AUR for Archlinux and winget for Windows.  
* _[New]_ Added an option to override hardware acceleration support.  
* _[New]_ Added an option to change the user interface scaling factor.  
* _[New]_ Added an option to trim extraneous spaces in malformed CSV files in parsing profiles.  
* _[Dependencies]_ Updated the embedded runtimes for Java and JavaFX to version 22.  
* _[Fixed]_ Dead links for support pages in MSI installer metadata.

## [binjr v3.16.0](https://github.com/binjr/binjr/releases/tag/v3.16.0)
Released on Wed, 7 Feb 2024

* _[New]_ Added an option to allow Basic auth in tunneling over https.  
* _[Dependencies]_ Updated the embedded runtimes for Java and JavaFX to version 21.0.2
* _[Fixed]_ Source pane remains opened if a connection failed.  
* _[Fixed]_ Replace Apache http client by OpenJDK's built-in implementation.  
* _[Fixed]_ Use https instead of http when inferring missing protocol.

## [binjr v3.15.0](https://github.com/binjr/binjr/releases/tag/v3.15.0)
Released on Tue, 5 Dec 2023

* _[New]_ Added an option to ignore samples with an undefined Y value instead of forcing them to zero ("Settings > Charts > Treat undefined Y values as 0").  
* _[New]_ A  notification popup now shows download progress when the app is being updated.  
* _[Fixed]_ `NaN` values produce duplicated samples after Largest-Triangle-Three-Buckets algorithm is applied.  
* _[Fixed]_ Pagination mechanism when fetching data from index does not honor forceNanToZero property.  
* _[Fixed]_ A race condition in TimeSeriesProcessor.   
* _[Fixed]_ Continuously clicking on "Check for update" results in queuing as many download task.

## [binjr v3.14.0](https://github.com/binjr/binjr/releases/tag/v3.14.0)
Released on Mon, 30 Oct 2023

* _[Dependencies]_ Updated the embedded runtimes for Java and JavaFX to version 21.0.1  
* _[New]_ Source and target compatibility level for binjr's artifacts have been updated to 21  
* _[New]_ It is now possible to set the default values for chart type and unit prefixes, used when these aren't defined by the source  
* _[New]_ Enhanced JVM logging parsing profiles to accept ISO timestamps  
* _[New]_ Clicking on the find or filter button in a log worksheet now sets focus on the relevant input field

## [binjr v3.13.0](https://github.com/binjr/binjr/releases/tag/v3.13.0)
Released on Wed, 9 Aug 2023

* _[New]_ Added new JDK Flight Recorder data adapter.  
* _[New]_ Users can now choose a worksheet's type when creating a blank one.  
* _[New]_ Added new type of prefix formatting for charts axis: Percentage.  
* _[New]_ Added an icon representing the visualization type to   sources panes and worksheet tabs.  
* _[Fixed]_ When there are too many facet pills in an event worksheet, they now overflow to popup menu.  
* _[Fixed]_ The UI themes in the Settings panel are now listed in alphabetical order.  
* _[Fixed]_ LogWorksheetController instances cannot be loaded prior to an adapter acquiring Indexes.LOG_FILES  
* _[Fixed]_ UserInterfaceThemes services cannot be loaded from registered plugin path.

## [binjr v3.12.0](https://github.com/binjr/binjr/releases/tag/v3.12.0)
Released on Sat, 20 May 2023

* _[New]_ *binjr* now defaults to an indexing strategy for log files that is optimized for partial terms search and filtering. It allows for fast matching of arbitrary character sequences without the need for explicit syntax like wildcards.
> To revert back to the old behavior that favors searching for whole words, go to "Settings > Logs" and select "Optimize index for whole words search".
* _[New]_ Added the ability to open a single log file instead of a whole folder or a zip archive.  
* _[New]_ Inline help is now directly accessible for many options throughout the application's User Interface by clicking the `?`next to it.  
* _[New]_ Let users choose the date & time that serves as an anchor to construct timestamps for partial data.  
* _[New]_ Added an option for the user to toggle whether or not the Y axis should always include the origin (0) when auto-scale is enabled.  
* _[Dependencies]_ Updated embedded runtimes to OpenJDK 20.0.1 and OpenJFX 20.0.1  
* _[New]_ Added more built-in parsing profiles (Quarkus, Syslog).  
* _[Fixed]_ Removed unnecessary scoring computation in Log adapter queries to increase filtering performances.  
* _[Fixed]_ Settings panel is now wider and its content less cramped.  
* _[Fixed]_ Different outcome when typing the name of a capture group vs selecting it from the dropdown list in the  profile editor.  
* _[Fixed]_ Files selection on Linux do not show files with no extensions when "All files" filter is selected.  
* _[Fixed]_ Log file adapter does not list files without extensions.  
* _[Fixed]_ Log file view does not use a monotype font on macOS and Linux.  
* _[Fixed]_ Auto-update feature ignores alternative signing openPGP signature.

## [binjr v3.11.0](https://github.com/binjr/binjr/releases/tag/v3.11.0)
Released on Wed, 1 Feb 2023

* _[New]_ Added the possibility to parse months from their names (english only for now) in logs and CSV parsing rules.
* _[New]_ Added built-in parsing profile for IcedTea-Web log files.
* _[New]_ Application bundles are now built with, and embed, the Eclipse Temurin distribution of OpenJDK.
* _[Dependencies]_ Updated embedded runtimes to OpenJDK 19.0.2 and OpenJFX 19.0.2.1
* _[Fixed]_ Removed unused dependencies to gtk2 package in rpm build.
* _[Fixed]_ Rpm package cannot be built using rpm v4.16.0 or later.

## [binjr v3.10.0](https://github.com/binjr/binjr/releases/tag/v3.10.0)
Released on Thu, 6 Oct 2022

* _[New]_ It is now possible to adjust the size of the text in logs worksheets, as well as changing the default size in the preferences (`Settings > Logs > Default text size`).
* _[Dependencies]_ Updated Java and JavaFX Runtimes to version 19.
* _[Fixed]_ Text in debug console could be rendered using wrong encoding on some platforms.

## [binjr v3.9.0](https://github.com/binjr/binjr/releases/tag/v3.9.0)
Released on Thu, 1 Sep 2022

* _[New]_ The csv plugin has been entirely rewritten and now features:
  * A new, off heap, backend which allows for working with large quantities of data.
  * A much larger selection of user-configurable parameters for parsing CSV files.
  * A brand new User Interface to set these parameters and interactively test their effects on sample data.
  * The ability to organize, save and import user defined sets of parameters as "profiles" so that they can be reused and shared.
* _[New]_ It is now possible to cancel the loading of log files if it takes too long.
* _[Fixed]_ It is now possible to zoom in on a time interval shorter than one second on charts.
* _[Fixed]_ Resetting the time range on a chart worksheet no longer only takes the first chart into account.
* _[Fixed]_ The reference date for the predefined ranges on the time range picker is now based on the boundaries reported by the adapter.
* _[Fixed]_ Loading indicator causes high GPU usage.

## [binjr v3.8.0](https://github.com/binjr/binjr/releases/tag/v3.8.0)
Released on Wed, 18 May 2022

* _[New]_ It is now possible to reorder worksheet tabs using drag and drop.
* _[New]_ Use a more robust cache mechanism for data fetched from data adapters.
* _[New]_ It is now possible to override the root location for *binjr* temporary folders and files, by setting the `temporaryFilesRoot` property.

## [binjr v3.7.0](https://github.com/binjr/binjr/releases/tag/v3.7.0)
Released on Wed, 23 Mar 2022

* _[New]_ It is now possible to set distinct parsing rules for log files retrieved from a single source, and to swap or edit parsing profiles after a file was added to a worksheet.
* _[New]_ Enhanced snapshot feature to provide a preview of the snapped image and allows users to either save it to a file or to the clipboard.
* _[New]_ Added contextual menu entries for copying series details from tree view and table view.
* _[New]_ *binjr* can now be built to run natively on aarch64 architectures on Linux and macOS.
* _[Dependencies]_ Updated Java and JavaFX Runtimes to version 18.
* _[Fixed]_ Pressing the "delete" key while editing series name removes it from worksheet.
* _[Fixed]_ Leaves in source treeview are not sorted in alphabetical order.
* _[Fixed]_ A deadlock can occur if an error is raised while parsing log events.
* _[Fixed]_ It is not possible to browse a folder for log files if some of its children are not accessible to the current user (e.g. due to lack of permission for instance).
* 
## [binjr v3.6.0](https://github.com/binjr/binjr/releases/tag/v3.6.0)
Released on Fri, 21 Jan 2022

* _[New]_ Added support for proxy on all HTTP-based data adapters.
* _[New]_ It is now possible to use regular expressions when filtering the source tree view.
* _[New]_ Added a context menu to the list of series in a worksheet to expose edition features (select, delete, rename, etc...)
* _[New]_ Introduced features to automatically infer names and colors for multiple series in a worksheet.
* _[Dependencies]_ Updated the embedded OpenJDK and JavaFX runtimes to 17.0.2
* _[Fixed]_ A memory leak cause by a regression introduced in JavaFX 17.0.0
* _[Fixed]_ Improved performances and reduced memory usage when working with logs.
* _[Fixed]_ A regression introduced in v3.3.0 that caused logs with messages already displayed in debug console to not
  be displayed there again until console is cleared.
* _[Fixed]_ Selecting a series color using the "custom colors" panel from the color picker does not change the graph.

## [binjr v3.5.2](https://github.com/binjr/binjr/releases/tag/v3.5.2)
Released on Fri, 17 Dec 2021

* _[Dependencies]_ Updated Log4j to version 2.16.0 in response to [CVE-2021-45046](https://nvd.nist.gov/vuln/detail/CVE-2021-45046)

## [binjr v3.5.1](https://github.com/binjr/binjr/releases/tag/v3.5.1)
Released on Sat, 11 Dec 2021

* _[Dependencies]_ Updated Log4j to version 2.15.0 in response to [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)

## [binjr v3.5.0](https://github.com/binjr/binjr/releases/tag/v3.5.0)
Released on Thu, 25 Nov 2021

* _[New]_ Added a bar chart to log worksheets that shows the distributions over time of events' severity.
* _[New]_ The precision of the time axis for charts has been increased from seconds to milli-seconds.
* _[New]_ Added the option to skip the confirmation dialog on closing worksheet tabs or charts.
* _[New]_ Added the ability to restore closed tabs using the `Ctrl`+`Shift`+`t` shortcut, similarly to web browsers.
* _[New]_ Enhanced the pagination control on log worksheets with the ability to jump directly to the first, last or arbitrary page.
* _[Fixed]_ After duplicating a log worksheet, changing properties of the log files (in the bottom view) affects both the original and duplicated worksheet.
* _[Fixed]_ Navigating backward or forward on a log worksheet does not change the timeline of linked worksheets.

## [binjr v3.4.0](https://github.com/binjr/binjr/releases/tag/v3.4.0)
Released on Thu, 22 Oct 2021

* _[New]_ Added the ability to zoom and pan on charts using the mouse wheel (or swipe motions on touch devices):
  * `Shift + scroll up/down`: Enlarges / reduces the height of all charts in the current worksheet.
  * `Ctrl + scroll up/down`: Zooms in / out on the time range for the current worksheet.
  * `Alt + scroll up/dowm`: Pans all charts to the left / right in the current worksheet.
* _[New]_ It is now possible to adjust the minimum height of charts in a worksheet.
* _[Fixed]_ Series legend panel in chart worksheets does not fill all available space.

## [binjr v3.3.0](https://github.com/binjr/binjr/releases/tag/v3.3.0)
Released on Thu, 16 Sep 2021

* _[New]_ Updated the embedded runtimes for Java and JavaFX to version 17.
* _[New]_ Source and target compatibility level for binjr's artifacts have been updated to 17.
* _[New]_ Added the ability to search for and highlight keywords in the application's logs in the debug console.
* _[New]_ Changed the format of macOS installable from a `dmg` image to a `pkg` installer.
> The reason behind this change in format for the macOS deliverable is that unsigned DMG images produced by jpackage
> could be reported as "corrupted" under certain conditions instead of just warning users that the application was not
> recognized by the Apple notary service.  
> Bundles generated as PKG installers do not exhibit the same problem and correctly warns users that the application is
> not recognized by Apple (and not that the package is corrupted).

* _[Fixed]_ It is not possible to select and copy portions of the logs output in the debug console.
* _[Fixed]_ The button to empty the search text field in log work worksheets is displayed even if the field is already empty.
* _[Fixed]_ Clean-up phase could sometime be skipped on closing worksheets, leading to potential memory leaks.
* _[Fixed]_ Inconsistent content type checking when processing http responses from data adapters.


## [binjr v3.2.0](https://github.com/binjr/binjr/releases/tag/v3.2.0)
Released on Thu, 12 August 2021

* _[New]_ Keep the most recently used log filters in the user history and allow users to save filters as favorites.
* _[Fixed]_ Freeze when clicking on `Browse` to open a file or folder on Linux.
* _[Fixed]_ The entered URL may be ignored when pressing "Enter" in the new source dialog.
* _[Fixed]_ An error occurs when accepting an empty path in the RRD and CSV adapters dialogs.

## [binjr v3.1.0](https://github.com/binjr/binjr/releases/tag/v3.1.0)
Released on Wed, 30 July 2021

* _[New]_ It is now possible to select multiple charts in a worksheet to delete them all at once.
* _[New]_ It is now possible to re-organize the position of charts in a worksheet using drag and drop.
* _[New]_ The panel that shows the series details for charts has been redesigned to make better use of available screen real estate.
* _[New]_ It is now possible to edit the name of a timeseries after it has been added to a worksheet.

## [binjr v3.0.2](https://github.com/binjr/binjr/releases/tag/v3.0.2)
Released on Fri, 30 Apr 2021

* _[Fixed]_ Application fails to start on Windows if Visual Studio 2019 Redistributable is not installed.

## [binjr v3.0.1](https://github.com/binjr/binjr/releases/tag/v3.0.1)
Released on Tue, 6 Apr 2021

* _[Fixed]_ Publication to APT and RPM repositories on new releases is broken.
* _[Fixed]_ Incorrect progression reporting when indexing more than one log file simultaneously.

## [binjr v3.0.0](https://github.com/binjr/binjr/releases/tag/v3.0.0)
Released on Wed, 31 Mar 2021

* _[Breaking Change]_ The plugin API for *binjr* v3.0.0 is no longer compatible with previous versions.
* _[New]_ *binjr* is now able to handle and render time series with data types other than numerical values.
* _[New]_ *binjr* can now extract timeseries data from log files to navigate and filter through log events , in sync with other sources.
* _[New]_ *binjr* can now be run under the Eclipse OpenJ9 JVM
* _[New]_ Relative presets in the time range selection panel.
* _[New]_ Users no longer have to input a minimum of 3 characters in the source filtering bar to trigger filtering.
* _[New]_ Added a new PERF log level in between INFO and DEBUG.
* _[Dependencies]_ Embedded Java runtime updated to OpenJDK 16 and OpenJFX 16.
* _[New]_ Added a "Reset Time Range" button to TimeRangePicker control.
* _[New]_ Added new keyboard shortcuts to close a worksheet and navigate history.
* _[New]_ Windows installer allows overriding existing installation path via an MSI property.
* _[New]_ Added the option to display numerical values on charts without unit prefixes.
* _[Change]_ Icons and labels for switching to/from 'Edit' and 'Presentation' mode changed to 'Expand/Reduce Series Views'
* _[Fixed]_ If an error occurs while loading an adapter, all subsequent adapter aren't loaded.
* _[Fixed]_ A sharp performance drop when zooming extremely close up on the time axis (i.e. displaying less than a few seconds)
* _[Fixed]_ Removed unused time zone selection field on Netdata adapter dialog.
* _[Fixed]_ Snapshots taken with the default output scaling use the main monitor scaling factor instead of the one on which the window is displayed.
* _[Fixed]_ Error occurring while fetching data from a single adapter prevents plotting the data recovered from other adapters.
* _[Fixed]_ Modified "New Tab" and "Save As" keyboard shortcuts to be more consistent with well known applications.
* _[Fixed]_ Clicking on an expended source tab's title does not cause it to collapse its contents.
* _[Fixed]_ Pressing `enter` or loosing focus from text entry field when editing source tab title does not validate entry.
* _[Fixed]_ Charts are blurry when binjr is displayed on a screen with a 125%, 150% or 175% scale ratio.
* _[Fixed]_ A concurrent modification exception when applying sampling reduction pre-processing on series.
* _[Fixed]_ Changes to Y axis scale in chart properties are not taken into account by navigation history.

## [binjr v2.17.0](https://github.com/binjr/binjr/releases/tag/v2.17.0)
Released on Thu, 02 Jul 2020

* _[Fixed]_ Jitter on the y-axis when hovering over charts with full height crosshair.
* _[Fixed]_ Incorrect capitalization on some menu entries and labels.

## [binjr v2.16.0](https://github.com/binjr/binjr/releases/tag/v2.16.0)
Released on Wed, 10 Jun 2020

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

## [binjr v2.15.0](https://github.com/binjr/binjr/releases/tag/v2.15.0)
Released on Tue, 12 May 2020

* _[New]_ A new adapter allows to use Netdata (https://netdata.cloud) servers as data sources.  
* _[New]_ Users can now choose which default color palette to use for charts when the color isn't specified by the source. 
* _[New]_ "Show outline" and "Default opacity" preferences are now settable separately for "area charts" and "stacked area" charts.
* _[Dependencies]_ Updated the embedded runtime to OpenJDK 14.0.1 and OpenJFX 14.0.1
* _[Fixed]_ JRDS adapter incorrectly reports all charts as stacked area charts.
* _[Fixed]_ "Show outline on area charts " user preference is not persisted across sessions. 
* _[Fixed]_ A concurrency issue causes an ArrayIndexOutOfBoundsException when applying sample reduction transform. 
* _[Fixed]_ The time range picker is not dismissed automatically after the user selects a preset range.

## [binjr v2.14.0](https://github.com/binjr/binjr/releases/tag/v2.14.0)
Released on Thu, 19 Mar 2020

* _[Dependencies]_ Updated the embedded runtime to OpenJDK 14 and OpenJFX 14.
* _[New]_ Linux version no longer depends on GTK 2.
* _[Fixed]_ "Unrecognized image loader:null" error occurs when attempting to capture snapshots of worksheet with many a large number of charts.

## [binjr v2.13.0](https://github.com/binjr/binjr/releases/tag/v2.13.0)
Released on Thu, 30 Jan 2020

* _[New]_ Enhanced downsampling algorithm; this allows a more faithful visual representation of series while still dramatically reducing the number of plotted samples.
* _[Dependencies]_ Updated the embedded Java and JavaFX runtimes to 13.0.2
* _[New]_ Changed default value for max heap size to 4GB
* _[Fixed]_ Last and first samples for the selected time range are ignored when rendering data from CSV adapter.
* _[Fixed]_ Time range label on screenshots is incorrect.
* _[Fixed]_ Charts on scaled up screenshots taken in "Presentation" mode are blurry.
* _[Fixed]_ Changed the icon for switching to "Edit" mode to a pen as the previously used cog was confusing.

## [binjr v2.12.0](https://github.com/binjr/binjr/releases/tag/v2.12.0)
Released on Fri, 20 Dec 2019

* _[New]_ Automatically adjusts the time range up when dropping series on an existing worksheets, provided no series where already present.
* _[New]_ The macOS application bundle is now available as a DMG image. This allows for better integration with the menu bar and to register workspace file extention. 
* _[Fixed]_ The JRDS data adapter does not check the content type before attempting tp parse an http response payload as JSON.
* _[Fixed]_ The CSV Data Adapter cannot deal with columns having the same name in a single file.

## [binjr v2.11.0](https://github.com/binjr/binjr/releases/tag/v2.11.0)
Released on Thu, 25 Nov 2019

* _[Dependencies]_ Updated the embedded Java runtime to OpenJDK 13.0.1
* _[New]_ It is now possible to choose the output scale (i.e. the physical pixel density) for snapshots taken from binjr worksheets.
* _[Fixed]_ On HiDPI screens the tooltip representing a tree node when dragging it to a worksheet is not at the right scale.  

## [binjr v2.10.0](https://github.com/binjr/binjr/releases/tag/v2.10.0)
Released on Thu, 24 Oct 2019

* _[New]_ New Adapter API method to center worksheets' time interval to be most relevant with regard to sources
* _[New]_ Application logs are now written to disk by default (in temp directory, 1 file per session, only keeps the last 10 files)
* _[Fixed]_ Concurrent modifications to output console's log queue.
* _[Fixed]_ File or folder chooser dialog does not appear when last opened path is invalid.

## [binjr v2.9.0](https://github.com/binjr/binjr/releases/tag/v2.9.0)
Released on Fri, 27 Sep 2019

* _[New]_ Added options to import and export user preferences, as well as clear opened files history.
* _[Dependencies]_ Update bundled OpenJavaFX to version 13.
* _[Fixed]_ NPE in JrdsDataAdapter when the adapter is loaded from saved workspace.
* _[Fixed]_ CsvDataAdapter ignores some configuration keys when loaded from saved workspace.
* _[Fixed]_ Fetching data via an adapter may fail silently.
* _[Fixed]_ Charts do no honor the exact time range specified by the user.
* _[Fixed]_ An offset on the time axis between two or more charts may occur if the sources for them have different resolutions.
* _[Fixed]_ UI themes defined in external plugins aren't loaded if set as the current theme when binjr is started.
* _[Fixed]_ Unexpected cache miss in http data adapters.
* _[Fixed]_ Disabling a DataAdapter in the settings section doesn't prevent it from being present in "Sources > New Sources..." menu.
* _[Fixed]_ Enabled DataAdapter settings are not persisted in between sessions 

## [binjr v2.8.1](https://github.com/binjr/binjr/releases/tag/v2.8.1)
Released on Wed, 04 Sep 2019

* _[Fixed]_ Memory leak: a closed worksheet controller remains reachable if an error notification popup is displayed and user preference _"Discard notification after:"_ is set to _"Never"_.


## [binjr v2.8.0](https://github.com/binjr/binjr/releases/tag/v2.8.0)
Released on Tue, 03 Sep 2019

* _[New]_ Now supports the addition of custom UI themes via external plugins.
* _[New]_ Accepts '.xml' as a valid extension for saved workspaces, in addition to '.bjr'
* _[Fixed]_ A regression introduced in 2.7.0 which prevents access to OS specific certificate stores for SSL validation (Windows / macOS)

## [binjr v2.7.0](https://github.com/binjr/binjr/releases/tag/v2.7.0)
Released on Sun, 18 Aug 2019

* _[New]_ Fetching data for a single chart but from different paths is now done concurrently on multiple threads.
* _[New]_ Added support for adapter that don't need a setup dialog box.
* _[Fixed]_ Worksheet masker pane is dismissed before all charts have been refreshed.
* _[Fixed]_ Concurrent modification of TextFlow control in OutputConsole throws an exception.
* _[Fixed]_ Spurious warnings about cookies invalid expires attributes. 
* _[Fixed]_ CsvDecoder should not be re-instantiated each time it is called.

## [binjr v2.6.3](https://github.com/binjr/binjr/releases/tag/v2.6.3)
Released on Wed, 07 Aug 2019

* _[Fixed]_ Prevent update check from proposing to download and install an update on macOS, as in-application installation does not work on this platform at the moment.
* _[Fixed]_ Stop deploying all platform specific resources across all platform application bundles. 

## [binjr v2.6.0](https://github.com/binjr/binjr/releases/tag/v2.6.0)
Released on Mon, 05 Aug 2019

* _[New]_ Ability to drag branches with many sub levels from the tree and have them rendered as separate charts on a worksheet.
* _[New]_ Better visual feedback when hovering above a worksheet during a drag and drop operation.
* _[New]_ Ability to select multiple nodes from the source tree to drag onto a worksheet.
* _[New]_ Added a filter functionality to the source tree.
* _[New]_ It is now possible to remove a chart or invoke its property page directly from buttons located on top of the chart's Y axis. 
* _[New]_ The position and size of the various resizable panes in the UI are now saved alongside the rest of a workspace, so that its appearance can be fully restored on reload.
* _[New]_ Charts legends pane in edit mode can now be scrolled up and down when many charts are added to a single worksheet. 
* _[Fixed]_ Vertical scrollbar on chart view in stacked layout hides part of the graph and causes an horizontal scrollbar to appear.
* _[Fixed]_ The "path" column in the chart legend table doesn't fill up the pane. 

## [binjr v2.5.0](https://github.com/binjr/binjr/releases/tag/v2.5.0)
Released on Wed, 06 Jul 2019

* _[New]_ Updates can now be downloaded and applied from within the application.
* _[New]_ binjr now remembers its main window's screen position in-between sessions.
* _[New]_ Added a new "Presentation Mode" that maximize the amount of space dedicated to the visualization of charts by hiding the source pane, chart settings pane and displays the chart legends in a condensed view.
* _[New]_ The snapshot functionality has been enhanced to automatically take a snapshot of the whole charts display area of a worksheet, even if this area requires scrolling when displayed in the application. 
* _[Dependencies]_ Embedded OpenJDK in application bundle has been updated to version 12.
* _[New]_ Defaults to the new Shenandoah garbage collector with the "compact" heuristics, which allows for a larger maximum heap size while keeping actual memory usage reasonable when a large heap is no longer required.   
* _[New]_ Warn end-users when trying to add a large number of series to a single chart at once.
* _[New]_ History of previously opened sources is now accessible via a combo box on the selection dialog (as well as through the existing auto-completion feature).
* _[New]_ Charts vertical axis label are now hilited on mouse-over, to better indicate that they are clickable (clicking on an axis selects the chart as the one currently editable when more than one chart are present on a worksheet).
* _[Fixed]_ Unsightly UI theme application on start-up or when detaching tabs.
* _[Fixed]_ If "Span crosshair over all charts" is true and "auto scale Y axis" is off, then selecting a new time range using the mouse results in incorrectly changing the Y axis scale.
* _[Fixed]_ Selecting a timezone in time picker sometime doesn't register.
* _[Fixed]_ Synchronizing timelines across worksheets is broken.
* _[Fixed]_ UI becomes unresponsive when output console displays  a large number of lines (>20000).
* _[Fixed]_ Check for a new versions fails due to Github API rate limit being reached.

## [binjr v2.4.1](https://github.com/binjr/binjr/releases/tag/v2.4.1)
Released on Mon, 08 Apr 2019

* _[Fixed]_ The application becomes unresponsive and crashes with an out-of-memory error if it gets overflown with user requests (e.g. continuous clicks on refresh or back/forward buttons).
* _[Fixed]_ NPE when drag-and-dropping a folded tree.
* _[Fixed]_ Tooltips are not styled according to the selected UI theme.
* _[Fixed]_ Chart properties slide pane should not obscure charting area.
* _[Fixed]_ Stroke width slider is grayed out for line and scatter point charts.
* _[Fixed]_ Suggest popup on data adapter dialog doesn't adapt to longer URLs/paths.

## [binjr v2.4.0](https://github.com/binjr/binjr/releases/tag/v2.4.0)
Released on Fri, 29 Mar 2019

* ___[API Change]___ Removed type parameters from the following classes from the Data Adapter API:
    * `DataAdapter<T>` is replaced by `DataAdapter`
    * `BaseDataAdapter<T>` is replaced by `BaseDataAdapter`
    * `HttpDataAdapter<T, A extends Decoder>` is replaced by `HttpDataAdapter`
    * `Decoder<T>` is replaced by `Decoder`
    * `TimeSeriesBinding<T>` is replaced by `TimeSeriesBinding`
    * `TimeSeriesInfo<T>` is replaced by `TimeSeriesInfo`
    * `TimeSeriesProcessor<T>` is replaced by `TimeSeriesProcessor`
* _[New]_ Added a "Settings" button to source panes. 
* _[New]_ Added an option to hide the source pane (Command bar menu "Sources > Hide Source Pane" or Ctrl+L)
* _[New]_ Added an option to hide charts legend (Worksheet toolbar "Hide Charts Legends" or Ctrl+B)
* _[New]_ Added an option to span the vertical bar of the selection crosshair over the height of all charts in a
worksheets using a stacked layout (Command bar "Settings > Charts > Hide Source Pane > Span crosshair over all charts")
* _[New]_ Buttons in a worksheet's toolbar will now overflow to a menu pane if there is not enough space to display all
of them all at once.
* _[New]_ A visual indication now identifies the currently selected charts on worksheets when there are more than one.
* _[New]_ Clicking on a chart's title in the graphing area new selects it and expands its legend in the bottom pane.  
* _[New]_ Added support for small numbers unit prefix (m = milli, µ = micro, n = nano, etc...) for formatting Y axis values.
* _[New]_ Added confirmation dialog when closing one or several worksheet tabs.  
* _[Fixed]_ A memory leak that occurs when adding, moving or changing the type of a chart in an existing worksheet.
* _[Fixed]_ Uncaught exception when entering a negative range for a chart's Y axis causes a worksheet to become.
* _[Fixed]_ Keyboard shortcuts do no work on detached tab windows.
* _[Fixed]_ Dialog boxes are sometime drawn with a null width and height on some Linux/KDE platforms.

## [binjr v2.3.1](https://github.com/binjr/binjr/releases/tag/v2.3.1)
Released on Mon, 11 Feb 2019

* _[Fixed]_ Debug console appender is initialized only when console is displayed for the first time.
* _[Fixed]_ Log level changes when entering or leaving debug console.
* _[Fixed]_ Date formatting does not use the system locale.
* _[Fixed]_ Wrong tooltip for time range picker on worksheet.

## [binjr v2.3.0](https://github.com/binjr/binjr/releases/tag/v2.3.0)
Released on Mon, 28 Jan 2019

* _[Fixed]_ Dropping series onto worksheets in main view fail after a detached tab window was closed.
* _[Fixed]_ Scatter charts are drawn using default colours instead of the colours defined in the source.
* _[Fixed]_ binjr fails to start when double-cliking on the launcher script on Linux or macOS.
* _[Fixed]_ DataAdapter plugins do not get loaded when starting binjr from exec-maven-plugin or Graviton.
* _[Fixed]_ JavaFX crashes on focus loss from dialog on macOS 10.14 Mojave.
* _[Fixed]_ Trying to establish a connection via HTTPS fails with " Received fatal alert: handshake_failure". 
* _[Fixed]_ DataAdapter never cleans up its resources if fails when populating source tree view.

## [binjr v2.2.1](https://github.com/binjr/binjr/releases/tag/v2.2.1)
Released on Fri, 11 Jan 2019

* _[New]_ Enhancements to debug mode console
* _[New]_ Added changelog to distribution
* _[Fixed]_ Disabled the forced sync mechanism for Rrd4J NIO backend.

## [binjr v2.2.0](https://github.com/binjr/binjr/releases/tag/v2.2.0)
Released on Sat, 5 Jan 2019

> **Please note**: Starting with this release, the Maven groupID for all the binjr artifacts changes from `eu.fthevenet` to `eu.binjr`

* _[Change]_ The keyboard shortcut to invoke debug mode changed from CRTL+SHIFT+D to F12.
* _[FIxed]_ The debug output console's log view perpetually grows.

## [binjr v2.1.1](https://github.com/binjr/binjr/releases/tag/v2.1.1)
Released on Fri, 28 Dec 2018

* _[New]_ It is now possible to open binary files and XML dumps created with [RrdTool](https://oss.oetiker.ch/rrdtool/) using the Rrd4j data adapter.  
  __NB:__ This uses Rrd4j 's built-in conversion facilities in order to import the original file's data into a temporary Rrd4j backend, so be aware that opening a very large rrd file (or a very large number of smaller ones) may be slower than expected, due to the necessary conversion process.
* _[Fixed]_ The tree hierarchy for series bindings created with the Rrd4j adapter is incorrect or incomplete

## [binjr v2.1.0](https://github.com/binjr/binjr/releases/tag/v2.1.0)
Released on Thu, 20 Dec 2018

* _[New]_ Added a new data adapter to directly open and plot the content of rrd db files produced by [RRd4j](https://github.com/rrd4j/rrd4j)
* _[New]_ Added a context menu accessible when right-clicking on the tab that provide shortcuts to various manipulations of the tabs (close, edit, duplicate and detach).

## [binjr v2.0.0](https://github.com/binjr/binjr/releases/tag/v2.0.0)
Released on Mon, 26 Nov 2018


>Starting with version 2.0.0, binjr is built to run on Java 11 and beyond. 
>
>___Please note that it does not run on previous version of Java.___
>
> If you require a version that runs on Java 8, you can use the latest releases versioned 1.x.x.

* _[New]_ Built to run on Java 11 and beyond, and use the new standalone distribution of OpenJFX (https://openjfx.io/)
The platform specific packages above contain all required dependencies, including the Java runtime; simply download the one for your OS, unpack it and run "binjr" to start.
* _[New]_ It is now possible to link the timeline of two or more independent worksheets (i.e. change the time range on one worksheet also affect all linked worksheets).
* _[New]_ It is now possible to copy/paste a time range from one worksheet to another.
* _[New]_ Removed the modal dialog shown when adding a new worksheet; instead new worksheet are set to editable mode upon creation.
* _[Fixed]_ Clicking the "OK" button on new source dialog has no effect when the source adapter is loaded from a faulty plugin.

## [binjr v1.6.0](https://github.com/binjr/binjr/releases/tag/v1.6.0)
Released on Wed, 31 Oct 2018

* _[New]_ Greatly enhanced time range selection on worksheets.
* _[New]_ Changes to source navigation panel's interface to make it clearer.
* _[New]_  When a source connection is closed,  all associated series on worksheets  are now removed.
* _[New]_ Many minor tweaks and fixes to UI themes.
* _[Fixed]_ Trailing slash in urls prevent connection to JRDS and other http sources.
* _[Fixed]_ An NPE could occur when closing a source with no worksheet.

## [binjr v1.5.3](https://github.com/binjr/binjr/releases/tag/v1.5.3)
Released on Thu, 11 Oct 2018

* _[Fixed]_ Resources from a DataAdapter are not disposed when a source tab is closed.
* _[Fixed]_ Console output window doesn't always acknowledge appearance changes.

## [binjr v1.5.2](https://github.com/binjr/binjr/releases/tag/v1.5.2)
Released on Fri, 5 Oct 2018

* _[New]_ Report the use of an unsupported version of Java 
* _[Fixed]_ Detection of missing JavaFX is broken
* _[Fixed]_ Spurious warning messages because of unset variables.

## [binjr v1.5.1](https://github.com/binjr/binjr/releases/tag/v1.5.1)
Released on Wed, 3 Oct 2018

* _[New]_ User can invoke a console that display log output an d change logging verbosity at runtime.
* _[Fixed]_ File picker dialog box doesn't show if last saved folder is invalid.

## [binjr v1.5.0](https://github.com/binjr/binjr/releases/tag/v1.5.0)
Released on Wed, 19 Sep 2018

* _[New]_ Added a "Dark" UI theme. "Modern" UI theme has been renamed "Light", while "Classic" is unchanged.
* _[New]_ Added the possibility to display debug menu and increase log verbosity at runtime.
* _[Fixed]_ JRDS adapter fails to connect to source if a url contains a trailing slash.
* _[Fixed]_ NPE when initiating a drag & drop motion on an empty tab pane.
* _[Fixed]_ Application cannot start if the UI theme name stored in user preference is not valid.
* _[Fixed]_ The labesl on command bar items sometimes remains visible when the command bar is reduced.

## [binjr v1.4.3](https://github.com/binjr/binjr/releases/tag/v1.4.3)
Released on Mon, 10 Sep 2018

* _[Fixed]_ Built-in DataAdapter are not loaded if an error occurs while scanning the plugin location at startup.
* _[Fixed]_ binjr takes a long time to start because scanning for DataAdapter at visits all sub-folders with maximum depth in plugin location.
* _[Fixed]_ DirectoryChooser dialog doesn't show up if current plugin location if invalid/not a folder

## [binjr v1.4.1](https://github.com/binjr/binjr/releases/tag/v1.4.1)
Released on Tue, 4 Sep 2018

* _[New]_  The duration after which popups automatically fade away can now be configured.
* _[New]_  Relaxed the parsing of URLs when adding a new source (infers a default protocol and port if omitted)
* _[Fixed]_ Failing when a malformed URL is entered for a new JRDS source does not offer a useful error notification.
* _[Fixed]_ Chart background is gray when multiple chart are displayed in stacked view mode but white when overlaid.

## [binjr v1.4.0](https://github.com/binjr/binjr/releases/tag/v1.4.0)
Released on Thu, 2 Aug 2018

* _[New]_ binjr's functionalities can now be extended through the use of plugins. 
For the time being, plugins can be used to implement new data source adapters, in order to make binjr capable to communicate with other source systems without the need to change anything to the core module itself.
* _[New]_ The artifact for the core binjr module, which is the sole dependency for building external plugins, is now available via [Maven Central](https://search.maven.org/%23artifactdetails%7Ceu.fthevenet%7Cbinjr%7C1.4.0%7Cjar).


## [binjr v1.3.4](https://github.com/binjr/binjr/releases/tag/v1.3.4)
Released on Wed, 27 Jun 2018

* _[New]_ Performs a sanity check when loading workspaces from files to verify format version number and alert user with a clear error message if it is incompatible.
* _[New]_ Added the option to choose the layout of multiple charts on a single worksheet, either stacked on top of each other, or as an overlay, sharing the same X axis.
* _[Fixed]_ Charts rendering performances greatly improved when visualizing many charts on a single worksheet.
* _[Fixed]_ Deselecting all time series in the main chart in an overlay view would make times series in other chart disappear.

## [binjr v1.3.0](https://github.com/binjr/binjr/releases/tag/v1.3.0)
Released on Mon, 18 Jun 2018

* _[New]_  It is now possible to more than one chart representation to a single worksheet. All charts have independent Y axis, with their own scale and unit, but share the same X axis (which represent time).
Charts on a single worksheet can be of different types (line, area, scatter points, etc...)
The User Interface has been extended to cater for that new core functionality:
  - Y axis boundaries are new settable on a per-chart basis (rather than a per- worksheet).
  - Time series bindings can now be dragged and dropped onto any existing charts, a new chart or a new worksheet.
  - Chart titles, unit names and unit prefixes can now be changed after a worksheet/chart has been created.
  - Time series in a chart can be selected/deselected all at once.

* _[Breaking Change]_  The file format used to persist workspaces had to be changed significantly in order to accommodate features introduced in this release and is no longer compatible with the format used in versions prior to 1.3.0.
Note that neither ascending nor descending compatibility is provided; files created in binjr v1.3.0+ cannot be loaded in older versions and files created in older versions cannot be loaded by binjr v1.3.0+.



## [binjr v1.2.3](https://github.com/binjr/binjr/releases/tag/v1.2.3)
Released on Mon, 19 Feb 2018

* _[New]_ Added support for CSV formatted files to be used as data sources.
* _[New]_ Added support for scatter plot charts.
* _[New]_ Added an option to reset all user settings their to default value.
* _[New]_ It is now possible to modify the timezone for a worksheet after is has been created (option in chart settings panel)
* _[Fixed]_  application closing even if "cancel" is selected on save confirmation dialog
* _[Fixed]_  Minor cosmetic fixes and enhancements (Cross-hair no longer appear in front of chart settings panel, message dialogs use vector graphic icons, spelling in messages and logs, etc...)
* _[Fixed]_  Unhandled exceptions when validating inputs in JRDS source dialog box could make the dialog box not acknowledge user clicking the OK button.
* _[Fixed]_  Missing security policies from embedded JRE in Windows native bundle
* _[Fixed]_  Application fail to start with "Could not create jvm" when using  Windows native bundle on a machine without a copy of MSVS C++ runtime redistributable installed.
* _[Fixed]_ Underscores in recently open file names menu are sometime removed.
* _[Fixed]_ Many long standing issues with timezone management.

## [binjr v1.1.0](https://github.com/binjr/binjr/releases/tag/v1.1.0)
Released on Fri, 29 Sep 2017

* _[New]_  Worksheet tabs can now be detached from the main window via a simple drag and drop (similar to a web browser).
* _[New]_ Native platform bundles available for Windows (.msi), MacOS (.dmg) and Linux (.rpm and .deb) 
These are platform specific install packages that contain a minimal and independent Java Runtime Environment and executable bootstrap, allowing binjr to run as a stand-alone application.
* _[New]_ binjr workspace files can be associated with the application so that binjr is launched on double clicking a  .bjr file. This association is automatically performed by the aforementioned native bundles.
* _[New]_  On Windows and MacOS,  root CA certificates stored in the OS keystore are used for SSL validation.
* _[Fixed]_  The modal dialog used for user authentication could appear behind the main stage, hence causing the application to appear frozen.
* _[Fixed]_  A possible Null Pointer Exception when using the source/search feature.
* _[Fixed]_  Aligned button background color in Modern theme with Windows 10 standard controls

## [binjr v1.0.15](https://github.com/binjr/binjr/releases/tag/v1.0.15)
Released on Mon, 24 Jul 2017

* _[New]_ Dialog boxes now support UI Theme
* _[Fixed]_ Wrong style applied to button in date picker control
* _[Fixed]_ Changing series visibility doesn't work if chart type is changed.
* _[Fixed]_  Better exception handling in JRDS dataAdapter: error message displayed to end users should be more relevant and helpful in common error scenario.


## [binjr v1.0.13](https://github.com/binjr/binjr/releases/tag/v1.0.13)
Released on Mon, 26 Jun 2017

* _[New]_ It is now possible to change the type of chart used on worksheet after it's been created.
* _[New]_ User can now set the stroke width on line chart and area charts with an outline.
* _[Fixed]_ Line charts ignore colors set in source.
* _[Fixed]_ A slowdown on plotting large series was introduced in release 1.0.12.

## [binjr v1.0.12](https://github.com/binjr/binjr/releases/tag/v1.0.12)
Released on Thu, 22 Jun 2017

* _[New]_ Displays the value of each series for the instant marked by the current position of the vertical marker.
* _[Fixed]_ Series info in table view aren't refreshed properly when time interval changes.
* _[Fixed]_ Removed obsolete parameters from settings panel.

## [binjr v1.0.11](https://github.com/binjr/binjr/releases/tag/v1.0.11)
Released on Tue, 13 Jun 2017

* _[New]_ Using the "Refresh" button now ignores any previously cached data.
* _[Fixed]_ Sorting JRDS treeview by "All filters" or "All tags" is broken
* _[Fixed]_ Application appears to hang when attempting to close it while it is minimized

## [binjr v1.0.10](https://github.com/binjr/binjr/releases/tag/v1.0.10)
Released on Fri, 9 Jun 2017

* _[Fixed]_  Application doesn't provide a clear reason for not starting when JavaFX runtime is not present.

## [binjr v1.0.9](https://github.com/binjr/binjr/releases/tag/v1.0.9)
Released on Wed, 7 Jun 2017

* _[New]_ Search bar to quickly find items in source tree view.
* _[New]_ Better support for JRDS tree view filters.
* _[Fixed]_ The text in "license" and "acknowledgement" panes in about box is blurry.

## [binjr v1.0.8](https://github.com/binjr/binjr/releases/tag/v1.0.8)
Released on Thu, 1 Jun 2017

* _[Fixed]_ Changes to chart appearance settings (outline, area opacity, etc...) are ignored on area charts.

## [binjr v1.0.7](https://github.com/binjr/binjr/releases/tag/v1.0.7)
Released on Wed, 31 May 2017

* _[New]_ JRDS SourceAdapter now supports authenticating through Kerberos
* _[Fixed]_ Dragged tree node would keep following the mouse pointer after being drop onto a worksheet on Linux
* _[Fixed]_ The landing zone for dropping sources onto empty worksheet pane is now much larger
* _[Fixed]_ An invalid cast exception occurs when rendering line charts.

## [binjr v1.0.6](https://github.com/binjr/binjr/releases/tag/v1.0.6)
Released on Tue, 23 May 2017

- *[Fixed]* JVM does not terminates on its own after the main window is closed.

## [binjr v1.0.5](https://github.com/binjr/binjr/releases/tag/v1.0.5)
Released on Tue, 23 May 2017

- *[New]* Long running tasks, such as loading a workpace or fetching time-series data from sources, are now executed asynchronously to the UI refresh. This increases the global responsiveness of the application and prevents most occurrences of the applications "freezing" for a few seconds during those tasks.
- *[New]* Errors when connecting to a source or parsing a workspace file are now reported as modeless notification popups rather than modal dialog boxes.
- *[New]* The behaviour of the auto-ranging feature for the Y axis has changed; it is now a toggle button, rather than a push button that would reset the range.
- *[Fixed]* An bug in DecimationTransform, causes a "java.lang.IllegalArgumentException: Duplicate data added" exceptions.

## [binjr v1.0.4](https://github.com/binjr/binjr/releases/tag/v1.0.4)
Released on Thu, 18 May 2017

- *[New]* Use drag and drop to add series sources to the current or a new worksheet.

## [binjr v1.0.3](https://github.com/binjr/binjr/releases/tag/v1.0.3)
Released on Wed, 17 May 2017

- *[Fixed]* Pressing 'del' to remove a series from a worksheet also removed all subsequent series in table view.

## [binjr v1.0.2](https://github.com/binjr/binjr/releases/tag/v1.0.2)
Released on Tue, 16 May 2017

- *[New]* Greatly enhanced responsiveness when working with series with large number of samples.
- *[New]* Reworked the UI to display settings and preferences via sliding panes rather than dialog boxes.
- *[New]* The crosshair visibility behaviour has been modified: the vertical marker is now on by default and switching both markers on or off is now remembered across sessions.
- *[Fixed]* Automatic check for updates now limited to once per hour.
- *[Fixed]* NPE in workspace source list listener.

## [binjr v1.0.1](https://github.com/binjr/binjr/releases/tag/v1.0.1)
Released on Tue, 25 Apr 2017

- *[New]* Added a feature to automatically check for new releases.
- *[Fixed]* An empty tree view is displayed when after attempt to add a source failed.
- *[Fixed]* Connecting an to invalid source fails silently.
- *[Fixed]* The application hangs while manipulating the tree view when running under Windows 10.

## [binjr v1.0.0](https://github.com/binjr/binjr/releases/tag/v1.0.0)
Released on Fri, 14 Apr 2017

Initial release

