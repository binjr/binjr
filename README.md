# binjr
![Travis build status](https://travis-ci.org/fthevenet/binjr.svg?branch=master) [![AppVeyor build status](https://ci.appveyor.com/api/projects/status/tv8vc0emdueymlp8?svg=true)](https://ci.appveyor.com/project/fthevenet/binjr) [![Maven Central](https://img.shields.io/maven-central/v/eu.fthevenet/binjr.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22eu.fthevenet%22%20AND%20a:%22binjr%22)


*binjr* is an open source time series visualization tool. It can plot time series data from multiple sources as a set of charts, which end users can navigate or zoom in and out.

Its focus is on enabling end users to constitute their own custom sets of views for the data exposed by various sources, generally in ways that are not proposed by the front-ends these sources might already propose, and then let them navigate these views dynamically, by zooming or panning to a chosen time interval or value range.

As such, *binjr* aims to become a valuable tool in forensic analysis when working with data sources that do not provide such flexible visualization natively.

It is a Java based client application, packaged as a single executable JAR file and has no other requirement than a compatible Java Runtime Environment, and runs on multiple Desktop environnements (Windows, MacOS and Linux).

![Screenshot](http://www.binjr.eu/assets/images/screenshot01.png)

![Screenshot](http://www.binjr.eu/assets/images/screenshot05.png)

![Screenshot](http://www.binjr.eu/assets/images/screenshot04.png)

## Getting started


_binjr_ is Java application, built on top of JavaFX, and requires a Java Runtime Environment version 1.8 with JavaFX version 8u40 or later.

The latest release can be found [here](https://github.com/fthevenet/binjr/releases/latest), and is available in different forms:
* _As a JAR_

  All dependencies are packaged inside a single, executable JAR file; to start the application simply run `java -jar binjr.jar` (or double-click the jar file, provided your environment is configured adequately).
  
  You must have a copy of a Java Runtime Environment version 1.8 installed on your computer for this to work.

* _As a native application bundle_

  Plateform specific bundles are available, in the form of an MSI installer for Windows, RPM and DEB packages for Linux and DMG image for MacOS.
  
  Those bundles contain the application and all of its dependencies, as well as a minimal copy of the Java Runtime Environment for the target platform. 
  With the native application bundle for your operating system, it doesn't matter what other version of Java, if any, is installed on your computer.

* _As source code_

  A zip or tar.gz archive that contains all the source code, which can be built using Maven 3 and a 1.8 Java Development Kit.

## Getting help
The documentation can be found [here](https://github.com/fthevenet/binjr/wiki/Reference).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/fthevenet/binjr/issues).

If you would like to contribute, please sumbit a pull request.

## How is it licensed?

*binjr* is released under the [Apache License version 2.0](https://github.com/fthevenet/binjr/blob/master/LICENSE).

