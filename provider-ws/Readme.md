# CXF DOSGi provider WS

SOAP based transport for Aries RSA.

# Properties

*   service.exported.interfaces Interfaces to be exported or * to export all
*   service.exported.configs org.apache.cxf.ws
*   org.apache.cxf.ws.address http://localhost:9090/greeter for CXF jetty transport or /greeter for servlet transport
*   org.apache.cxf.ws.httpservice.context Can be set to use a specific http context
*   org.apache.cxf.ws.context.properties.* Properties wih this prefix will be set as properties of the factory. They can be used to configure features
*   cxf.bus.prop.* Properties with this prefix will be set as CXF bus properties (with the prefix removed)

# Modes

This transport has two modes: Simple and JAX-WS. If the service is annotated using @Webservice
then JAX-WS mode is used else simple mode is used.

## Simple

This mode uses the CXF simple frontend and the Aegis Databinding. It can export almost any service but is not much configureable. Aegis is also not very popular anymore. So this
mode is more for exporting existing services and small tests.

## JAX-WS

If the service is annotated using @Webservice then the JAX-WS mode is activated. It uses
the CXF JAX-WS frontend and the JAXB databinding. It can be customized using the usual annotations.

# Samples

See sample greeter
