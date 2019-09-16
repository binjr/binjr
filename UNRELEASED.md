* _[New]_ Update bundled OpenJavaFX to version 13.
* _[Fixed]_ NPE in JrdsDataAdapter when the adapter is loaded from saved workspace.
* _[Fixed]_ CsvDataAdapter ignores some configuration keys when loaded from saved workspace.
* _[Fixed]_ Fetching data via an adapter may fail silently.
* _[Fixed]_ Charts do no honor the exact time range specified by the user.
* _[Fixed]_ An offset on the time axis between two or more charts may occurs if the sources for them have different resolutions.
* _[Fixed]_ UI themes defined in external plugins aren't loaded if set as the current theme when binjr is started.