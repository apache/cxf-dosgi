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
package org.apache.cxf.dosgi.topologymanager.importer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;

public final class FilterHelper {
    private static final String OBJECTCLASS_EXPRESSION = ".*\\(" + Constants.OBJECTCLASS + "=([a-zA-Z_0-9.]+)\\).*";
    private static final Pattern OBJECTCLASS_PATTERN = Pattern.compile(OBJECTCLASS_EXPRESSION);

    private FilterHelper() {
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
}
