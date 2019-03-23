* ___[API Change]___ Removed type parameters from the following classes from the Data Adapter API:
    * `DataAdapter<T>` is replaced by `DataAdapter`
    * `BaseDataAdapter<T>` is replaced by `BaseDataAdapter`
    * `HttpDataAdapter<T, A extends Decoder>` is replaced by `HttpDataAdapter`
    * `Decoder<T>` is replaced by `Decoder`
    * `TimeSeriesBinding<T>` is replaced by `TimeSeriesBinding`
    * `TimeSeriesInfo<T>` is replaced by `TimeSeriesInfo`
    * `TimeSeriesProcessor<T>` is replaced by `TimeSeriesProcessor`
* _[New]_ Added an explicit button to trigger editing the name of a source. 
* _[New]_ Added an option to show/hide series legend and source panes.
* _[New]_ Worksheets buttons can now overflow to a menu pane if there is not enough space to display all of them.
* _[New]_ Added support for small numbers unit prefix (m = milli, µ = micro, n = nano, etc...) for formatting Y axis values.  
* _[Fixed]_ A memory leak that occurs when adding, moving or changing the type of a chart in an existing worksheet.
* _[Fixed]_ Uncaught exception when entering a negative range for a chart's Y axis causes a worksheet to become.
* _[Fixed]_ Keyboard shortcuts do no work on detached tab windows.
* _[Fixed]_ Dialog boxes are sometime drawn with a null width and height on some Linux/KDE platforms.
