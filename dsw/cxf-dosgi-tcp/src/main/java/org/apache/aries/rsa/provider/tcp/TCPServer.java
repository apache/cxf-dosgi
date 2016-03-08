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

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPServer implements Closeable, Runnable {
    private Logger log = LoggerFactory.getLogger(TCPServer.class);
    private ServerSocket serverSocket;
    private Object service;
    private boolean running;
    private ExecutorService executor;

    public TCPServer(Object service, String localip, Integer port, int numThreads) {
        this.service = service;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.running = true;
        this.executor = Executors.newCachedThreadPool();
        for (int c = 0; c < numThreads; c++) {
            this.executor.execute(this);
        }
    }
    
    int getPort() {
        return this.serverSocket.getLocalPort();
    }

    public void run() {
        ClassLoader serviceCL = service.getClass().getClassLoader();
        while (running) {
            try (
                Socket socket = this.serverSocket.accept();
                ObjectInputStream ois = new LoaderObjectInputStream(socket.getInputStream(), serviceCL);
                ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream())
                ) {
                String methodName = (String)ois.readObject();
                Object[] args = (Object[])ois.readObject();
                Object result = invoke(methodName, args);
                objectOutput.writeObject(result);
            } catch (SocketException e) {
                running = false;
            } catch (Exception e) {
                log.warn("Error processing service call.", e);
            }
        }
    }

    private Object invoke(String methodName, Object[] args)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?>[] parameterTypesAr = getTypes(args);
        Method method = service.getClass().getMethod(methodName, parameterTypesAr);
        try {
            return method.invoke(service, args);
        } catch (Throwable e) {
            return e;
        }
    }

    private Class<?>[] getTypes(Object[] args) {
        List<Class<?>> parameterTypes = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                parameterTypes.add(arg.getClass());
            }
        }
        Class<?>[] parameterTypesAr = parameterTypes.toArray(new Class[]{});
        return parameterTypesAr;
    }

    @Override
    public void close() throws IOException {
        this.serverSocket.close();
        this.running = false;
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        this.executor.shutdownNow();
    }

}
