# ![binjr](./resources/images/binjr_logo.png)
##### _Version ${version}, Released on ${releaseDate}_ 

***binjr*** is a time series data browser; its purpose is to render time series data produced and stored by 
other applications, and to navigate these rendition as fluently as possible, using the mouse to zoom in and out on
dynamically composed charts, created by the end user on the fly.
 
It is a standalone client application, that runs independently from the applications that produce the data; there are
no application server or server side components dedicated to ***binjr*** that needs to be installed on the source.   
Like a generic SQL browser only requires a driver to connect and retrieve data from a given DBMS, ***binjr*** 
only needs one specifically written piece of code - here called a data adapter - to enable the dialog with a specific 
source of time series data.

***binjr*** was originally designed - and it still mostly used - to browse performance metrics collected from computers 
and software components, but it was built as a forensic analysis tool, to investigate performance issuess or applications
crashes, rather than as a typical monitoring application.   

Because of that, the user experience is more reminiscent of using a profiling application like [WPA](https://docs.microsoft.com/en-us/windows-hardware/test/wpt/windows-performance-analyzer) 
than a dashboard-oriented platform like [Grafana](https://grafana.com/): it revolves around enabling the user to compose
a custom view by using any of the time-series exposed by the source, simply by dragging and dropping them on the view.  
That view then constantly evolves, as the user adds or removes series, from different sources, while navigating through 
it by changing the time range, the type of chart visualization and smaller aspects such as the colour or 
transparency for each individual series.  
The user can then save the current state of the session at any time to a file, inorder to reopen it later or to share it 
with someone else.

### Features

#### Data source agnostic
  * Standalone, client-side application.
  * Can connect to any number of sources, of different types, at the same time.
  * Communicates though the APIs exposed by the source. 
  * Supports for data sources is expendable via plugins.
   
####  Designed for ad-hoc view composition
  * Drag and drop series from any sources directly on the chart view.
  * Mix series from different sources on the same view.
  * Allows charts overlay: create charts with several Y axis and a shared time line.
  * Highly customizable; choose chart types, change series colours, transparency, legends, etc...
  * The whole working session can be saved to a file at any time, to reopen later or to share it with someone else.  
   
####  Fluent navigation 
  * Drag and drop driven zoom of both X and Y axis.
  * Browser-like, forward & backward navigation of zoom history.
  * Advanced time-range selection widget.
  * Create many charts views in detachable tabs, which you can synchronize to the same time line.
  
####  Fast, responsive & aesthetically pleasing visuals
  * Built on top of JavaFX for a modern look and great performances thanks to cross-platform, hardware accelerated graphics.
  * Offers three different UI themes, to better integrate with host OS and fit user preferences.
    
####  Java based application 
  * Cross-platform: works great on Linux, macOS and Windows desktops!
  * Strong performances under heavy load (many charts with dozens of series and tens of thousands of samples).  

  
### Supported data sources

***binjr*** can consume time series data provided by the following data sources:

* [JRDS](http://jrds.fr): A performance monitoring application written in Java.
* Round-Robin Database (RRD) files, produced by [RRDtool](https://oss.oetiker.ch/rrdtool/) and [RRD4J](https://github.com/rrd4j/rrd4j).
* Comma Separated Values (CSV) files.

### Getting started

This package contains contains everything needed to run ***binjr***, including a copy of the Java runtime specially
 crafted to only include the required components and save disk space.

Make sure you've downloaded the right package for the OS you intend to run ***binjr*** on (see the
 [release page](https://github.com/binjr/binjr/releases/latest) for more), unpack it and run `binjr` to start!

  
### Getting help

The documentation can be found [here](https://github.com/binjr/binjr/wiki/Reference).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/binjr/binjr/issues).


### How is it licensed?

***binjr*** is released under the [Apache License version 2.0](https://github.com/binjr/binjr/blob/master/LICENSE.md).




