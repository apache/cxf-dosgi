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
package org.apache.cxf.dosgi.samples.soap.client;

import java.util.Collection;

import org.apache.cxf.dosgi.samples.soap.Task;
import org.apache.cxf.dosgi.samples.soap.TaskService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component//
(//
    service = TaskServiceCommand.class,
    property = //
    {
     "osgi.command.scope=task", //
     "osgi.command.function=list", //
     "osgi.command.function=add", //
     "osgi.command.function=delete"
    }//
)
public class TaskServiceCommand {

    private TaskService taskService;

    public void list() {
        System.out.println("Open tasks:");
        Collection<Task> tasks = taskService.getAll();
        for (Task task : tasks) {
            String line = String.format("%d %s", task.getId(), task.getTitle());
            System.out.println(line);
        }
    }
    
    public void add(Integer id, String title) {
        Task task = new Task(id, title, "");
        taskService.addOrUpdate(task);
    }
    
    public void delete(Integer id) {
        taskService.delete(id);
    }

    @Reference
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }
}
