# CXF DOSGi example SOAP

This example shows how to expose and use a SOAP service using declarative services.

The API module defines the TaskService interface which is annotated using @WebService to signal that
we want to create a JAXWS based SOAP service.

The impl module implements the TaskService using a simple HashMap internally. It allows to manage Task objects which represent items of a to do list.

 
## Installation

Unpack karaf 4 into a server and client directory.

### Install server 

Start the server karaf

```
feature:repo-add cxf-dosgi-samples 2.0-SNAPSHOT
feature:install cxf-dosgi-sample-soap-impl
rsa:endpoints
```

The last command should show one endpoint with a URI as id. You should be able to open the url in the browser.

### Install client 

Start the client karaf

```
feature:repo-add cxf-dosgi-samples 2.0-SNAPSHOT
feature:install cxf-dosgi-sample-soap-client
```
Use commands to test

```
rsa:endpoints
task:list
task:add 4 Mytask
task:list
```
