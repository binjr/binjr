* _[New] [CSV Adapter]_ The CSV adapter now allows for browsing multiple files from a folder or a ZIP compressed archive.  
  > **Please Note:** All the CSV files made available as a part of a single source declaration will share the same parsing profile. If some of these files require different parsing options, you'll need to declare another source with the appropriate parsing profile for these.
* _[New] [Core]_ [Compact object headers]((https://openjdk.org/jeps/450)) are now enabled by default by the application's launcher, for a 10 to 20% reduction in heap memory usage.  
* _[New] [Core]_ The binjr workspace schema version number has been updated to `3.2`. Opening workspaces created with this version in prior versions of binjr will cause warnings to be logged and may have incompatibility issues.   
* _[Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to `24.0.2`
* _[Fixed] [Core]_ binjr can now properly resolve relative paths to source assets (e.g. paths to .csv, .jfr or log files, etc...) inside saved workspaces (.bjr).  
* _[Fixed] [UI]_ Closing the last tab in a panel on an external tab window closes the whole window even if it contains other non-empty panels.
* _[Fixed] [Core]_ Missing "jdk.accessibility" module causes binjr to fail working with screen readers.
* _[Fixed] [Core]_ Explicitly enabled native access for JavaFX graphics modules to remove warnings.