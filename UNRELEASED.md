_[New]_ Changed the default indexing tokenization rules for log files; all dots are now considered a token delimiter (not only dots followed by a space).
> The old tokenizer behaviour can be reinstated by setting UserPreferences.doNotTokenizeOnDots to true in the console.

_[New]_ Added the ability to open a single log file instead of a whole folder or a zip archive.  
_[New]_ Let users choose the date & time that serves as an anchor to construct timestamps for partial data.  
_[Fixed]_ Different outcome when typing the name of a capture group vs selecting it from the dropdown list in the  profile editor.  
_[Fixed]_ Files selection on Linux do not show files with no extensions when "All files" filter is selected.  
_[Fixed]_ Log file adapter does not list files without extensions.  
