# binjr-adapter-jvmgc

[![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-adapter-jvmgc.svg?label=Maven%20Central&style=flat-square)](https://search.maven.org/search?q=g:%22eu.binjr%22%20AND%20a:%22binjr-adapter-jvmgc%22)

This module implements a DataAdapter capable of consuming data from a Hotspot JVM GC log files.

## How to use it
In the right hand side panel, select `Sources > New Source... > JVM > GC Logs` and select the path for the GC log you want to analyse.  
After the file's been parsed successfully, the data series that the adapter could extract from it will appear in the source tree; drag the ones you are interested in onto the large chart icon in the right hand pane to plot the data in one or more charts.  
You can then add new charts to an existing tab (a.k.a. "worksheet"), add series to existing charts, or add charts to a new worksheets by dragging data series from the source tree.  

Check out the binjr [user guide](https://binjr.eu/documentation/user_guide/main/) for more.


## Implemented data sources
> **Please Note:**  
> The available data series for a given GC log file will vary depending on the options and verbosity settings passed to the jvm when producting it (i.e. a particular source binding will not appear in the source tree if there was no corresponding data in the log file), as well as the type of garbage collector used.  
> For the time being, all of the above are available for the following collectors:
> * Serial
> * Parallel
> * CMS
> * G1
> 
> Only the "Pause Time" series are available for ZGC and Shenandoah at the moment.


### Pause Time
Scatter point chart with one series per collection event type. Shows durations of pause events, in seconds.


### Occupancy
Charts that show the evolution over time for heap's -- and metaspace -- occupancy (i.e. live set) 

#### Total Heap
Line chart that represents the total heap occupancy (in bytes) with three seriees:
* Before garbage collection (`Heap (Before GC)`)
* After garbage collection (`Heap (After GC)`)
* A merge view of the two above (`Heap (Merge)`)

#### Detailed (Merged)
Stacked area chart that shows a detailed view of the heap occupancy (in bytes) with one series per memory pool type (metaspace, tenured, eden/young, etc... Varies by GC type).


#### Detailed (Before GC)
Stacked area chart that shows a detailed view of the heap occupancy (in bytes) before collection, with one series per memory pool type (metaspace, tenured, eden/young, etc... Varies by GC type).

#### Detailed (After GC)
Stacked area chart that shows a detailed view of the heap occupancy (in bytes) after collection, with one series per memory pool type (metaspace, tenured, eden/young, etc... Varies by GC type).

### Size
Charts that show the evolution over time for the committed size of the memory pools that make up the heap and metaspace.

#### Size (Before GC)
Line chart that show the size of the heap (in bytes) before collection with one series per memory pool type (metaspace, tenured, eden/young, etc... Varies by GC type).

#### Size (After GC)
Line chart that show the size of the heap (in bytes) after collection with one series per memory pool type (metaspace, tenured, eden/young, etc... Varies by GC type).

### CPU
Charts that shows the time spent in threads executing GC code, in seconds

#### CPU Time
Stacked area chart for total cpu time executing GC code, broken out in two series represtenting kernel and user time.

#### CPU (Wall clock)
Line chart showing the real (wall clock) time spent in GC threads.

### References

#### Refences (Pause time)
Scatter points chart that shows the cumulated pause time (in seconds) over time for references, broken out by type (phantom, soft, weak, strong, etc...)

#### Refences (Count)
Scatter points chart that shows the number of references over time, broken out by type (phantom, soft, weak, strong, etc...)

### Allocation Size
(heap usage at start of this gc - heap usage at end of last gc)

### Allocation Rate
(heap usage at start of this gc - heap usage at end of last gc)/time since last gc

## FAQ

### _My log file was created yesterday; why does binjr thinks it was recorded on Jan 1st 1970?_
Binjr can only work with time stamps represented as fully defined instant (i.e. complete date + time + time zone), but it is very common for GC logs to only record elapsed time since the VM start instead of full time stamps, so in order to display these events in binjr, it is first necessary to "anchor" them onto a specific instant. 

By default, binjr uses 1970/01/01 00:00:00 UTC for that purpose, but it is possible to choose a different instant to use as the anchor for imcomplete time stamps by going into `Settings > Appearance & Behavior` and choose a different value for the setting `Anchor for imcomplete timestamps`.
