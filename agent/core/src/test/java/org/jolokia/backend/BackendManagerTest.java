package org.jolokia.backend;

/*
 * Copyright 2009-2013 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Map;

import javax.management.*;

import org.jolokia.backend.dispatcher.RequestDispatcher;
import org.jolokia.backend.dispatcher.RequestHandler;
import org.jolokia.config.ConfigKey;
import org.jolokia.converter.Converters;
import org.jolokia.converter.JmxSerializer;
import org.jolokia.detector.ServerHandle;
import org.jolokia.request.JolokiaRequest;
import org.jolokia.request.JmxRequestBuilder;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.service.*;
import org.jolokia.util.*;
import org.json.simple.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author roland
 * @since Jun 15, 2010
 */
public class BackendManagerTest {

    private TestJolokiaContext ctx;

    private TestJolokiaContext createContext(Object ... configKeysAndValues) {
        TestJolokiaContext.Builder builder =
                new TestJolokiaContext.Builder()
                .services(JmxSerializer.class,new Converters());
        if (configKeysAndValues.length > 0) {
            builder.config(ConfigKey.DEBUG, "true");
        }
        ctx = builder.build();
        return ctx;
    }

    @Test
    public void simpleRead() throws JMException, IOException {
        BackendManager backendManager = createBackendManager(new Object[] { ConfigKey.DEBUG,"true"});
        JolokiaRequest req = new JmxRequestBuilder(RequestType.READ,"java.lang:type=Memory")
                .attribute("HeapMemoryUsage")
                .build();
        JSONObject ret = backendManager.handleRequest(req);
        assertTrue((Long) ((Map) ret.get("value")).get("used") > 0);
    }

    private BackendManager createBackendManager(Object[] pContextParams) {
        JolokiaContext ctx = createContext(pContextParams);
        return new BackendManager(ctx, new TestRequestDispatcher(ctx));
    }


    @Test
    public void lazyInit() throws JMException, IOException {
        BackendManager backendManager = createBackendManager(new Object[0]);

        JolokiaRequest req = new JmxRequestBuilder(RequestType.READ,"java.lang:type=Memory")
                .attribute("HeapMemoryUsage")
                .build();
        JSONObject ret = backendManager.handleRequest(req);
        assertTrue((Long) ((Map) ret.get("value")).get("used") > 0);
    }

    @Test
    public void defaultConfig() {
        BackendManager backendManager = createBackendManager(new Object[] {  ConfigKey.DEBUG_MAX_ENTRIES,"blabal" });
    }

    @Test
    public void doubleInit() {
        JolokiaContext ctx = createContext();
        RequestDispatcher dispatcher = new TestRequestDispatcher(ctx);
        BackendManager b1 = new BackendManager(ctx, dispatcher);
        BackendManager b2 = new BackendManager(ctx, dispatcher);
    }

    @Test
    public void remoteAccessCheck() {
        ctx = new TestJolokiaContext.Builder().restrictor(new AllowAllRestrictor()).build();
        BackendManager backendManager = new BackendManager(ctx, new TestRequestDispatcher(ctx));
        assertTrue(backendManager.isRemoteAccessAllowed("localhost", "127.0.0.1"));
    }

    @Test
    public void convertError() throws MalformedObjectNameException {
        BackendManager backendManager = createBackendManager(new Object[0]);
        Exception exp = new IllegalArgumentException("Hans",new IllegalStateException("Kalb"));
        JolokiaRequest req = new JmxRequestBuilder(RequestType.READ,"java.lang:type=Memory").build();
        JSONObject jsonError = (JSONObject) backendManager.convertExceptionToJson(exp,req);
        assertTrue(!jsonError.containsKey("stackTrace"));
        assertEquals(jsonError.get("message"), "Hans");
        assertEquals(((JSONObject) jsonError.get("cause")).get("message"), "Kalb");
    }

    // =========================================================================================

    static class RequestHandlerTest extends AbstractJolokiaService<RequestHandler> implements RequestHandler {

        static boolean called = false;

        public RequestHandlerTest(Converters pConverters, ServerHandle pServerHandle, Restrictor pRestrictor) {
            super(RequestHandler.class,1);
            assertNotNull(pConverters);
            assertNotNull(pRestrictor);
        }

        public Object handleRequest(JolokiaRequest pJmxReq) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IOException {
            called = true;
            if (pJmxReq.getType() == RequestType.READ) {
                return new JSONObject();
            } else if (pJmxReq.getType() == RequestType.WRITE) {
                return "faultyFormat";
            }
            return null;
        }

        public boolean canHandle(JolokiaRequest pJolokiaRequest) {
            return true;
        }

        public boolean useReturnValueWithPath(JolokiaRequest pJolokiaRequest) {
            return false;
        }

        public void destroy() throws JMException {
        }
    }

    // ========================================================

    static class RequestHandlerWrong extends AbstractJolokiaService<RequestHandler> implements RequestHandler {

        protected RequestHandlerWrong() {
            super(RequestHandler.class,1);
        }

        // No special constructor --> fail

        public Object handleRequest(JolokiaRequest pJmxReq) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IOException {
            return null;
        }

        public boolean canHandle(JolokiaRequest pJolokiaRequest) {
            return false;
        }

        public boolean useReturnValueWithPath(JolokiaRequest pJolokiaRequest) {
            return false;
        }

        public void destroy() throws JMException {
        }
    }
}
