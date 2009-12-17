/** 
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements. See the NOTICE file 
 * distributed with this work for additional information 
 * regarding copyright ownership. The ASF licenses this file 
 * to you under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations 
 * under the License. 
 */
package org.apache.cxf.dosgi.dsw.hooks;


public final class ServiceHookUtils {

//    private static final Logger LOG = Logger.getLogger(ServiceHookUtils.class.getName());
//
//    private ServiceHookUtils() {
//
//    }
//
//    public static ServiceRegistration publish(BundleContext bc, final ServiceReference sr,
//                                              EndpointDescription sd) {
//        ServiceRegistration publication = bc.registerService(ServicePublication.class.getName(),
//                                                             new ServicePublication() {
//                                                                 public ServiceReference getReference() {
//                                                                     return sr;
//                                                                 }
//                                                             }, getPublicationProperties(sd));
//
//        if (publication != null) {
//            LOG.info("Remote " + sd.getInterfaces().toArray()[0]
//                     + " endpoint published via Discovery service");
//        }
//        return publication;
//    }
//
//    public static Server createServer(ConfigurationTypeHandler handler, ServiceReference serviceReference,
//                                      BundleContext dswContext, BundleContext callingContext,
//                                      EndpointDescription sd, Object serviceObject) {
//
//        if (handler == null) {
//            return null;
//        }
//
//        String interfaceName = (String)sd.getInterfaces().toArray()[0];
//        // this is an extra sanity check, but do we really need it now ?
//        Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, interfaceName);
//
//        if (interfaceClass != null) {
//            try {
//                return handler.createServer(serviceReference, dswContext, callingContext, sd, interfaceClass,
//                                            serviceObject);
//            } catch (IntentUnsatifiedException iue) {
//                LOG.info("Did not remote service " + interfaceName + " because intent " + iue.getIntent()
//                         + " could not be satisfied");
//            } catch (Exception ex) {
//                LOG.warning("WARNING : Problem creating a remote endpoint for " + interfaceName
//                            + " from CXF PublishHook, reason is " + ex.getMessage());
//                ex.printStackTrace();
//            }
//        }
//
//        return null;
//    }
//
//    public static void unregisterServer(ServiceRegistration publication, EndpointInfo ei) {
//
//        try {
//            Server server = ei.getServer();
//
//            LOG.info("Stopping CXF Endpoint at "
//                     + server.getDestination().getAddress().getAddress().getValue());
//            server.getDestination().shutdown();
//            server.stop();
//        } catch (Exception ex) {
//            // continue
//        }
//
//        if (ei.isPublished()) {
//            LOG.info("Unpublishing Service Description for "
//                     + ei.getServiceDescription().getProvidedInterfaces().toArray()[0]
//                     + " from a Discovery service ");
//            publication.unregister();
//        }
//    }
//
//    public static ConfigurationTypeHandler getHandler(BundleContext dswBC, List<String> configurationTypes,
//                                                      Dictionary serviceProperties, Map<String, Object> props) {
//        return ConfigTypeHandlerFactory.getInstance().getHandler(dswBC, configurationTypes,
//                                                                 serviceProperties, props);
//    }
//
//    public static boolean isCreatedByDsw(ServiceReference sref) {
//        return sref != null && sref.getProperty(Constants.DSW_CLIENT_ID) != null;
//    }
//
//    private static Map<String, Object> getServiceProperties(EndpointDescription sd) {
//        Map<String, Object> props = sd.getProperties();
//        // for (Object key : sd.getPropertyKeys()) {
//        // props.put(key.toString(), sd.getProperty(key.toString()));
//        // }
//        LOG.info("service properties: " + props);
//        return props;
//    }
//
//    @SuppressWarnings("unchecked")
//    private static Dictionary getPublicationProperties(EndpointDescription sd) {
//        Dictionary props = new Hashtable();
//        props.put(SERVICE_INTERFACE_NAME, sd.getInterfaces());
//        props.put(SERVICE_PROPERTIES, getServiceProperties(sd));
//        props.put(ENDPOINT_ID, UUID.randomUUID().toString());
//        if (sd.getRemoteURI() != null) {
//            props.put(ENDPOINT_LOCATION, sd.getRemoteURI());
//        }
//        LOG.info("publication properties: " + props);
//        return props;
//    }
}
