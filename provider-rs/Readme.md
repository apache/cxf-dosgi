# CXF DOSGi provider RS

REST based transport for Aries RSA. The exported service must be annotated with JAX-RS annotations to map the methods to HTTP verbs.

# Properties

*   service.exported.interfaces Interfaces to be exported or * to export all
*   service.exported.configs org.apache.cxf.rs
*   org.apache.cxf.rs.address http://localhost:9090/greeter for CXF jetty transport or /greeter for servlet transport
*   org.apache.cxf.rs.httpservice.context Can be set to use a specific http context
*   org.apache.cxf.rs.httpservice.context.properties.* Properties wih this prefix will be set as properties of the factory. They can be used to configure features
*   org.apache.cxf.rs.wadl.location
*   cxf.bus.prop.* Properties with this prefix will be set as CXF bus properties (with the prefix removed)

# Sample

See sample greeter-rest
