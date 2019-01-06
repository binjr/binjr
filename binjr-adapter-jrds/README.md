# binjr-adapter-jrds [![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-adapter-jrds.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22eu.binjr%22%20AND%20a:%22binjr-adapter-jrds%22)

This modules implements a DataAdapter capable of consuming data from a [JRDS](http://jrds.fr/) instance.

JRDS is a monitoring and performance collection application. It already proposes a web based front-end that allow end-users 
to visualize time-series as graphs which is based on [RRD4J](https://github.com/rrd4j/rrd4j), but does so by producing static images that can’t be 
zoomed in or out.

Using this DataAdapter, you can connect to a JRDS server via HTTP and use the flexibility offered by binjr to 
navigate through the data.

This adapter supports authentication via Kerberos (see [here](https://github.com/binjr/binjr/wiki/Troubleshooting#kerberos-authentication-issues)
 if you need help getting it to work).