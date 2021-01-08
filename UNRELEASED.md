## THIS IS A BETA BUILD OF BINJR v3.0.0
#### New features are not entirely stable and might change in incompatible ways in the final release

### Beta 1

* _[Breaking Change]_ The plugin API for *binjr* v3.0.0 is no longer compatible with previous versions.
* _[Breaking Change]_ The format for saved workspace in *binjr* v3.0.0 has changed and is not compatible with previous versions
* _[New]_ *binjr* is now able to handle and render time series with data types other than numerical values. 
* _[New]_ *binjr* can now extract timeseries data from log files to navigate and filter through log events , in sync with other sources. 
* _[New]_ *binjr* can now be run under the Eclipse OpenJ9 JVM
* _[New]_ Relative presets in the time range selection panel.
* _[New]_ Users no longer have to input a minimum of 3 characters in the source filtering bar to trigger filtering.
* _[New]_ Added a new PERF log level in between INFO and DEBUG.
* _[Change]_ Icons and labels for switching to/from 'Edit' and 'Presentation' mode changed to 'Expand/Reduce Series Views'
* _[Fixed]_ If an error occurs while loading an adapter, all subsequent adapter aren't loaded.
* _[Fixed]_ A sharp performance drop when zooming extremely close up on the time axis (i.e. displaying less than a few seconds)
* _[Fixed]_ Removed unused time zone selection field on Netdata adapter dialog. 
* _[Fixed]_ Snapshots taken with the default output scaling use the main monitor scaling factor instead of the one on which the window is displayed. 

### Beta 2
* _[New]_ Added a "Reset Time Range" button to TimeRangePicker control 

### Beta 3
* _[Change]_ Visual tweaks on severity tags on log worksheet view in dark theme. 
* _[Fixed]_ Failed to load a workspace containing a log worksheet that uses a built-in parsing profile.

### Beta 4  
* _[Fixed]_ Workspaces saved with a previous schema version (2.2.0 or higher) are automatically migrated to the new format.
* _[Fixed]_ Error occurring while fetching data from a single adapter prevents plotting the data recovered from other adapters.

### Beta 5
* _[Fixed]_ An error occurs when invoking the log parsing rules edit dialog box.