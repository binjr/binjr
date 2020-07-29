## [binjr v2.17.0](https://github.com/binjr/binjr/releases/tag/v2.16.0)
Released on Thu, 2 Jul 2020

* _[Fixed]_ Jitter on the y-axis when hovering over charts with full height crosshair.
* _[Fixed]_ Incorrect capitalization on some menu entries and labels.

## [binjr v2.16.0](https://github.com/binjr/binjr/releases/tag/v2.16.0)
Released on Wed, 10 June 2020

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
* _[New]_ Updated the embedded runtime to OpenJDK 14.0.1 and OpenJFX 14.0.1
* _[Fixed]_ JRDS adapter incorrectly reports all charts as stacked area charts.
* _[Fixed]_ "Show outline on area charts " user preference is not persisted across sessions. 
* _[Fixed]_ A concurrency issue causes an ArrayIndexOutOfBoundsException when applying sample reduction transform. 
* _[Fixed]_ The time range picker is not dismissed automatically after the user selects a preset range.

## [binjr v2.14.0](https://github.com/binjr/binjr/releases/tag/v2.14.0)
Released on Thu, 19 Mar 2020

* _[New]_ Updated the embedded runtime to OpenJDK 14 and OpenJFX 14.
* _[New]_ Linux version no longer depends on GTK 2.
* _[Fixed]_ "Unrecognized image loader:null" error occurs when attempting to capture snapshots of worksheet with many a large number of charts.

## [binjr v2.13.0](https://github.com/binjr/binjr/releases/tag/v2.13.0)
Released on Thu, 30 Jan 2020

* _[New]_ Enhanced downsampling algorithm; this allows a more faithful visual representation of series while still dramatically reducing the number of plotted samples.
* _[New]_ Updated the embedded Java and JavaFX runtimes to 13.0.2
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

* _[New]_ Updated the embedded Java runtime to OpenJDK 13.0.1
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
* _[New]_ Update bundled OpenJavaFX to version 13.
* _[Fixed]_ NPE in JrdsDataAdapter when the adapter is loaded from saved workspace.
* _[Fixed]_ CsvDataAdapter ignores some configuration keys when loaded from saved workspace.
* _[Fixed]_ Fetching data via an adapter may fail silently.
* _[Fixed]_ Charts do no honor the exact time range specified by the user.
* _[Fixed]_ An offset on the time axis between two or more charts may occurs if the sources for them have different resolutions.
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
* _[New]_ Embedded OpenJDK in application bundle has been updated to version 12.
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
* _[Fixed]_ DataAdapter never cleans up its resources if if fails when populating source tree view.

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
* _[New]_ Added a context menu accessible when right-cliking on the tab that provide shortcuts to various manipulations of the tabs (close, edit, duplicate and detach).

## [binjr v2.0.0](https://github.com/binjr/binjr/releases/tag/v2.0.0)
Released on Mon, 26 Nov 2018


>Starting with version 2.0.0, binjr is built to run on Java 11 and beyond. 
>
>___Please note that it does not run on previous version of Java.___
>
> If you require a version that runs on Java 8, you can use the latest releases versioned 1.x.x.

* _[New]_ Built to run on Java 11 and beyond, and use the new standalone distribution of OpenJFX (https://openjfx.io/)
The platform specific packages above contain all required dependencies, including the Java runtime; simply download the one for your OS, unpack it and run "binjr" to start.
* _[New]_ It is now possible to link the time line of two or more independent worksheets (i.e. change the time range on one worksheet also affect all linked worksheets).
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

