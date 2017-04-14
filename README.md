# binjr ![](https://travis-ci.org/fthevenet/binjr.svg?branch=master)

*binjr* is a Java based, open source, time series visualization tool. It can plot time series data from multiple sources as a set of charts, which end users can navigate or zoom in and out.

It is a client application, packaged as a single executable JAR file and has no other requirement than a Java Runtime Environment of the correct version.

Its focus is on enabling end users to constitute their own custom sets of views for the data exposed by various sources, generally in ways that are not proposed by the front-ends these sources might already propose. 

As such, *binjr* aims to become a valuable tool in forensic analysis when working with data sources that do not provide such flexible visualization natively.

![Screenshot](http://www.binjr.eu/assets/images/screenshot01.png)

## How to use it?

*binjr* is pure Java application, built on top of JavaFX, and requires a Java 1.8 JRE with JavaFX 8u40 or later.
Using the latest available version of either Oracle's Hotspot or OpenJDK (with OpenJFX) is highly recommended, as JavaFX is still an area where bug fixes and performance improvments are routinely provided with each new version.

The latest release can be found [here](https://github.com/fthevenet/binjr/releases/latest) 

You can either build it from source using Maven, or use the provided runnable JAR file, which encapsulate all dependencies.

A short tutorial on how to get started can be found [here](https://github.com/fthevenet/binjr/wiki/Getting-started)

## How is it licensed?

*binjr* is released under the Apache License version 2.0.

## What is the status of the project?

*binjr* is very much **a work in progress** and the project is still in its early days.

For the moment, its feature set is far from complete, and absolutely no guarantee is made that the features currently in there right now will actually works, or are not going to evolve significantly in the near future.

Still, because binjr only consumes data, in a read-only fashion, the only risk you’re taking in trying it out is that it might not work quite as you like (or work at all…).

For the moment, the only source with significant support is [JRDS, a monitoring application written in Java](http://jrds.fr/), but the aim of the project is to quickly offer an API flexible enough to allow the rapid development of data adapters for any systems capable of exporting time series data.



