# CXF DOSGi Decorator

Allows to export existing services by adding an xml descriptor to OSGI-INF/remote-service.

The descriptor selects services by matching interfaces and service properties and can add arbitrary service properties to it. So these services can be marked for export and configured.

See [example descriptor](src/test/resources/test-resources/sd.xml).
