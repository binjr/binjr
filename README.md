# binjr 
[![Build Status: Linux](https://img.shields.io/travis/binjr/binjr.svg?logo=linux&logoColor=white&style=flat-square)](https://travis-ci.org/binjr/binjr)
[![Build Status: macOS](https://img.shields.io/travis/binjr/binjr.svg?logo=apple&logoColor=white&style=flat-square)](https://travis-ci.org/binjr/binjr)
[![Build Status: Windows](https://img.shields.io/appveyor/ci/fthevenet/binjr.svg?logo=windows&style=flat-square)](https://ci.appveyor.com/project/fthevenet/binjr/branch/master)
[![Github Release](https://img.shields.io/github/release/binjr/binjr.svg?style=flat-square)](https://github.com/binjr/binjr/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-core.svg?label=Maven%20Central&style=flat-square)](https://search.maven.org/search?q=g:%22eu.binjr%22)




>Starting with version 2.0.0, binjr is built to run on Java 11 and beyond. 
>
>___Please note that it does not run on previous version of Java.___
>
> If you require a version that runs on Java 8, you can use the latest releases versioned 1.x.x, 
> the source code for it being available in branch [binjr-1_x](https://github.com/binjr/binjr/tree/binjr-1_x).

*binjr* is an open source time series visualization tool. It can plot time series data from multiple sources as a set of charts, which end users can navigate or zoom in and out.

Its focus is on enabling end users to constitute their own custom sets of views for the data exposed by various sources, generally in ways that are not proposed by the front-ends these sources might already propose, and then let them navigate these views dynamically, by zooming or panning to a chosen time interval or value range.

As such, *binjr* aims to become a valuable tool in forensic analysis when working with data sources that do not provide such flexible visualization natively.

It is a Java based client application and runs on multiple Desktop environnements (Windows, MacOS and Linux).

![Screenshot](http://binjr.eu/assets/images/screenshot01.png)

![Screenshot](http://binjr.eu/assets/images/screenshot02.png)

## Getting started
The platform specific packages avalable on the [release page](https://github.com/binjr/binjr/releases/latest) contain all the dependencies requiered to run the app, including the Java runtime.

Simply download the one for your system (Windows, Linux or macOS), unpack it and run "binjr" to start!

## Getting help
The documentation can be found [here](https://github.com/binjr/binjr/wiki/Reference).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/binjr/binjr/issues).

If you would like to contribute, please sumbit a pull request.

## How is it licensed?

*binjr* is released under the [Apache License version 2.0](https://github.com/binjr/binjr/blob/master/LICENSE).

