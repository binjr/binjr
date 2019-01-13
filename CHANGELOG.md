### [binjr v2.2.1](https://github.com/binjr/binjr/releases/tag/v2.2.1)
Released on Fri, 11 Jan 2019

* _[New]_ Enhancements to debug mode console
* _[New]_ Added changelog to distribution
* _[Fixed]_ Disabled the forced sync mechanism for Rrd4J NIO backend.

### [binjr v2.2.0](https://github.com/binjr/binjr/releases/tag/v2.2.0)
Released on Sat, 5 Jan 2019

> **Please note**: Starting with this release, the Maven groupID for all the binjr artifacts changes from `eu.fthevenet` to `eu.binjr`

* _[Change]_ The keyboard shortcut to invoke debug mode changed from CRTL+SHIFT+D to F12.
* _[FIxed]_ The debug output console's log view perpetually grows.

### [binjr v2.1.1](https://github.com/binjr/binjr/releases/tag/v2.1.1)
Released on Fri, 28 Dec 2018

* _[New]_ It is now possible to open binary files and XML dumps created with [RrdTool](https://oss.oetiker.ch/rrdtool/) using the Rrd4j data adapter.  
  __NB:__ This uses Rrd4j 's built-in conversion facilities in order to import the original file's data into a temporary Rrd4j backend, so be aware that opening a very large rrd file (or a very large number of smaller ones) may be slower than expected, due to the necessary conversion process.
* _[Fixed]_ The tree hierarchy for series bindings created with the Rrd4j adapter is incorrect or incomplete

### [binjr v2.1.0](https://github.com/binjr/binjr/releases/tag/v2.1.0)
Released on Thu, 20 Dec 2018

* _[New]_ Added a new data adapter to directly open and plot the content of rrd db files produced by [RRd4j](https://github.com/rrd4j/rrd4j)
* _[New]_ Added a context menu accessible when right-cliking on the tab that provide shortcuts to various manipulations of the tabs (close, edit, duplicate and detach).

### [binjr v2.0.0](https://github.com/binjr/binjr/releases/tag/v2.0.0)
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

### [binjr v1.6.0](https://github.com/binjr/binjr/releases/tag/v1.6.0)
Released on Wed, 31 Oct 2018

* _[New]_ Greatly enhanced time range selection on worksheets.
* _[New]_ Changes to source navigation panel's interface to make it clearer.
* _[New]_  When a source connection is closed,  all associated series on worksheets  are now removed.
* _[New]_ Many minor tweaks and fixes to UI themes.
* _[Fixed]_ Trailing slash in urls prevent connection to JRDS and other http sources.
* _[Fixed]_ An NPE could occur when closing a source with no worksheet.

### [binjr v1.5.3](https://github.com/binjr/binjr/releases/tag/v1.5.3)
Released on Thu, 11 Oct 2018

* _[Fixed]_ Resources from a DataAdapter are not disposed when a source tab is closed.
* _[Fixed]_ Console output window doesn't always acknowledge appearance changes.

### [binjr v1.5.2](https://github.com/binjr/binjr/releases/tag/v1.5.2)
Released on Fri, 5 Oct 2018

* _[New]_ Report the use of an unsupported version of Java 
* _[Fixed]_ Detection of missing JavaFX is broken
* _[Fixed]_ Spurious warning messages because of unset variables.

### [binjr v1.5.1](https://github.com/binjr/binjr/releases/tag/v1.5.1)
Released on Wed, 3 Oct 2018

* _[New]_ User can invoke a console that display log output an d change logging verbosity at runtime.
* _[Fixed]_ File picker dialog box doesn't show if last saved folder is invalid.

### [binjr v1.5.0](https://github.com/binjr/binjr/releases/tag/v1.5.0)
Released on Wed, 19 Sep 2018

* _[New]_ Added a "Dark" UI theme. "Modern" UI theme has been renamed "Light", while "Classic" is unchanged.
* _[New]_ Added the possibility to display debug menu and increase log verbosity at runtime.
* _[Fixed]_ JRDS adapter fails to connect to source if a url contains a trailing slash.
* _[Fixed]_ NPE when initiating a drag & drop motion on an empty tab pane.
* _[Fixed]_ Application cannot start if the UI theme name stored in user preference is not valid.
* _[Fixed]_ The labesl on command bar items sometimes remains visible when the command bar is reduced.

### [binjr v1.4.3](https://github.com/binjr/binjr/releases/tag/v1.4.3)
Released on Mon, 10 Sep 2018

* _[Fixed]_ Built-in DataAdapter are not loaded if an error occurs while scanning the plugin location at startup.
* _[Fixed]_ binjr takes a long time to start because scanning for DataAdapter at visits all sub-folders with maximum depth in plugin location.
* _[Fixed]_ DirectoryChooser dialog doesn't show up if current plugin location if invalid/not a folder

### [binjr v1.4.1](https://github.com/binjr/binjr/releases/tag/v1.4.1)
Released on Tue, 4 Sep 2018

* _[New]_  The duration after which popups automatically fade away can now be configured.
* _[New]_  Relaxed the parsing of URLs when adding a new source (infers a default protocol and port if omitted)
* _[Fixed]_ Failing when a malformed URL is entered for a new JRDS source does not offer a useful error notification.
* _[Fixed]_ Chart background is gray when multiple chart are displayed in stacked view mode but white when overlaid.

### [binjr v1.4.0](https://github.com/binjr/binjr/releases/tag/v1.4.0)
Released on Thu, 2 Aug 2018

* _[New]_ binjr's functionalities can now be extended through the use of plugins. 
For the time being, plugins can be used to implement new data source adapters, in order to make binjr capable to communicate with other source systems without the need to change anything to the core module itself.
* _[New]_ The artifact for the core binjr module, which is the sole dependency for building external plugins, is now available via [Maven Central](https://search.maven.org/%23artifactdetails%7Ceu.fthevenet%7Cbinjr%7C1.4.0%7Cjar).


### [binjr v1.3.4](https://github.com/binjr/binjr/releases/tag/v1.3.4)
Released on Wed, 27 Jun 2018

* _[New]_ Performs a sanity check when loading workspaces from files to verify format version number and alert user with a clear error message if it is incompatible.
* _[New]_ Added the option to choose the layout of multiple charts on a single worksheet, either stacked on top of each other, or as an overlay, sharing the same X axis.
* _[Fixed]_ Charts rendering performances greatly improved when visualizing many charts on a single worksheet.
* _[Fixed]_ Deselecting all time series in the main chart in an overlay view would make times series in other chart disappear.

### [binjr v1.3.0](https://github.com/binjr/binjr/releases/tag/v1.3.0)
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



### [binjr v1.2.3](https://github.com/binjr/binjr/releases/tag/v1.2.3)
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

### [binjr v1.1.0](https://github.com/binjr/binjr/releases/tag/v1.1.0)
Released on Fri, 29 Sep 2017

* _[New]_  Worksheet tabs can now be detached from the main window via a simple drag and drop (similar to a web browser).
* _[New]_ Native platform bundles available for Windows (.msi), MacOS (.dmg) and Linux (.rpm and .deb) 
These are platform specific install packages that contain a minimal and independent Java Runtime Environment and executable bootstrap, allowing binjr to run as a stand-alone application.
* _[New]_ binjr workspace files can be associated with the application so that binjr is launched on double clicking a  .bjr file. This association is automatically performed by the aforementioned native bundles.
* _[New]_  On Windows and MacOS,  root CA certificates stored in the OS keystore are used for SSL validation.
* _[Fixed]_  The modal dialog used for user authentication could appear behind the main stage, hence causing the application to appear frozen.
* _[Fixed]_  A possible Null Pointer Exception when using the source/search feature.
* _[Fixed]_  Aligned button background color in Modern theme with Windows 10 standard controls

### [binjr v1.0.15](https://github.com/binjr/binjr/releases/tag/v1.0.15)
Released on Mon, 24 Jul 2017

* _[New]_ Dialog boxes now support UI Theme
* _[Fixed]_ Wrong style applied to button in date picker control
* _[Fixed]_ Changing series visibility doesn't work if chart type is changed.
* _[Fixed]_  Better exception handling in JRDS dataAdapter: error message displayed to end users should be more relevant and helpful in common error scenario.


### [binjr v1.0.13](https://github.com/binjr/binjr/releases/tag/v1.0.13)
Released on Mon, 26 Jun 2017

* _[New]_ It is now possible to change the type of chart used on worksheet after it's been created.
* _[New]_ User can now set the stroke width on line chart and area charts with an outline.
* _[Fixed]_ Line charts ignore colors set in source.
* _[Fixed]_ A slowdown on plotting large series was introduced in release 1.0.12.

### [binjr v1.0.12](https://github.com/binjr/binjr/releases/tag/v1.0.12)
Released on Thu, 22 Jun 2017

* _[New]_ Displays the value of each series for the instant marked by the current position of the vertical marker.
* _[Fixed]_ Series info in table view aren't refreshed properly when time interval changes.
* _[Fixed]_ Removed obsolete parameters from settings panel.

### [binjr v1.0.11](https://github.com/binjr/binjr/releases/tag/v1.0.11)
Released on Tue, 13 Jun 2017

* _[New]_ Using the "Refresh" button now ignores any previously cached data.
* _[Fixed]_ Sorting JRDS treeview by "All filters" or "All tags" is broken
* _[Fixed]_ Application appears to hang when attempting to close it while it is minimized

### [binjr v1.0.10](https://github.com/binjr/binjr/releases/tag/v1.0.10)
Released on Fri, 9 Jun 2017

* _[Fixed]_  Application doesn't provide a clear reason for not starting when JavaFX runtime is not present.

### [binjr v1.0.9](https://github.com/binjr/binjr/releases/tag/v1.0.9)
Released on Wed, 7 Jun 2017

* _[New]_ Search bar to quickly find items in source tree view.
* _[New]_ Better support for JRDS tree view filters.
* _[Fixed]_ The text in "license" and "acknowledgement" panes in about box is blurry.

### [binjr v1.0.8](https://github.com/binjr/binjr/releases/tag/v1.0.8)
Released on Thu, 1 Jun 2017

* _[Fixed]_ Changes to chart appearance settings (outline, area opacity, etc...) are ignored on area charts.

### [binjr v1.0.7](https://github.com/binjr/binjr/releases/tag/v1.0.7)
Released on Wed, 31 May 2017

* _[New]_ JRDS SourceAdapter now supports authenticating through Kerberos
* _[Fixed]_ Dragged tree node would keep following the mouse pointer after being drop onto a worksheet on Linux
* _[Fixed]_ The landing zone for dropping sources onto empty worksheet pane is now much larger
* _[Fixed]_ An invalid cast exception occurs when rendering line charts.

### [binjr v1.0.6](https://github.com/binjr/binjr/releases/tag/v1.0.6)
Released on Tue, 23 May 2017

- *[Fixed]* JVM does not terminates on its own after the main window is closed.

### [binjr v1.0.5](https://github.com/binjr/binjr/releases/tag/v1.0.5)
Released on Tue, 23 May 2017

- *[New]* Long running tasks, such as loading a workpace or fetching time-series data from sources, are now executed asynchronously to the UI refresh. This increases the global responsiveness of the application and prevents most occurrences of the applications "freezing" for a few seconds during those tasks.
- *[New]* Errors when connecting to a source or parsing a workspace file are now reported as modeless notification popups rather than modal dialog boxes.
- *[New]* The behaviour of the auto-ranging feature for the Y axis has changed; it is now a toggle button, rather than a push button that would reset the range.
- *[Fixed]* An bug in DecimationTransform, causes a "java.lang.IllegalArgumentException: Duplicate data added" exceptions.

### [binjr v1.0.4](https://github.com/binjr/binjr/releases/tag/v1.0.4)
Released on Thu, 18 May 2017

- *[New]* Use drag and drop to add series sources to the current or a new worksheet.

### [binjr v1.0.3](https://github.com/binjr/binjr/releases/tag/v1.0.3)
Released on Wed, 17 May 2017

- *[Fixed]* Pressing 'del' to remove a series from a worksheet also removed all subsequent series in table view.

### [binjr v1.0.2](https://github.com/binjr/binjr/releases/tag/v1.0.2)
Released on Tue, 16 May 2017

- *[New]* Greatly enhanced responsiveness when working with series with large number of samples.
- *[New]* Reworked the UI to display settings and preferences via sliding panes rather than dialog boxes.
- *[New]* The crosshair visibility behaviour has been modified: the vertical marker is now on by default and switching both markers on or off is now remembered across sessions.
- *[Fixed]* Automatic check for updates now limited to once per hour.
- *[Fixed]* NPE in workspace source list listener.

### [binjr v1.0.1](https://github.com/binjr/binjr/releases/tag/v1.0.1)
Released on Tue, 25 Apr 2017

- *[New]* Added a feature to automatically check for new releases.
- *[Fixed]* An empty tree view is displayed when after attempt to add a source failed.
- *[Fixed]* Connecting an to invalid source fails silently.
- *[Fixed]* The application hangs while manipulating the tree view when running under Windows 10.

### [binjr v1.0.0](https://github.com/binjr/binjr/releases/tag/v1.0.0)
Released on Fri, 14 Apr 2017

Initial release

