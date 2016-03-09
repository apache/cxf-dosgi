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
package org.apache.cxf.dosgi.topologymanager.util;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public final class Utils {

    private static final String OBJECTCLASS_EXPRESSION = ".*\\(" + Constants.OBJECTCLASS + "=([a-zA-Z_0-9.]+)\\).*";
    private static final Pattern OBJECTCLASS_PATTERN = Pattern.compile(OBJECTCLASS_EXPRESSION);

    private Utils() {
        // prevent instantiation
    }

    public static String getObjectClass(String filter) {
        if (filter != null) {
            Matcher matcher = OBJECTCLASS_PATTERN.matcher(filter);
            if (matcher.matches() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Returns the value of a "string+" property as an array of strings.
     * <p>
     * A "string+" property can have a value which is either a string,
     * an array of strings, or a collection of strings.
     * <p>
     * If the given value is not of one of the valid types, or is null,
     * an empty array is returned.
     *
     * @param property a "string+" property value
     * @return the property value as an array of strings, or an empty array
     */
    public static String[] getStringPlusProperty(Object property) {
        if (property instanceof String) {
            return new String[] {(String)property};
        } else if (property instanceof String[]) {
            return (String[])property;
        } else if (property instanceof Collection) {
            try {
                @SuppressWarnings("unchecked")
                Collection<String> strings = (Collection<String>)property;
                return strings.toArray(new String[strings.size()]);
            } catch (ArrayStoreException ase) {
                // ignore collections with wrong type
            }
        }
        return new String[0];
    }

    public static String getBundleName(ServiceReference<?> sref) {
        Bundle bundle = sref.getBundle();
        return bundle == null ? "<unregistered>" : bundle.getSymbolicName();
    }
}
