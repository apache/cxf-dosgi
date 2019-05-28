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

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class ServiceDecoratorBundleListener implements BundleListener {
    /**
     *
     */
    private final ServiceDecoratorImpl serviceDecorator;

    /**
     * @param serviceDecorator
     */
    public ServiceDecoratorBundleListener(ServiceDecoratorImpl serviceDecorator) {
        this.serviceDecorator = serviceDecorator;
    }

    @Override
    public void bundleChanged(BundleEvent be) {
        switch(be.getType()) {
        case BundleEvent.STARTED:
            this.serviceDecorator.addDecorations(be.getBundle());
            break;
        case BundleEvent.STOPPING:
            this.serviceDecorator.removeDecorations(be.getBundle());
            break;
        default:
        }
    }
}
