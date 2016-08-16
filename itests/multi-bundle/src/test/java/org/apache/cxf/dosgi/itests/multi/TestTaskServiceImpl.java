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
package org.apache.cxf.dosgi.itests.multi;

import java.util.Collection;

import org.apache.cxf.dosgi.samples.soap.Task;
import org.apache.cxf.dosgi.samples.soap.TaskService;

public class TestTaskServiceImpl implements TaskService {

    @Override
    public Task get(Integer id) {
        return new Task(1, "test", "");
    }

    @Override
    public void addOrUpdate(Task task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Integer id) {
        throw new UnsupportedOperationException();

    }

    @Override
    public Collection<Task> getAll() {
        throw new UnsupportedOperationException();
    }

}
