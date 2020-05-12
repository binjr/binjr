* _[New]_ A new adapter allows to use Netdata (https://netdata.cloud) servers as data sources.  
* _[New]_ Users can now choose which default color palette to use for charts when the color isn't specified by the source. 
* _[New]_ "Show outline" and "Default opacity" preferences are now settable separately for "area charts" and "stacked area" charts.
* _[New]_ Updated the embedded runtime to OpenJDK 14.0.1 and OpenJFX 14.0.1
* _[Fixed]_ JRDS adapter incorrectly reports all charts as stacked area charts.
* _[Fixed]_ "Show outline on area charts " user preference is not persisted across sessions. 
* _[Fixed]_ A concurrency issue causes an ArrayIndexOutOfBoundsException when applying sample reduction transform. 
* _[Fixed]_ The time range picker is not dismissed automatically after the user selects a preset range.