# CXF DOSGi Distributions

## Apache karaf features

Installing CXF DOSGi in apache karaf is very easy. 

```
feature:repo-add cxf-dosgi 2.0.0
feature:install cxf-dosgi-provider-ws feature:install cxf-dosgi-provider-rs
```

There are also karaf features for the samples that make it really easy to try them inside karaf.

## Bndtools pom based index

Bndtools allows to specify a deployment using a bndrun file. In this file the user only needs to specify
some top level bundles. Bnd then resolves these against an OSGi index.
In CXF-DOSGi we provide all necessary bundles in the [repository](repository) module. This can then be used to create an index for bndtools.

Any example for this is the [samples/repository](../samples/repository) module. This pom refers to the Aries RSA and CXF DOSGi repository poms and adds other bundles needed to create a complete OSGi deployment.

The [SOAP sample](../samples/soap) contains a bndrun file to describe the setup of the SOAP sample. 

## Multi-Bundle

Provides an archive of the dependencies of CXF DOSGi as well as configs for felix and equinox to start the bundles.

This distribution is deprecated as it is quite tedious to create an automated build for your own application based on the archive. 

## Source

Provides the full source code of CXF DOSGi as required by the apache policies. A better way to access the source is through git and [github](https://github.com/apache/cxf-dosgi).
