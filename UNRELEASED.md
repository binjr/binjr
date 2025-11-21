* _[New] [UI]_ Added new chart types as possible data representation for time series data:
    * Bar charts: a vertical bar with a height proportional to value being plotted.  
    * Vertical markers: to mark a series of instants on the timeline, as vertical lines that takes up the whole height of a chart.  
    * Duration markers: to represent durations as vertical bands with a width proportional to the duration being plotter.  
* _[New] [UI]_ Added bar charts as a possible type of data representation for time series data.  
* _[New] [UI]_ Added an option to highlight the 'current' column the table view (which displays the Y values for each series on the selected chart at the time currently marked by the crosshair).
* _[Fixed] [UI]_ NaN value show as phantom sample in top left corner of scatter charts.  
* _[Fixed] [JVM GC Logs Adapter]_ Heap generation sizes are not extracted from G1 GC logs.  
* _[Fixed] [JVM GC Logs Adapter]_ GC allocation rate calculation is incorrect.  
* _[Fixed] [Core]_ Pressing `Reset Time Range` sometimes fails to correctly identify the lowest timestamp for the current series.    