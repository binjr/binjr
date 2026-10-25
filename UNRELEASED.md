
* _[New] [UI]_ Linked worksheets that are not visible are no longer refreshed immediately when the timeline change, but only when they become visible again. This greatly speeds up rendering for the worksheets that *are* visible.  
* _[New] [Flatpak]_ The file picker dialog box now uses the system Desktop Environment and honors theming.  
* _[New] [Core]_ Changed default garbage collector tuning, in order to increase performances on large queries, at the expense of being less eager to release uncommitted memory to the OS.  
* _[New] [Dependencies]_ Replaced GCToolkit with GCSee as the dependency used for parsing JVM GC logs.  
* _[New] [Dependencies]_ Updated embedded runtime (Java 25.0.5 / JavaFX 27.0.1)  