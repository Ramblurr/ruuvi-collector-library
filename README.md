# ruuvi-collector-library

This is a small proof of concept library that implements a library interface
for reading RuuviTags in Java.


It is based on the code from
[Scrin/RuuviCollector](https://github.com/Scrin/RuuviCollector), but with these
differences:

* bring your own persistence/storage

  Implement the `MeasurementListener` interface to handle measurements.

* measurements are immutable values
* derived data is separate from the base data
* no real configuration options
* robust error handling and process restarting

This isn't a real library per-se, but a POC of what one would look like. Take
the code into your own project and improve it.

### Run the example

The example is no frills. No maven, no gradle. None of that. Just a few jars.

Add them to your classpath with your IDE. Compile and run Main.

jars:

* lombok - https://projectlombok.org/download

  Feel free to delomok if you want.

* [ruuvitag-common](https://github.com/Scrin/ruuvitag-common-java) is a little
  library that parses the binary payload emitted by the tags. You can get the
  jar from [the releases page](https://github.com/Scrin/ruuvitag-common-java/releases).
* org.apache.logging.log4j:log4j-core
* log4j:apache-log4j-extras
