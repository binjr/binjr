_[New]_ Added an option to ignore samples with an undefined Y value instead of forcing them to zero ("Settings > Charts > Treat undefined Y values as 0").  
_[New]_ A  notification popup now shows download progress when the app is being updated.  
_[Fixed]_ `NaN` values produce duplicated samples after Largest-Triangle-Three-Buckets algorithm is applied.  
_[Fixed]_ Pagination mechanism when fetching data from index does not honor forceNanToZero property.  
_[Fixed]_ A race condition in TimeSeriesProcessor.   
_[Fixed]_ Continuously clicking on "Check for update" results in queuing as many download task.  