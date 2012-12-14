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
package org.apache.cxf.dosgi.samples.springdm.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.dosgi.samples.springdm.DinnerService;
import org.apache.cxf.dosgi.samples.springdm.Restaurant;

public class DinnerServiceImpl implements DinnerService {
    List<Restaurant> restaurants 
        = Arrays.<Restaurant>asList(
            new Restaurant("Jojo's", "1 food way", 3),
            new Restaurant("Boohaa's", "95 forage ave", 1),
            new Restaurant("MicMac", "Plastic Plaza", 1)
        );
    
    public List<Restaurant> findRestaurants(String query) {
        System.out.println("Hey! Someone's using the Dinner Service! Query: " + query);
        return restaurants;
    }
}
