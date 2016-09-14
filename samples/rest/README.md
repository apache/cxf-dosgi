# CXF DOSGi example REST

This example shows how to expose and use a REST service using declarative services.

The API module defines the TaskResource interface which is annotated using JAXRS annotations.

The impl module implements the TaskService using a simple HashMap internally. It allows to manage Task objects which represent items of a to do list.

 
## Installation

Unpack karaf 4 into a server and client directory.

### Install server 

Start the server karaf

```
feature:repo-add cxf-dosgi-samples 2.0.0
feature:install cxf-dosgi-sample-rest-impl
rsa:endpoints
```

The last command should show one endpoint with a URI as id. You should be able to open the url in the browser. The browser should show the predefined tasks as xml.

### Install client 

Start the client karaf

```
feature:repo-add cxf-dosgi-samples 2.0.0
feature:install cxf-dosgi-sample-rest-client
```
Use commands to test

```
rsa:endpoints
task:list
task:add 4 Mytask
task:list
```

