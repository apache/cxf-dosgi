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
package org.apache.cxf.dosgi.discovery.local.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // prevent instantiation
    }

    public static List<String> getStringPlusProperty(ServiceReference sr, String key) {
        Object value = sr.getProperty(key);
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof String) {
            return Collections.singletonList((String) value);
        }

        if (value instanceof String[]) {
            String[] values = (String[]) value;
            List<String> result = new ArrayList<String>(values.length);
            for (String v : values) {
                if (v != null) {
                    result.add(v);
                }
            }
            return Collections.unmodifiableList(result);
        }

        if (value instanceof Collection<?>) {
            Collection<?> values = (Collection<?>) value;
            List<String> result = new ArrayList<String>(values.size());
            for (Object v : values) {
                if (v instanceof String) {
                    result.add((String) v);
                }
            }
            return Collections.unmodifiableList(result);
        }

        return Collections.emptyList();
    }

    public static boolean matchFilter(BundleContext bctx, String filter, EndpointDescription endpoint) {
        if (filter == null) {
            return false;
        }

        try {
            Filter f = bctx.createFilter(filter);
            Dictionary<String, Object> dict = new Hashtable<String, Object>(endpoint.getProperties());
            return f.match(dict);
        } catch (Exception e) {
            LOG.error("Problem creating a Filter from " + filter, e);
            return false;
        }
    }
    
    public static String normXML(String s) {
        String s2 = stripComment(s);
        String s3 = stripProlog(s2);
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new StreamSource(new StringReader(s3)), new StreamResult(buffer));
            return buffer.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String stripComment(String s) {
        return Pattern.compile("<!--(.*?)-->", Pattern.DOTALL).matcher(s).replaceAll("");
    }

    private static String stripProlog(String s) {
        return s.replaceAll("<\\?(.*?)\\?>", "");
    }
}
