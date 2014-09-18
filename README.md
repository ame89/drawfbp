DrawFBP
=======

Tool for Creating and Exploring Flow-Based Programming Diagram Hierarchies


Description
-----------

DrawFBP is a picture-drawing tool that allows users to create multi-level diagrams implementing the technology and methodology known as Flow-Based Programming (FBP).  Diagrams are saved in DrawFBP XML format, and can actually be used to generate JavaFBP networks, which can then be compiled and run on an IDE such as Eclipse.

DrawFBP supports "stepwise decomposition" by supporting subnets - blocks in the diagram can specify lower level diagrams,which can in turn specify lower level ones, and so on.   This allows the user to "zoom in" to a lower level, and then pop back up to the original diagram.

DrawFBP can generate networks for Java, C#, and NoFlo.  These are kept separate in the DrawFBP dialogs and typically use different libraries.

DrawFBP also generates a network definition in .fbp notation.  This was originally defined by Wayne Stevens, and has been somewhat modified for NoFlo.  It will also be usable as input to the C++ implementation, called CppFBP (under development). 

For information about FBP in general, see the FBP web site - http://www.jpaulmorrison.com/fbp . 


Additional features
----

- Variety of symbols, including Report, File, Legend (text with no boundary), External ports (for subnets), Human, ...
- Display subnet in separate tab
- Convert portion of diagram to subnet ("excise")
- Specify connection capacity
- Indicate "drop oldest" for given connection
- Generate Java, C#, JSON, or .fbp notation
- Pan, zoom in/out
- Keyboard-only usage (except positioning of blocks)
- Choose fonts, font sizes
- Help facility
- Export diagram as image
- Print diagram


Prerequisites
---

The project requires Gradle for building (tested with version 2.0). You can download the corresponding package from the following URL: 
http://www.gradle.org

Windows and Linux users should follow the installation instructions on the Maven website (URL provided above).

OSX users (using Brew, http://brew.sh) can install Maven by executing the following command:

    brew install gradle

Eclipse IDE Integration
---

You can generate Eclipse project using the following mvn command:

    gradle eclipse

If you already created an Eclipse project you can run:

    gradle cleanEclipse Eclipse

You need to install a Gradle plugin for Eclipse as explained here:
https://github.com/spring-projects/eclipse-integration-gradle/
Then import a generated project in Eclipse, right (ctrl for OSX) click on the project in Eclipse -> Configure -> Convert to Gradle Project. After the conversion you can Right (ctrl for OSX) click on the project -> Gradle -> Task Quick Launcher and type `build`.

Building from command line
---

For building the project simply run the following command:

    gradle build

As a result a `DrawFBP-2.10.2.jar` file will be created in the `build/libs` directory. 


Running DrawFBP
----

DrawFBP can be executed directly by executing the jar file on the Java platform.  Alternatively it can be run from the command line by entering 

    java -cp build\libs\drawfbp-2.10.2.jar com.jpmorrsn.graphics.DrawFBP

Alternatively, run `gradle installApp` and you will find start scripts in `build\install\drawfbp\bin`.

Sample DrawFBP network
---

Here is a very simple diagram built using DrawFBP, showing process names, names of component source code and class names (used when checking port connections).

![MergeSortDrop](https://github.com/jpaulm/drawfbp/blob/master/docs/MergeSortDisplay1.png "Simple Network Diagram")

JavaHelp facility
---

DrawFBP has a help facility which uses the JavaHelp facility.  The first time you click on Help/Launch Help, you will be prompted to locate the DrawFBP-Help.jar file.  This is the standard JavaHelp jar file, and can be found in the project `lib` directory, in the `lib` directory in the DrawFBP jar file, or can be downloaded from http://www.jpaulmorrison.com/graphicsstuff/DrawFBP-Help.jar .

From then on DrawFBP will remember the location in your `DrawFBPProperties.xml` file.

Running JavaFBP networks generated by DrawFBP
---

If you wish to run any networks that you create with DrawFBP, you will need to add the JavaFBP install jar file, obtainable from GitHub -  https://github.com/jpaulm/javafbp/releases/download/3.0.1/javafbp-3.0.1.jar, or from J. Paul Morrison's web site http://www.jpaulmorrison.com/fbp/JavaFBP-3.0.1.jar, to the Java Build Path of any projects you create. 

If you want to run an app using JavaFBP WebSockets, you will need the jar file for that as well, as described in the README file for the `javafbp-websockets` project on GitHub.

