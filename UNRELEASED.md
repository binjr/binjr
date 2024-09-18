* _[New]_ Added a new data adaptor to plot metrics extracted from JVM GC logs.  
* _[New]_ The number of ticks between two graduations the Y axis now automatically adapts to best fit based on the available space and unit types.  
* _[New]_ Doing a hard refresh (Ctrl+F5) on a worksheet now forces all data read form CSV sources to be reloaded from the underlying files.  
* _[New]_ Added a keyboard shortcut (F9) to reset the time range of a worksheet to its default value.  
* _[New]_ Updated embedded OpenJDK and JavaFX runtimes to 23
* _[Fixed]_ When dropping more than one node from the source tree view onto the legend pane of a worksheet, only the last node is added to the current chart.  