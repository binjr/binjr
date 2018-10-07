# binjr 2 beta ![Travis build status](https://travis-ci.org/fthevenet/binjr.svg?branch=binjr2) 

>This is a version of binjr built to run on Java 11 and beyond. 
It should be considered of beta quality for the time being.
>
>___Please note that it does not run on previous version of Java.___
>
>Also note that as of Java 11, the JavaFX runtime, which binjr relies on, is no longer supplied as part of the JDK. 
>
>As such, the simplest way to run this version of binjr is to use the full distribution package (choose the right one for your OS), which contains a modular runtime image of Java 11 that includes the JavaFX runtime modules and all other dependencies.

*binjr* is an open source time series visualization tool. It can plot time series data from multiple sources as a set of charts, which end users can navigate or zoom in and out.

Its focus is on enabling end users to constitute their own custom sets of views for the data exposed by various sources, generally in ways that are not proposed by the front-ends these sources might already propose, and then let them navigate these views dynamically, by zooming or panning to a chosen time interval or value range.

As such, *binjr* aims to become a valuable tool in forensic analysis when working with data sources that do not provide such flexible visualization natively.

It is a Java based client application and runs on multiple Desktop environnements (Windows, MacOS and Linux).

![Screenshot](http://www.binjr.eu/assets/images/screenshot01.png)

![Screenshot](http://www.binjr.eu/assets/images/screenshot05.png)

![Screenshot](http://www.binjr.eu/assets/images/screenshot04.png)


## Getting help
The documentation can be found [here](https://github.com/fthevenet/binjr/wiki/Reference).

If you encounter an issue, or would like to suggest an enhancement or a new feature, you may do so [here](https://github.com/fthevenet/binjr/issues).

If you would like to contribute, please sumbit a pull request.

## How is it licensed?

*binjr* is released under the [Apache License version 2.0](https://github.com/fthevenet/binjr/blob/master/LICENSE).

