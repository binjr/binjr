* ___[API Change]___ Removed type parameters from the following classes from the Data Adapter API:
    * `DataAdapter<T>` is replaced by `DataAdapter`
    * `BaseDataAdapter<T>` is replaced by `BaseDataAdapter`
    * `HttpDataAdapter<T, A extends Decoder>` is replaced by `HttpDataAdapter`
    * `Decoder<T>` is replaced by `Decoder`
    * `TimeSeriesBinding<T>` is replaced by `TimeSeriesBinding`
    * `TimeSeriesInfo<T>` is replaced by `TimeSeriesInfo`
    * `TimeSeriesProcessor<T>` is replaced by `TimeSeriesProcessor`
       
    This change acknowledges the fact that while the original intent was to keep options open for arbitrary types 
    supports in time series, other part of the code, mostly the presentation layer, is really only able to deal with 
    `double` values.  
    Because the complexity of making the rest of the code fully generic is high, the benefit to do so is not apparent 
    for the time being and the code resulting from the current status quo is prone to unsafe casts to and other code 
    smells, it is preferable to revert to using raw types that only deal with `double` values everywhere. 

* _[New]_ UI refresh:
  * Added an explicit button to trigger editing the name of a source. 
  * Icons for buttons that remove sources/worksheets have been changed from a cross to a bin, to emphasise the 
  fact that the action will irreversibly remove something and not simply dismiss a dialog/pane.  
  * Remove source/worksheet buttons are now hidden until "edit more" is triggered.  
* _[New]_ Added support for small numbers unit prefix (m = milli, µ = micro, n = nano, etc...) for formatting Y axis 
  values.    
* _[Fixed]_ Uncaught exception when entering a negative range for a chart's Y axis cause worksheet to become 
  unresponsive.