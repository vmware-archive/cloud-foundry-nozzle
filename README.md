# Summary
Wavefront Nozzle for Pivotal Cloud Foundry (PCF). The nozzle is deployed as an app (cf push) in PCF.
<br />
The nozzle takes the data from Loggregator Firehose and forwards those metrics to Wavefront proxy.

# Prerequisites
* Make sure you have a working Wavefront proxy with accessible <hostname:port> talking to a Wavefront cloud.
* Make sure you have a working PCF deployment along with access to pcf.host and a UAA user who is authorized to access the loggregator firehose.

# Steps to manually deploy and configure the nozzle

1. Clone the repo
    * Make sure you have Java Maven installed on your machine.
1. Build the source code to generate the JAR - `mvn clean install -DskipTests` is installed.
    * For instance, you can find the JAR here -  "~/.m2/repository/com/wavefront/wavefront-nozzle/<version>-SNAPSHOT/wavefront-nozzle-<version>-SNAPSHOT.jar"
1. Create a target/tmp directory and cp the jar in that directory.
1. Copy the manifest file - "manifest.yml" from the repo to this directory
1. Update the manifest file
    1. Update "path" i.e. the wavefront-nozzle JAR path from above target location
    1. Update "pcf.host"
    1. Update UAA user credentials "pcf.user" and "pcf.password"
    1. Allow or skip SSL Validation, i.e. update "pcf.skipSslValidation"
    1. Update "pcf.firehose.eventTypes" with all the event types that you want. Currently we support - { COUNTER_EVENT, VALUE_METRIC, CONTAINER_METRIC }
    1. Optional - Change "pcf.firehose.parallelism" if you want to.
    1. Update Wavefront proxy hostname and port - i.e. "wavefront.proxy.hostname" and "wavefront.proxy.port"
1. cd into that directory (where you have the jar and manifest.yml) and issue the command - `cf push`
    * Make sure the app is running by monitoring the log file - `cf logs wavefront-firehose-nozzle`

# Running the nozzle locally
If you don't want to deploy the wavefront-nozzle to a PCF instance but rather run/debug this locally on your dev machine,
then you don't need to update manifest.yml file.
<br />
Instead just update the src/main/resources/application.properties with the same values that you intend to replace in manifest.yml
<br />
**Note** - If you don't have a valid application.properties, your nozzle app won't run locally.