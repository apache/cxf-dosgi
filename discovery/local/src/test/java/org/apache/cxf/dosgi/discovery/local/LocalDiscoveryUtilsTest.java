package org.apache.cxf.dosgi.discovery.local;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.jdom.Element;
import org.jdom.Namespace;
import org.osgi.framework.Bundle;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.discovery.ServicePublication;

public class LocalDiscoveryUtilsTest extends TestCase {
    public void testNoRemoteServicesXMLFiles() {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(0, rsElements.size());        
    }
    
    public void testRemoteServicesXMLFiles() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(2, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    public void testMultiRemoteServicesXMLFiles() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        URL rs2URL = getClass().getResource("/rs2.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL, rs2URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(3, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("org.example.Service", rsElements.get(2).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    @SuppressWarnings("unchecked")
    public void testRemoteServicesXMLFileAlternateLocation() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        Dictionary headers = new Hashtable();        
        headers.put("Remote-Service", "META-INF/osgi");
        headers.put("Bundle-Name", "testing bundle");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.findEntries(
            EasyMock.eq("META-INF/osgi"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(2, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    public void testAllRemoteReferences() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                andReturn(Collections.enumeration(Arrays.asList(rs1URL)));
        EasyMock.replay(b);
        
        List<ServiceEndpointDescription> seds = LocalDiscoveryUtils.getAllRemoteReferences(b);
        assertEquals(2, seds.size());
        Map<Collection<String>, String> eids = getEndpointIDs(seds);
        
        List<String> interfaces = Arrays.asList("SomeService");
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        sed1Props.put(ServicePublication.ENDPOINT_ID, eids.get(
                Collections.singleton("SomeService")));
        sed1Props.put(ServicePublication.SERVICE_INTERFACE_NAME, interfaces);
        ServiceEndpointDescription sed1 = 
            new ServiceEndpointDescriptionImpl(interfaces, sed1Props);

        List<String> interfaces2 = Arrays.asList("SomeOtherService", "WithSomeSecondInterface");
        Map<String, Object> sed2Props = new HashMap<String, Object>();
        sed2Props.put(ServicePublication.ENDPOINT_ID, eids.get(
                new HashSet<String>(interfaces2)));
        sed2Props.put(ServicePublication.SERVICE_INTERFACE_NAME, interfaces2);
        ServiceEndpointDescription sed2 = 
            new ServiceEndpointDescriptionImpl(interfaces2, sed2Props);
        assertTrue(seds.contains(sed1));
        assertTrue(seds.contains(sed2));
    }

    @SuppressWarnings("unchecked")
    private Map<Collection<String>, String> getEndpointIDs(
            List<ServiceEndpointDescription> seds) {
        Map<Collection<String>, String> map = new HashMap<Collection<String>, String>();
        
        for (ServiceEndpointDescription sed : seds) {
            map.put((Collection<String>) sed.getProvidedInterfaces(), sed.getEndpointID());
        }
        
        return map;
    }
}
