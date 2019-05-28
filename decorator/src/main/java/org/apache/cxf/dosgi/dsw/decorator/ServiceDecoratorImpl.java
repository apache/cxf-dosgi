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
package org.apache.cxf.dosgi.dsw.decorator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.xmlns.service_decoration._1_0.AddPropertyType;
import org.apache.cxf.xmlns.service_decoration._1_0.MatchPropertyType;
import org.apache.cxf.xmlns.service_decoration._1_0.MatchType;
import org.apache.cxf.xmlns.service_decoration._1_0.ServiceDecorationType;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDecoratorImpl implements ServiceDecorator {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDecoratorImpl.class);
    final List<Rule> decorations = new CopyOnWriteArrayList<>();

    private DecorationParser parser;

    public ServiceDecoratorImpl() {
        parser = new DecorationParser();
    }

    @Override
    public void decorate(ServiceReference<?> sref, Map<String, Object> target) {
        for (Rule matcher : decorations) {
            matcher.apply(sref, target);
        }
    }

    void addDecorations(Bundle bundle) {
        for (ServiceDecorationType decoration : getDecorationElements(bundle)) {
            for (MatchType match : decoration.getMatch()) {
                decorations.add(getRule(bundle, match));
            }
        }
    }

    private Rule getRule(Bundle bundle, MatchType match) {
        InterfaceRule m = new InterfaceRule(bundle, match.getInterface());
        for (MatchPropertyType propMatch : match.getMatchProperty()) {
            m.addPropMatch(propMatch.getName(), propMatch.getValue());
        }
        for (AddPropertyType addProp : match.getAddProperty()) {
            m.addProperty(addProp.getName(), addProp.getValue(), addProp.getType());
        }
        return m;
    }

    List<ServiceDecorationType> getDecorationElements(Bundle bundle) {
        @SuppressWarnings("rawtypes")
        Enumeration entries = bundle.findEntries("OSGI-INF/remote-service", "*.xml", false);
        if (entries == null) {
            return Collections.emptyList();
        }
        List<ServiceDecorationType> elements = new ArrayList<>();
        while (entries.hasMoreElements()) {
            try {
                elements.addAll(parser.getDecorations((URL)entries.nextElement()));
            } catch (Exception e) {
                LOG.warn("Error parsing remote-service descriptions in bundle" + bundle.getSymbolicName(), e);
            }
        }
        return elements;
    }

    void removeDecorations(Bundle bundle) {
        for (Rule r : decorations) {
            if (bundle.equals(r.getBundle())) {
                decorations.remove(r); // the iterator doesn't support 'remove'
            }
        }
    }
}
