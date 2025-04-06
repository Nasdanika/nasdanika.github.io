Nasdanika Command Line Interface (CLI) is a suite of Nasdanika capabilities packaged as command line tools. 

[Sources](https://github.com/Nasdanika/cli) 

## Prerequisites

To run Nasdanika CLI you'd need Java 17+.
To build from sources you'd also need Maven.

## Installation

Download installation archive from the [releases](https://github.com/Nasdanika/cli/releases) page.
On Linux make ``nsd`` executable: ``chmod a+x nsd``.

## Building from sources

* Download [sources](https://github.com/Nasdanika/cli) as a zip file or clone the repository
* Run ``mvn clean verify``
* After the build completes the distribuion will be available in ``target/dist`` directory

## Adding to PATH

The distribution is portable and local - it can be put to any directory, but it can only be executed from that directory.
To create an installation which can be used from any directory you will need to create launcher files with absolute paths.

### Windows

```
nsd.bat launcher -f options-global -o nsd-global.bat -s -m org.nasdanika.launcher -c org.nasdanika.launcher.Launcher --add-modules ALL-SYSTEM -M modules -j "@java"
```

If you are using [telemetry](../core/telemetry/index.html), add telemetry options after ``@java``

Add the installation to the ``PATH`` environment variable. 
You may delete/rename ``nsd.bat`` and rename ``nsd-global.bat`` to ``nsd.bat``. 

### Linux

```
./nsd launcher -o nsd-global -s -m org.nasdanika.launcher -c org.nasdanika.launcher.Launcher -M modules
```

Open ``nsd-global`` in a text editor and add ``#!/bin/bash`` line before the java command line.
Make the file executable and add the installation directory to the path. 
You may remove/rename ``nsd`` and rename ``nsd-global`` to ``nsd``.

If you get ``java.lang.module.FindException: Module <module name> not found`` error, open the file in a text editor, locate the problematic module and remove it from the ``--add-modules`` list.