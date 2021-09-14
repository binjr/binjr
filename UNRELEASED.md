* _[New]_ Updated the embedded runtimes for Java and JavaFX to version 17.
* _[New]_ Source and target compatibility level for binjr's artifacts have been updated to 17.
* _[New]_ Added the ability to search for and highlight keywords in the application's logs in the debug console.
* _[New]_ Changed the format of macOS installable from a `dmg` image to a `pkg` installer.
> The reason behind this change in format for the macOS deliverable is that unsigned DMG images produced by jpackage 
> could be reported as "corrupted" under certain conditions instead of just warning users that the application was not 
> recognized by the Apple notary service.  
> Bundles generated as PKG installers do not exhibit the same problem and correctly warns users that the application is 
> not recognized by Apple (and not that the package is corrupted).

* _[Fixed]_ It is not possible to select and copy portions of the logs output in the debug console.
* _[Fixed]_ The button to empty the search text field in log work worksheets is displayed even if the field is already empty.
* _[Fixed]_ Clean-up phase could sometime be skipped on closing worksheets, leading to potential memory leaks.
* _[Fixed]_ Inconsistent content type checking when processing http responses from data adapters.