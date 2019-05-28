# CXF DOSGi ssl example

This example demonstrates how to configure ssl with a custom keystore and required key based client auth using a plain DOSGi service with a custom intent.

The example reuses the soap example code. It then adds the ssl intent to the service impl by using a config. This prevents the service from starting up.

We then install the ssl-intent bundle which implements the intent. This allows the service and client to start up.

## Karaf SSL config

We want the karaf HttpService to be secured by https and require a client certificate for authentication.

# Keystore generation

* Create client and server keys.
* Add client certificate to server 

```
mkdir -p etc/keystores
# Create server key
keytool -genkey -dname CN=localhost -keyalg RSA -validity 100000 -alias serverkey -keypass password -storepass password -keystore etc/keystores/keystore.jks
# Create client key and add to keystore as trusted
keytool -genkey -dname CN=chris -keyalg RSA -validity 100000 -alias clientkey -keypass password -storepass password -keystore etc/keystores/client.jks
keytool -export -rfc -keystore etc/keystores/client.jks -storepass password -alias clientkey -file client.cer
keytool -import -trustcacerts -keystore etc/keystores/keystore.jks -storepass password -alias clientkey -file client.cer

# Export server cert
keytool -exportcert -storepass password -keystore etc/keystores/keystore.jks -alias serverKey -file server.cert
# Import server cert into client store
keytool -importcert -storepass password -keystore etc/keystores/client.jks -alias serverKey -file server.cert
```

- Copy these files in etc to the karaf etc dir
- Copy the keystores (*.jks) into the karaf etc directory.

## Installation

- Copy the server side ssl config org.apache.cxf.http.jetty-ssl.cfg into etc 
- Install the CXF DOSGi features
- Install the example

``` 
feature:repo-add cxf-dosgi-samples 2.0.0
feature:install cxf-dosgi-sample-soap-impl cxf-dosgi-sample-soap-client
install -s mvn:org.apache.cxf.dosgi.samples/cxf-dosgi-samples-ssl-intent/2.0.0
```

# Test using browser

If you want to access the service using your browser then you have to export it in pkcs12 format and import it into your browser.

```
# Export client cert as pkcs12 for browser
keytool -importkeystore -srckeystore etc/keystores/client.jks -destkeystore etc/keystores/client.p12 -deststoretype PKCS12
```

Access the service as [](https://localhost:8443/cxf/echo "echo service").
Add a security exemption to accept the server certificate.
