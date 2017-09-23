Apache CXF Distributed OSGi
===========================

This distribution combines the felix frameworks, Aries Remote Service Admin and CXF-DOSGi
providers[2] for JAX-WS and JAX-RS.

Getting started
---------------

* Start felix

    java -jar bin/felix.jar

* List available endpoints:

    rsa:endpoints

* The list should be empty at this point

* Deploy samples. Copy soap and rest api and impl from the sample dir to the bundle dir
* Delete felix-cache
* Start felix again like above
* List the endpoints again. This time it should show a REST and a SOAP endpoint

[2] http://cxf.apache.org/distributed-osgi.html
