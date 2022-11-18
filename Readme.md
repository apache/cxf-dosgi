# CXF DOSGi

# NOTE This project is now archived

Provides CXF based Distribution providers for [Aries Remote Service Admin (RSA)](http://aries.apache.org/modules/rsa.html).

CXF DOSGi allows to easily publish and consume SOAP and REST services without using the CXF blueprint extensions or publishing the services using java code. So this is the recommended way to use CXF in OSGi with declarative services. Check the examples to see how simple it is to use.

## Modules

* [common - Common services like intents and HTTPService support](common).
* [provider-ws - SOAP transport](provider-ws)
* [provider-rs - REST transport](provider-rs)
* [decorator - Support for exporting existing services](decorator)

* [Examples](samples)
* [Distributions](distribution)

## Intents

Intents allow a service to leverage CXF extensions like features. A service can list the named intents it requires.
It will then only be exported / imported once all the intents are available. This allows for example security restrictions or logging.
For more information see [common module](common).

## Build

mvn clean install
