* _[Breaking Change]_ The plugin API for *binjr* v3.0.0 is no longer compatible with previous versions.
* _[New]_ *binjr* is now able to handle and render time series with data types other than numerical values. 
* _[New]_ *binjr* can now extract timeseries data from log files to navigate and filter through log events , in sync with other sources. 
* _[New]_ *binjr* can now be run under the Eclipse OpenJ9 JVM
* _[New]_ Relative presets in the time range selection panel.
* _[New]_ Users no longer have to input a minimum of 3 characters in the source filtering bar to trigger filtering.
* _[New]_ Added a new PERF log level in between INFO and DEBUG.
* _[New]_ Embedded Java runtime updated to OpenJDK 16 and OpenJFX 16.
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