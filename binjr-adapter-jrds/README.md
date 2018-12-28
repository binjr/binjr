# binjr-adapter-jrds

This modules implements a DataAdapter capable of consuming data from a [JRDS](http://jrds.fr/) instance.

JRDS is a monitoring and performance collection application. It already proposes a web based front-end that allow end-users 
to visualize time-series as graphs which is based on [RRD4J](https://github.com/rrd4j/rrd4j), but does so by producing static images that can’t be 
zoomed in or out.

Using this DataAdapter, you can connect to a JRDS server via HTTP and use the flexibility offered by binjr to 
navigate through the data.

This adapter supports authentication via Kerberos (see [here](https://github.com/fthevenet/binjr/wiki/Troubleshooting#kerberos-authentication-issues)
 if you need help getting it to work).