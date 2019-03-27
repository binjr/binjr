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
