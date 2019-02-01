# binjr 

[![Build Status: Linux](https://img.shields.io/travis/binjr/binjr.svg?logo=linux&logoColor=white&style=flat-square)](https://travis-ci.org/binjr/binjr)
[![Build Status: macOS](https://img.shields.io/travis/binjr/binjr.svg?logo=apple&logoColor=white&style=flat-square)](https://travis-ci.org/binjr/binjr)
[![Build Status: Windows](https://img.shields.io/appveyor/ci/fthevenet/binjr.svg?logo=windows&style=flat-square)](https://ci.appveyor.com/project/fthevenet/binjr/branch/master)
[![Github Release](https://img.shields.io/github/release/binjr/binjr.svg?style=flat-square)](https://github.com/binjr/binjr/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-core.svg?label=Maven%20Central&style=flat-square)](https://search.maven.org/search?q=g:%22eu.binjr%22)




***binjr*** is a time series data browser; its purpose is to render time series data produced and stored by 
other applications, and to navigate these rendition as fluently as possible, using the mouse to zoom in and out on
dynamically composed charts, created by the end user on the fly.
 
It is a standalone client application, that runs independently from the applications that produce the data; there are
no application server or server side components dedicated to ***binjr*** that needs to be installed on the source.   
Like a generic SQL browser only requires a driver to connect and retrieve data from a given DBMS, ***binjr*** 
only needs one specifically written piece of code - here called a data adapter - to enable the dialog with a specific 
source of time series data.

***binjr*** was originally designed - and it still mostly used - to browse performance metrics collected from computers 
and software components, but it was built as a forensic analysis tool, to investigate performance issues or applications
crashes, rather than as a typical monitoring application.   

Because of that, the user experience is more reminiscent of using a profiling application like [WPA](https://docs.microsoft.com/en-us/windows-hardware/test/wpt/windows-performance-analyzer) 
than a dashboard-oriented platform like [Grafana](https://grafana.com/): it revolves around enabling the user to compose
a custom view by using any of the time-series exposed by the source, simply by dragging and dropping them on the view.  
That view then constantly evolves, as the user adds or removes series, from different sources, while navigating through 
it by changing the time range, the type of chart visualization and smaller aspects such as the colour or 
transparency for each individual series.  
The user can then save the current state of the session at any time to a file, in order to reopen it later or to share it 
with someone else.

![Screenshot](https://binjr.eu/assets/images/screenshot06.png)

## Features

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
  * Built on top of [JavaFX](https://openjfx.io/) for a modern look and great performances thanks to cross-platform, hardware accelerated graphics.
  * Offers three different UI themes, to better integrate with host OS and fit user preferences.
    
####  Java based application 
  * Cross-platform: works great on Linux, macOS and Windows desktops!
  * Strong performances under heavy load (many charts with dozens of series and tens of thousands of samples).  

  
## Supported data sources

***binjr*** can consume time series data provided by the following data sources:

* [JRDS](http://jrds.fr): A performance monitoring application written in Java.
* Round-Robin Database (RRD) files, produced by [RRDtool](https://oss.oetiker.ch/rrdtool/) and [RRD4J](https://github.com/rrd4j/rrd4j).
* Comma Separated Values (CSV) files.

## Getting started

There are several ways to get up and running with ***binjr***:

### Download an application bundle

The simplest way to start using ***binjr*** is to download an application bundle from the [release page](https://github.com/binjr/binjr/releases/latest).  

These bundles contain all the dependencies required to run the app, including a copy of the Java runtime specially 
crafted to only include the required components and save disk space.  
They are less than 45 MB in size and there is one for each of the supported platform: Windows, Linux and macOS.

Simply download the one for your system, unpack it and run `binjr` to start!

### Launch the latest version via Apache Maven
 
Alternatively, if your environment is properly set up to run Java 11 and Apache Maven, you can start ***binjr*** simply by running the following command line:

#### On Linux or macOS:

* To start the latest version:
  ```
  mvn exec:java -f <(curl https://binjr.eu/run-binjr.pom)
  ```
* To start a specific version:
  ```
  mvn exec:java -f <(curl https://binjr.eu/run-binjr.pom) -Dbinjr.version=2.3.0
  ```
  
#### On Windows:

* To start the latest version:
  ```
  curl https://binjr.eu/run-binjr.pom > %temp%\run-binjr.pom & mvn exec:java -f %temp%\run-binjr.pom  
  ```
* To start a specific version:
  ```
  curl https://binjr.eu/run-binjr.pom > %temp%\run-binjr.pom & mvn exec:java -f %temp%\run-binjr.pom -Dbinjr.version=2.3.0
  ```
  
Runnning ***binjr*** that way means that you don't need to worry about keeping your copy up to date: it will always start 
the latest version that was published over on [Maven Central](https://search.maven.org/search?q=g:%22eu.binjr%22) 
(unless you explicitly set the desired version, see above).   
Downloaded components are cached locally by Maven, so it doesn't need to download them again every time you 
run the application.

> **NB:** In order to run ***binjr*** that way, you not only need to have Apache Maven installed on your 
> machine but also need your JAVA_HOME environment variable to point at a copy of a __Java runtime version 11 or later__.

### Build from source

You can also build or run the application from the source code using the included Gradle wrapper.  
Simply clone the [repo from Github](https://github.com/binjr/binjr/) and run:
* `./gradlew build` to build the JAR for the all the modules.
* `./gradlew run` to build and start the application straight away.
* `./gradlew clean packageDistribution` to build an application bundle for the platform on which you ran the build.
  
## Getting help

The documentation can be found [here](https://github.com/binjr/binjr/wiki/).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/binjr/binjr/issues).

## Contributing

At the moment, sources that ***binjr*** can use are limited both in types and numbers, which is to be expected given 
that it is a fully community driven effort with a tiny number of contributors.  

The great thing about it being an open source, community driven project, though, is that should you believe that there is 
 is a use case where ***binjr*** could be a good fit but lacks supports for a specific time-series DB or some other feature,
 there are always ways to make it happen.
 
So, please, do not hesitate to suggest anew  feature or source support request by opening a [issue](https://github.com/binjr/binjr/issues). 
 
Source code contributions are also welcome; if you wish to make one, please fork this repository and submit a pull request
with your changes. 


## How is it licensed?

***binjr*** is released under the [Apache License version 2.0](https://github.com/binjr/binjr/blob/master/LICENSE.md).




