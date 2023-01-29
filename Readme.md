# Fieldtracks Applications

This repository contains all applications that need to be deployed for running fieldtracks.

* **middlware**: MQTT-Aggregator, Kotlin (Java) CLI-application
* **fieldmon**: Frontend-application, Client-side Javascript
* **StoneFlashtool**: Installer for JellingStone, controlled over MQTT

### Development Notice

This branch is under active development. Currently, there is nothing to create a releasable artifact.
The general idea is created a Dockerimage including all applications - however, is is not created, yet.

This branch implements the new wire protocol as discussed in https://fieldtracks.org/misc/2022/05/25/wire-protocol.html -
however, the status of the individual applications varies.

* **middlware** is rewritten from scratch and has reached feature parity with old Python-MQTT tools.
It can aggregate scan data, simulate stones and handles naming.
* **fieldmon** is not really functional. It is still based on the wire protocol and needs to be rewritten. This is the next step.
* **StoneFlashtool** is not adapted to the new wire protocol, however, there's hardly anything to adapt.
The perspective to replace this tool by a web-usb based installer for easier deployment and installation

Typically, a local MQTT-broker is utilized for development. `contrib/docker-compose.yml` contains an example configuration for starting a mqtt-container.
## Middleware

Middleware is designed as a CLI-application that is deployed alongside the MQTT-Broker. Typically,
it connects to localhost, whereby not needing any credentials. However, different connection-settings can be configured.

Note: Currently, middleware undergoes to refactoring that makes it a Quarkus application.

Notes (unsorted)

* Generating a JWT-key before 

### Building and Running

Middleware is built using gradle. Using the [distribution plugin](https://docs.gradle.org/current/userguide/distribution_plugin.html), various artifacts (.zip, .tar, local dir ) can be created:

#### Starting the application using gradle
```bash
$ cd src/middleware/
./gradlew run
```

#### Creating and using a local distribution
```bash
$ cd src/middleware/
$ ./gradlew clean installDist 

BUILD SUCCESSFUL in 3s
7 actionable tasks: 7 executed

$ cd build/install/middleware/

$ bin/middleware -h
Usage: middleware [OPTIONS]

Options:
  -i, --interval INT          Scan interval in seconds (default: 8)
  -ba, --beacon-age-max INT   Time a beacon is offline before being excluded
                              in seconds (default: 48 * 3600 = 172800)
  -ra, --report-age-max INT   Maximum age of a scan report in seconds
                              (default: 8)
  -s, --server-url-mqtt TEXT  MQTT Server c.f.
                              https://www.eclipse.org/paho/clients/java/
  -sim, --simulate INT...     Simulate stones, beacons - do not process
                              reports
  -u, --user-mqtt TEXT        MQTT User
  -p, --password-mqtt TEXT    MQTT Password
  -h, --help                  Show this message and exit

```

#### Creating releasable artefacts

```bash
$ cd src/middleware/

$ ./gradlew clean assembleDist

$ ls -lh build/distributions/
total 19M
-rw-r--r-- 1 jan jan 9,7M Jun 16 20:22 middleware-1.0-SNAPSHOT.tar
-rw-r--r-- 1 jan jan 8,7M Jun 16 20:22 middleware-1.0-SNAPSHOT.zip

```
