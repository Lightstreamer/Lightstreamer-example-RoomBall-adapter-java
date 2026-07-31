# Lightstreamer - Room-Ball Demo - Java Adapter

<!-- START DESCRIPTION lightstreamer-example-roomball-adapter-java -->

This project includes the resources needed to develop the Metadata and Data Adapters for the [Room-Ball Demo](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-client-javascript) that is pluggable into Lightstreamer Server.<br>

The *Room-Ball Demo* implements a simple pure Server-side Mode multiplayer soccer game:
* Physics runs on server side only
* User commands are streamed from clients to server
* Position updates are streamed from server to clients
* Clients are pure renderers (no feedback, no prediction, no interpolation)
 
For more information, see this slide deck from the HTML5 Developer Conference:<br>
http://www.slideshare.net/alinone/slides-html5-devconf-20131022

## Details

The project is comprised of source code and a deployment example.

### Dig the Code

#### Java Data Adapter and MetaData Adapter

A Java Adapter implementing both the `DataAdapter` interface and the `MetadataAdapter` interface, to inject data into Lightstreamer server with real-time information about the movement of every player in the room. The adapter accepts also message submission for the chat room.<br>
The adapter receives input commands from Lightstreamer server, which forwards messages arrived from clients to the adapter in relation to:
* Movement commands;
* Changing last message for the player.

The Metadata Adapter implements `MetadataAdapter` directly and just adds a simple support for message submission. It should not be used as a reference, as no guaranteed delivery and no clustering support is shown.

<!-- END DESCRIPTION lightstreamer-example-roomball-adapter-java -->

### The Adapter Set Configuration

This Adapter Set is configured and will be referenced by the clients as `ROOMBALL`. 

The `adapters.xml` file for the *Room-Ball Demo*, should look like:

```xml      
<?xml version="1.0"?>
<adapters_conf id="ROOMBALL">

    <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>

    <metadata_provider>
        <adapter_class>com.lightstreamer.examples.roomball_demo.adapters.RoomBallMetaAdapter</adapter_class>

        <!-- Ensure compatibility with old versions of the demo Clients. -->
        <support_LiteralBased_subscriptions>Y</support_LiteralBased_subscriptions>
  
        <!--
          TCP port on which Sun/Oracle's JMXMP connector will be
          listening.
        -->
        <param name="jmxPort">9999</param>
        
        <!--
          Dimensions of the world. 
          Any changes to these parameters involves actions on the client
          side for the drawing of the room.
        -->
        <param name="world_size_x">1000</param>
        <param name="world_size_y">550</param>
        
    </metadata_provider>
    
    <data_provider>
        <adapter_class>com.lightstreamer.examples.roomball_demo.adapters.RoomBallAdapter</adapter_class>
        
        <!--
          Frame rate for physics calculations. In milliseconds.
        -->
        <param name="frameRate">10</param>

        <!--
          Number of steps for a single frame
        -->
        <param name="stepsPerFrame">4</param>
          
    </data_provider>
</adapters_conf>
```
<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, see the [Java In-Process Adapter Interface Project](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#configuration) for details.</i><br>
<br>
Please refer [here](https://lightstreamer.com/docs/ls-server/latest/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

## Install

If you want to install a version of the *Room-Ball Demo* in your local Lightstreamer Server, follow these steps:

* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Make sure that Lightstreamer Server is not running.
* Get the `deploy.zip` file of the [proper release](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/releases), unzip it, and copy the `RoomBall` folder into the `adapters` folder of your Lightstreamer Server installation.
* [Optional]  Customize the logging settings in log4j configuration file `RoomBall/classes/log4j2.xml`.
* Launch Lightstreamer Server.
* Test the Adapter, launching the [Lightstreamer - Room-Ball Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-client-javascript) listed in [Clients Using This Adapter](#clients-using-this-adapter).

## Build

To build your own version of `roomball-adapter-java-x.y.z.jar` instead of using the one provided in the `deploy.zip` file from the [Install](#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity, only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```


## See Also

### Clients Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Room-Ball Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-client-javascript)

### Related Projects

* [Lightstreamer - 3D World Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-3DWorld-adapter-java)
* [Lightstreamer - 3D World Demo - Three.js Client](https://github.com/Lightstreamer/Lightstreamer-example-3DWorld-client-javascript)

<!-- END RELATED_ENTRIES -->
## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java In-Process Adapters since version 9.0.
- For a version of this example compatible with Lightstreamer SDK for Java In-Process Adapters versions 8.x, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/tree/XXXXXXXXXXXXXXXXXXXXX).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters versions 7.4.x, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/tree/last_for_interface_7.4.x).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters versions 7.0 to 7.3, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/tree/last_for_interface_7.3.x).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 6.0, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/tree/pre_mvn).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 5.1, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RoomBall-adapter-java/tree/for_Lightstreamer_5.1).
