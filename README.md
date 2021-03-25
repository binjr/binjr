# <a href="https://binjr.eu"> <img alt="binjr" width="30%" height="auto" src="https://binjr.eu/assets/images/binjr_title_dark.png"/></a>  

[![Build Status](https://dev.azure.com/binjr/binjr/_apis/build/status/binjr.binjr)](https://dev.azure.com/binjr/binjr/_build/latest?definitionId=1) [![Github Release](https://img.shields.io/github/release/binjr/binjr.svg?label=Github%20Release)](https://github.com/binjr/binjr/releases/latest) [![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22eu.binjr%22) 

[![trailer](https://binjr.eu/assets/images/binjr_landing_poster.png)](https://binjr.eu/trailer.html)

## Contents
* [What is binjr?](#what-is-binjr)
* [Features](#features)
* [Getting started](#getting-started)
* [Trying it out](#trying-it-out)
* [Getting help](#getting-help)
* [Contributing](#contributing)
* [How is it licensed?](#how-is-it-licensed)

## What is binjr?

***binjr*** is a time series data browser; it renders time series data produced by other applications as dynamically 
editable charts and provides advanced features to navigate through the data in a natural and fluent fashion 
(drag & drop, zoom, history, detacheable tabs, advanced time-range picker).
 
It is a standalone client application, that runs independently of the applications that produce the data; there are
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

* [JRDS](https://github.com/fbacchella/jrds): A performance monitoring application written in Java.
* [Netdata](https://www.netdata.cloud):  distributed, real-time performance and health monitoring for systems and applications
* Round-Robin Database (RRD) files, produced by [RRDtool](https://oss.oetiker.ch/rrdtool/) and [RRD4J](https://github.com/rrd4j/rrd4j).
* Comma Separated Values (CSV) files.
* [A stand-alone demonstration data source.](#trying-it-out)

## Getting started

There are several ways to get up and running with ***binjr***:

#### Download an application bundle

The simplest way to start using ***binjr*** is to download an application bundle from the [download page](https://binjr.eu/download/latest_release/).  

These bundles contain all the dependencies required to run the app, including a copy of the Java runtime specially 
crafted to only include the required components and save disk space.  
They are less than 60 MB in size and there is one for each of the supported platform: Windows, Linux and macOS.

Simply download the one for your system, unpack it and run `binjr` to start!

#### Build from source

You can also build or run the application from the source code using the included Gradle wrapper.  
Simply clone the [repo from Github](https://github.com/binjr/binjr/) and run:
* `./gradlew build` to build the JAR for the all the modules.
* `./gradlew run` to build and start the application straight away.
* `./gradlew clean packageDistribution` to build an application bundle for the platform on which you ran the build.
> Please note that it is mandatory to run the `clean` task in between two executions of the `packageDistribution` in 
> the same environment.


#### Download and run the latest version from the command line.
  
 Alternatively, if your environment is properly set up to run Java 11+ and Apache Maven, you can start ***binjr*** simply 
 by running a single command line:
 * Linux / macOS:
   ```
   mvn exec:java -f <(curl https://binjr.eu/run-binjr.pom)
   ```
 * Windows:
   ```
   curl https://binjr.eu/run-binjr.pom > %temp%\run-binjr.pom & mvn exec:java -f %temp%\run-binjr.pom  
   ```
 
 See [Launch the latest version via Apache Maven](https://github.com/binjr/binjr/wiki/getting-started#launch-the-latest-version-via-apache-maven) 
 in the wiki form more details.

  
## Trying it out

If you'd like to experience binjr's visualization capabilities but do not have a compatible data source handy, you can use
the [demonstration data adapter](https://github.com/binjr/binjr-adapter-demo). 

It is a plugin which embeds a small, stand-alone data source that you can readily browse using ***binjr***.

1. Make sure ***binjr*** is installed on your system and make a note of the folder it is installed in.
2. Download the `binjr-adapter-demo-1.x.x.zip` archive from https://github.com/binjr/binjr-adapter-demo/releases/latest
3. Copy the `binjr-adapter-demo-1.x.x.jar` file contained in the zip file into the `plugins` folder of your 
   ***binjr*** installation.
4. Start ***binjr*** (or restart it if it was runnning when you copied the plugin) and open the `demo.bjr`
   workspace contained in the zip (from the command menu, select `Workspaces > Open...`, or press Ctrl+O) 

  
## Getting help

The documentation can be found [here](https://github.com/binjr/binjr/wiki/).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/binjr/binjr/issues).

## Contributing

At the moment, sources that ***binjr*** can use are limited both in types and numbers, which is to be expected given 
that it is a fully community-driven effort with a tiny number of contributors.  

The great thing about it being an open source, community driven project, though, is that should you believe that there is 
 is a use case where ***binjr*** could be a good fit but lacks supports for a specific time-series DB or some other feature,
 there are always ways to make it happen.
 
So, please, do not hesitate to suggest a new  feature or source support request by opening a [issue](https://github.com/binjr/binjr/issues). 
 
Source code contributions are also welcome; if you wish to make one, please fork this repository and submit a pull request
with your changes. 


## How is it licensed?

***binjr*** is released under the [Apache License version 2.0](https://github.com/binjr/binjr/blob/master/LICENSE.md).




