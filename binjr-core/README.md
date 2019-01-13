# binjr-core 

[![Maven Central](https://img.shields.io/maven-central/v/eu.binjr/binjr-core.svg?label=Maven%20Central&style=flat-square)](https://search.maven.org/search?q=g:%22eu.binjr%22%20AND%20a:%22binjr-core%22)

This is the main module for the application. It is responsible for :
 * The presentation layer and interaction with the end-user.
 * The management and persistence of user sessions:
    - A single session is organised as a [Workspace](https://github.com/binjr/binjr/tree/master/binjr-core/src/main/java/eu/binjr/core/data/workspace/Workspace.java).
    - A workspace can hold an arbitrary number of [Worksheets](https://github.com/binjr/binjr/tree/master/binjr-core/src/main/java/eu/binjr/core/data/workspace/Worksheet.java) and [Sources](https://github.com/binjr/binjr/tree/master/binjr-core/src/main/java/eu/binjr/core/data/workspace/Source.java)
    - A worksheet displays data from time-series from any sources on one or more graphs, all sharing a single timeline. 
    - A source is backed by a [DataAdapter](https://github.com/binjr/binjr/tree/master/binjr-core/src/main/java/eu/binjr/core/data/adapters/DataAdapter.java), a component which handles the actual communication to a data source.
    - A workspace can be saved (i.e. serialized to XML and written to a file) by the end-user at any time to be restored later on.
 * Loading and instantiating DataAdapters for the various supported data sources, packaged in other modules.
 * Exposing the [DataAdapter API](https://github.com/binjr/binjr/tree/master/binjr-core/src/main/java/eu/binjr/core/data/adapters) to other modules 
