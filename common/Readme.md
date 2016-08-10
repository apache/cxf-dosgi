# CXF DOSGi common

Handles communication with the HttpService and Intents.

## HttpServiceManager

In case a custom http context is used the providers will register a custom servlet.
Besides this the HttpServiceManager also handles the creation of a default address if none is given and the conversion from the relative address specified on the service property to a fully qualified address.

Configs

Key             | Default | Description
----------------|---------|------------
httpBase        |         | Start of the addres like http://myserver:8181
cxfServletAlias | /cxf     | Name of the cxf servlet alias

The absolute address of a service is determined by :

\[httpBase\]\[cxfservletAlias\]\[relative address\]

## IntentManager

The IntentManager service tracks intent services and allows to apply these to a client or endpoint.

An intent is marked by the service property `org.apache.cxf.dosgi.IntentName`. The value of this property represents the name of the intent that the user can specify on his service to refer to the intent.

An intent can be of the following types.

### IntentTypes

* org.apache.cxf.feature.Feature
* org.apache.cxf.databinding.DataBinding
* org.apache.cxf.binding.BindingConfiguration
* javax.ws.rs.ext.ExceptionMapper
* javax.ws.rs.ext.MessageBodyReader
* javax.ws.rs.ext.MessageBodyWriter
* Callable<List<Object>>

The Callable allows to publish a intent service that returns a List of intents. So several intents can be grouped with one name.
