# CXF DOSGi Examples

All examples are written using DS. For other DI frameworks use their methods to publish and consume
OSGi services.

The features module provides Apache Karaf features for the examples. So using karaf is the easiest way to get the examples up and running quickly.

The SOAP example also provides a bndrun file for bndtools that allows to directly start/debug the service from Eclipse and export it into a runnable jar.

## Checkout and Build 

```
git clone git@github.com:apache/cxf-dosgi.git
mvn clean install -DskipTests -Dcheckstyle.skip=true
```

This will install the examples into your local maven repo.

## Editing the examples in eclipse

Start eclipse and do Import .. Existing Maven Projects. Browse to the samples directory of your
checkout and import all example projects.

## Using bndtools

To use the bndrun files you need the [bndtools](http://bndtools.org/) eclipse extension >= 3.3.0.

## Preparing Apache Karaf

[Download Apache karaf 4.x](http://karaf.apache.org/download.html). Extract the archive and
start Apache Karaf using `bin/karaf`  

## Examples

* [soap - Publish and Consume JAXWS SOAP services](soap)
* [rest - Publish and Consume REST services] (rest) 
* [security-filter - Custom HTTP filter] (security filter)
* [ssl - SSL support and client cert based auth] (ssl)

