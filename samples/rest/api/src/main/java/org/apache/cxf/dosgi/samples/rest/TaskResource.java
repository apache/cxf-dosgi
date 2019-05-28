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
package org.apache.cxf.dosgi.samples.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = {"tasks"})
@Path("")
@Consumes({"application/xml", "application/json"})
@Produces({"application/xml", "application/json"})
public interface TaskResource {

    @ApiOperation(value = "Get task by ID", notes = "Returns a single task", response = Task.class)
    @ApiResponses(value = {
                           @ApiResponse(code = 404, message = "Task not found")
    })
    @GET
    @Path("/{id}")
    Task get(@PathParam("id") Integer id);

    @ApiOperation(value = "Add task")
    @POST
    void add(Task task);

    @ApiOperation(value = "Update existing task")
    @PUT
    @Path("/{id}")
    void update(Integer id, Task task);

    @ApiOperation(value = "Deletes a task")
    @ApiResponses(value = {
                           @ApiResponse(code = 404, message = "Task not found")
    })
    @DELETE
    @Path("/{id}")
    void delete(Integer id);

    @ApiOperation(value = "Retrieve all tasks")
    @GET
    Task[] getAll();
}
