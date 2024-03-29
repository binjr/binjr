# binjr
###### A Time Series Data Browser
*_Version ${version}, Released on ${releaseDate}_* 

---

## What is binjr?

***binjr*** is a time series data browser; it renders time series data produced by other applications as dynamically 
editable charts and provides advanced features to navigate through the data in a natural and fluent fashion 
(drag & drop, zoom, history, detacheable tabs, advanced time-range picker).
 
It is a standalone client application, that runs independently from the applications that produce the data; there are
no application server or server side components dedicated to ***binjr*** that needs to be installed on the source.   
Like a generic SQL browser only requires a driver to connect and retrieve data from a given DBMS, ***binjr*** 
only needs one specifically written piece of code - here called a data adapter - to enable the dialog with a specific 
source of time series data.

***binjr*** was originally designed - and it still mostly used - to browse performance metrics collected from computers 
and software components, but it was built as a forensic analysis tool, to investigate performance issues or applications
crashes, rather than as a typical monitoring application.   

Because of that, the user experience is more reminiscent of using a profiling application than a dashboard-oriented
monitoring platform; it revolves around enabling the user to compose a custom view by using any of the time-series 
exposed by the source, simply by dragging and dropping them on the view.  
That view then constantly evolves, as the user adds or removes series, from different sources, while navigating through 
it by changing the time range, the type of chart visualization and smaller aspects such as the colour or 
transparency for each individual series.  
The user can then save the current state of the session at any time to a file, in order to reopen it later or to share it 
with someone else.

### ...and what it isn't
* _binjr_ is **not** a system performance collector, nor a collector of anything else for that matter. What it provides is
   efficient navigation and pretty presentation for time series collected elsewhere. 
* _binjr_ is **not** a cloud solution. It's not even a server based solution; it's entirely a client application, 
  albeit one that can get its data from remote servers. Think of it as a browser, only just for time series. 
* _binjr_ is **not** a live system monitoring dashboard. While you can use it to connect to live sources, its feature set is
  not geared toward that particular task, and there are better tools for that out there. Instead, it aims to be an 
  investigation tool, for when you don't necessarily know what you're looking for beforehand and you'll want to build 
  and change the view of the data as you navigate through it rather than be constrained by pre-determined dashboards. 

## Features

#### Data source agnostic
  * Standalone, client-side application.
  * Can connect to any number of sources, of different types, at the same time.
  * Communicates though the APIs exposed by the source. 
  * Supports for data sources is extensible via plugins.
   
####  Designed for ad-hoc view composition
  * Drag and drop series from any sources directly on the chart view.
  * Mix series from different sources on the same view.
  * Allows charts overlay: create charts with several Y axis and a shared time line.
  * Highly customizable views; choose chart types, change series colours, transparency, legends, etc...
  * Save you work session to a file at any time, to be reopened later or shared with someone else.  
   
####  Smooth navigation 
  * Mouse driven zoom of both X and Y axis.
  * Drag and drop composition.
  * Browser-like, forward & backward navigation of zoom history.
  * Advanced time-range selection widget.
  * The tabs holding the chart views can be detached into separate windows.
  * Charts from different tabs/windows can be synchronized to a common time line.
  
####  Fast, responsive & aesthetically pleasing visuals
  * Built on top of [JavaFX](https://openjfx.io/) for a modern look and cross-platform, hardware accelerated graphics.
  * Three different UI themes, to better integrate with host OS and fit user preferences.
    
####  Java based application 
  * Cross-platform: works great on Linux, macOS and Windows desktops!
  * Strong performances, even under heavy load (dozens of charts with dozens of series and thousands of samples).  

  
## Supported data sources

***binjr*** can consume time series data provided by the following data sources:

| Name                                                        | Description                                                                                                                    | Built-in[1] | Source type |
|-------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|-------------|-------------|
| [JRDS](https://github.com/fbacchella/jrds)                  | A performance monitoring application written in Java.                                                                          | ✓           | Remote      |
| [Netdata](https://www.netdata.cloud)                        | Distributed, real-time performance and health monitoring for systems and applications.                                         | ✓           | Remote      |
| RRD Files                                                   | Round-Robin Database files produced by [RRDtool](https://oss.oetiker.ch/rrdtool/) and [RRD4J](https://github.com/rrd4j/rrd4j). | ✓           | Local files | 
| CSV Files                                                   | Comma Separated Values files.                                                                                                  | ✓           | Local files | 
| Log Files                                                   | Text based, semi-structured log files.                                                                                         | ✓           | Local files | 
| [JDK Flight Recoder](https://openjdk.org/jeps/328)          | Low-overhead data collection framework for troubleshooting Java applications and the HotSpot JVM.                              | ✓           | Local files | 
| [Demo Adapter](https://github.com/binjr/binjr-adapter-demo) | A plugin for binjr that provides data sources for demonstration purposes.                                                      |             | Local files |

[1]: Support for data sources not marked as *'Built-in'* requires additional plugins.

## Getting help

The documentation can be found [here](https://binjr.eu/documentation/user_guide/main/).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/binjr/binjr/issues).

## How is it licensed?

***binjr*** is released under the [Apache License version 2.0](https://apache.org/licenses/LICENSE-2.0).


