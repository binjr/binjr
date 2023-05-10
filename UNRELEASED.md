_[New]_ *binjr* now defaults to an indexing strategy for log files that is optimized for partial terms search and filtering. It allows for fast matching of arbitrary character sequences without the need for explicit syntax like wildcards.
> To revert back to the old behavior that favors searching for whole words, go to "Settings > Logs" and select "Optimize index for whole words search".

_[New]_ Added the ability to open a single log file instead of a whole folder or a zip archive.  
_[New]_ Inline help is now directly accessible for many options throughout the application's User Interface by clicking the `?`next to it.  
_[New]_ Let users choose the date & time that serves as an anchor to construct timestamps for partial data.  
_[New]_ Added an option for the user to toggle whether or not the Y axis should always include the origin (0) when auto-scale is enabled.  
_[New]_ Updated embedded runtimes to OpenJDK 20.0.1 and OpenJFX 20.0.1
_[New]_ Added more built-in parsing profiles (Quarkus, Syslog)
_[Fixed]_ Removed unnecessary scoring computation in Log adapter queries to increase filtering performances.
_[Fixed]_ Settings panel is now wider and its content less cramped.
_[Fixed]_ Different outcome when typing the name of a capture group vs selecting it from the dropdown list in the  profile editor.  
_[Fixed]_ Files selection on Linux do not show files with no extensions when "All files" filter is selected.  
_[Fixed]_ Log file adapter does not list files without extensions.  
_[Fixed]_ Log file view does not use a monotype font on macOS and Linux 
_[Fixed]_ Auto-update feature ignores alternative signing openPGP signature.
