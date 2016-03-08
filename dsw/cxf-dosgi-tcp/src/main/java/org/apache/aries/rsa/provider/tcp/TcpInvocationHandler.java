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
package org.apache.aries.rsa.provider.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpInvocationHandler implements InvocationHandler {
    private String host;
    private int port;
    private ClassLoader cl;

    public TcpInvocationHandler(ClassLoader cl, String host, int port)
        throws UnknownHostException, IOException {
        this.cl = cl;
        this.host = host;
        this.port = port;

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try (
            Socket socket = new Socket(this.host, this.port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ) {
            out.writeObject(method.getName());
            out.writeObject(args);
            out.flush();
            return parseResult(socket);
        } catch (Exception  e) {
            throw new RuntimeException("Error calling " + host + ":" + port + " method: " + method.getName(), e);
        }
    }

    private Object parseResult(Socket socket) throws IOException, ClassNotFoundException, Throwable {
        try (ObjectInputStream in = new LoaderObjectInputStream(socket.getInputStream(), cl)) {
            Object result = in.readObject();
            if (result instanceof Throwable) {
                throw (Throwable)result;
            } else {
                return result;
            }
        }
    }

}
