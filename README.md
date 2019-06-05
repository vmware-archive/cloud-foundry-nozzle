# Wavefront cloud-foundry-nozzle (Retired) [![build status][ci-img]][ci]
Wavefront Nozzle for Pivotal Cloud Foundry (PCF). The nozzle is deployed as an app in PCF as part of the [Wavefront by VMware Nozzle for PCF](https://network.pivotal.io/products/wavefront-nozzle/).

The nozzle gathers data from the Loggregator Firehose and forwards those metrics to the Wavefront proxy.

**Note**: This nozzle has been replaced with [cloud-foundry-nozzle-go](https://github.com/wavefrontHQ/cloud-foundry-nozzle-go).

## Requirements
* A working Wavefront proxy with accessible `<hostname:port>`.
* A working PCF deployment with access to `pcf.host` and a UAA user authorized to access the loggregator firehose.
* Java >= 1.8
* Maven

## Manually configure and deploy the nozzle

1. `git clone github.com/wavefrontHQ/cloud-foundry-nozzle`
1. Build the source code to generate the JAR under `target/wavefront-nozzle-<version>-SNAPSHOT.jar`:
   ```
   mvn clean install -DskipTests
   ```
1. Copy the above jar and the `manifest.yml` file to a temporary directory
1. Update the following properties in the manifest file:
    * `path`: The path to the wavefront-nozzle jar under the temporary directory
    * `pcf.host`: Enter the PCF FQDN
    * `pcf.user`: PCF UAA user authorized to access the loggregator firehose
    * `pcf.password`: PCF UAA user password
    * `pcf.skipSslValidation`: `true` to skip SSL validation.
    * `pcf.firehose.eventTypes`: Enter the event types desired. Currently supported: `COUNTER_EVENT, VALUE_METRIC, CONTAINER_METRIC`
    * `pcf.firehose.parallelism`: Optional - Change if needed
    * `wavefront.proxy.hostname`: Enter the hostname or IP address of the Wavefront proxy
    * `wavefront.proxy.port`: 2878
1. cd into the directory where you have the jar and manifest.yml and run: `cf push`
1. Verify the app is running by monitoring the log file - `cf logs wavefront-firehose-nozzle`

## Running the nozzle locally
If you don't wish to deploy the wavefront-nozzle to a PCF instance, you can run/debug it locally on your dev machine.

To run locally, you don't need the manifest file. Just update `src/main/resources/application.properties` with the same values as in the manifest file and run the application.

**Note** - The nozzle will not run locally if you have an invalid application.properties.

[ci-img]: https://travis-ci.com/wavefrontHQ/cloud-foundry-nozzle.svg?branch=master
[ci]: https://travis-ci.com/wavefrontHQ/cloud-foundry-nozzle
