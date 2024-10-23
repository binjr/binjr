* _[New] [UI]_ The number of ticks between two graduations the Y axis now automatically adapts to best fit based on the available space and unit types.
* _[New] [UI]_ Added a keyboard shortcut (F9) to reset the time range of a worksheet to its default value.  
* _[New] [CSV Adapter]_ Doing a hard refresh (Ctrl+F5) on a worksheet now forces all data read form CSV sources to be reloaded from the underlying files.
* _[New] [CSV Adapter]_ Added an option to ignore lines with unparsable time stamps.  
* _[New] [CSV Adapter]_ Better error message when failing to parse a time stamp (provides column and line numbers).  
* _[New] [Dependencies]_ Updated embedded OpenJDK and JavaFX runtimes to 23.0.1  
* _[New] [Dependencies]_ Updated to Lucene 10.  
* _[Fixed]_ "Unable to find valid certification path to requested target" error when trying to establish an HTTPS connection on macOS.  
* _[Fixed]_ When dropping more than one node from the source tree view onto the legend pane of a worksheet, only the last node is added to the current chart.  
* _[Fixed]_ User preference for "Treat undefined Y values as 0" is ignored.  
* _[Fixed]_ The title bar for the binjr window is larger (or smaller) than it should be when using multiple monitors on Windows.  
* _[Fixed]_ Closing and reopening the application causes the main window to slightly grow (or shrink) each time when using multiple monitors on Windows.  