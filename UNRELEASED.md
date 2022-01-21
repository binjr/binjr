* _[New]_ Added support for proxy on all HTTP-based data adapters.
* _[New]_ It is now possible to use regular expressions when filtering the source tree view.
* _[New]_ Added a context menu to the list of series in a worksheet to expose edition features (select, delete, rename, etc...)
* _[New]_ Introduced features to automatically infer names and colors for multiple series in a worksheet. 
* _[New]_ Updated the embedded OpenJDK and JavaFX runtimes to 17.0.2
* _[Fixed]_ A memory leak cause by a regression introduced in JavaFX 17.0.0
* _[Fixed]_ Improved performances and reduced memory usage when working with logs.
* _[Fixed]_ A regression introduced in v3.3.0 that caused logs with messages already displayed in debug console to not 
be displayed there again until console is cleared.
* _[Fixed]_ Selecting a series color using the "custom colors" panel from the color picker does not change the graph.