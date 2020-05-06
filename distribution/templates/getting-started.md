# Getting started

There are several ways to get up and running with ***binjr***:

## Download an application bundle

The simplest way to start using ***binjr*** is to download an application bundle from the [download page](../../download/latest_release/).

These bundles contain all the dependencies required to run the app, including a copy of the Java runtime specially
crafted to only include the required components and save disk space.
They are less than 60 MB in size and there is one for each of the supported platform:
[Linux](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_linux-amd64.tar.gz),
[macOS](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_mac-x86_64.dmg) and
[Windows](https://github.com/binjr/binjr/releases/download/${tagName}/binjr-${version}_windows-amd64.msi).

=== "Linux (glibc v2.5 or higher)"
    ***binjr*** for Linux is packaged as a [tar.gz archive](https://github.com/binjr/binjr/releases/download/v2.15.0-SNAPSHOT/binjr-2.15.0-SNAPSHOT_linux-amd64.tar.gz):
     unpack it and run `binjr` to start.

=== "macOS (10.10 or later)"
    ***binjr*** for macOS is packaged as a [dmg image](https://github.com/binjr/binjr/releases/download/v2.15.0-SNAPSHOT/binjr-2.15.0-SNAPSHOT_mac-x86_64.dmg):
    mount the image and click on `binjr`.

    !!! Info
         binjr might not be recognized by Apple, so you may get a warning when trying to run the first time.
         To override it, locate binjr in the Finder on your Mac, control-click the app icon, then choose `Open` from the
         shortcut menu and click `Open` ([see more](https://support.apple.com/guide/mac-help/mh40616/mac)).

         If you want to ensure that your download has not been tempered with,
         [you can verify its openGPG signature](../verify-signature).


=== "Windows (7 or later)"
    ***binjr*** for Windows is packaged as an [msi installer](https://github.com/binjr/binjr/releases/download/v2.15.0-SNAPSHOT/binjr-2.15.0-SNAPSHOT_windows-amd64.msi):
    run the installer and launch `binjr` from the start menu.

    !!! Info
        binjr's installer might not be recognized by Microsoft Defender SmartScreen, so you might need to suppress a warning
        to install it on your system. To do so, click "more info" then "run anyway"
        ([see more](https://docs.microsoft.com/en-us/windows/security/threat-protection/microsoft-defender-smartscreen/microsoft-defender-smartscreen-overview)).

        If you want to ensure that your download has not been tempered with,
        [you can verify its openGPG signature](../verify-signature).


## Build from source

You can also build or run the application from the source code.

**Prerequisites:**

* [Git](https://git-scm.com/open)
* [OpenJDK 11 or later](http://openjdk.java.net/)

**(Optional):**

* (macOS): Xcode command line tools and a version of OpenJDK including [jpackage](https://openjdk.java.net/jeps/343) 
  (14 or later) are required to build the DMG image.
* (Windows): [WiX 3.0 or later](https://wixtoolset.org/) is required to build the MSI installer.

**Build**

1. Clone the [repo from Github](https://github.com/binjr/binjr/): 
    ``` sh
    git clone https://github.com/binjr/binjr/
    ```
   
2. Use the included gradle wrapper to:

    - Build all the modules
    
        === "Linux / macOS"
            ``` sh
            sh gradlew build
            ```
          
        === "Windows"
            ``` bat
            gradlew.bat build
            ```
   
    - Build and start the application   
      
        === "Linux / macOS"
            ``` sh
            sh gradlew run
            ```
      
        === "Windows"
            ``` bat
            gradlew.bat run
            ```
   
    - Build an application bundle for the platform on which you run the build     

        === "Linux / macOS"
            ``` sh
            sh gradlew clean packageDistribution  
            ```
          
        === "Windows"
            ``` bat
            gradlew.bat clean packageDistribution  
            ```
                                  
        !!! warning 
            Please note that it is mandatory to run the `clean` task in between two executions of `packageDistribution` in
            the same environment.


## Run from the command line

You can also start ***binjr*** simply by running a single command line. Running binjr that way means that you don't
need to worry about keeping your copy up to date: it will always start the latest version that was published over
on Maven Central.

!!! Note
    In order to run binjr that way, you need to have Apache Maven installed on your machine and your JAVA_HOME
    environment variable must point at a copy of a Java runtime version 11 or later.


=== "Linux / macOS"

    ``` sh
    mvn exec:java -f <(curl https://binjr.eu/run-binjr.pom)
    ```

=== "Windows"

    ``` bat
    curl https://binjr.eu/run-binjr.pom > %temp%\\run-binjr.pom & mvn exec:java -f %temp%\\run-binjr.pom
    ```

You can also use the `binjr.version`property to start a specific version of binr:

=== "Linux / macOS"

    ``` sh
    mvn exec:java -f <(curl https://binjr.eu/run-binjr.pom) -Dbinjr.version=2.14.0
    ```

=== "Windows"

    ``` bat
    curl https://binjr.eu/run-binjr.pom > %temp%\\run-binjr.pom & mvn exec:java -f %temp%\\run-binjr.pom  -Dbinjr.version=2.14.0
    ```

!!! Tip
    Downloaded components are cached locally by Maven, so it doesn't need to download them again every time you run the application.

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

