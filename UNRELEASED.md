* _[New]_ The csv plugin has been entirely rewritten and now features:
  * A new, off heap, backend which allows for working with large quantities of data.
  * A much larger selection of user-configurable parameters for parsing CSV files.
  * A brand new User Interface to manipulate these parameters and interactively test their effects on some sample data.
  * The ability to organize, save and import user defined sets of parameters as "profiles" so that they can be reused and shared.
* _[New]_ It is now possible to cancel the loading of log files if it takes too long.
* _[Fixed]_ It is now possible to zoom in on a time interval shorter than one second on charts.
* _[Fixed]_ Resetting the time range on a chart worksheet no longer only takes the first chart into account.
* _[Fixed]_ The reference date for the predefined ranges on the time range picker is now based on the boundaries reported by the adapter.
* _[Fixed]_ Loading indicator causes high GPU usage.