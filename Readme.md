# CXF DOSGi

Provides CXF based Distribution providers for [Aries Remote Service Admin (RSA)](http://aries.apache.org/modules/rsa.html).

## Distribution Providers

*   cxf-dosgi-provider-ws SOAP transport 
*   cxf-dosgi.provider-rs REST transport

## Intents

A service can list the named intents it requires. It will then only be exported / imported 
once all the intents are available. This allows for example security restrictions or logging.

Example

* service.exported.intents=logging

See [](common "common module").

## Build

mvn clean install

## Deployment

CXF DOSGi can be deployed in three different ways

*   Multi bundle distro (deprecated)
*   Karaf feature
*   Bndtools pom repository
 
