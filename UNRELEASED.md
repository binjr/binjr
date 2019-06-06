* _[New]_ Updates can now be downloaded and applied from within the application.
* _[New]_ binjr now remembers its main window's screen position in-between sessions.
* _[New]_ Embedded OpenJDK in application bundle has been updated to version 12.
* _[New]_ Defaults to the new Shenandoah garbage collector with the "compact" heuristics, which allows for a larger maximum heap size while keeping actual memory usage reasonable when a large heap is no longer required.   
* _[New]_ Warn end-users when trying to add a large number of series to a single chart at once.
* _[New]_ History of previously opened sources is now accessible via a combo box on the selection dialog (as well as through the existing auto-completion feature).
* _[Fixed]_ Unsightly UI theme application on start-up or when detaching tabs.
* _[Fixed]_ If "Span crosshair over all charts" is true and "auto scale Y axis" is off, then selecting a new time range using the mouse results in incorrectly changing the Y axis scale.
* _[Fixed]_ Selecting a timezone in time picker sometime doesn't register.
* _[Fixed]_ Synchronizing timelines across worksheets is broken. 