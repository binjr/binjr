* _[New]_ It is now possible to set distinct parsing rules for log files retrieved from a single source, and to swap or edit parsing profiles after a file was added to a worksheet. 
* _[New]_ Enhanced snapshot feature to provide a preview of the snapped image and allows users to either save it to a file or to the clipboard.
* _[New]_ Added contextual menu entries for copying series details from tree view and table view.
* _[New]_ *binjr* can now be built to run natively on aarch64 architectures on Linux and macOS.
* _[Fixed]_ Pressing the "delete" key while editing series name removes it from worksheet.
* _[Fixed]_ Leaves in source treeview are not sorted in alphabetical order.
* _[Fixed]_ A deadlock can occur if an error is raised while parsing log events.
* _[Fixed]_ It is not possible to browse a folder for log files if some of its children are not accessible to the current user (e.g. due to lack of permission for instance). 