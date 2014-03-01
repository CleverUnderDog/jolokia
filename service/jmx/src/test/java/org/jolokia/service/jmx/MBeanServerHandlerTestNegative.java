package org.jolokia.service.jmx;

/*
 * Copyright 2009-2011 Roland Huss
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.*;

import org.jolokia.server.core.service.impl.MBeanRegistry;
import org.jolokia.server.core.util.jmx.DefaultMBeanServerAccess;
import org.jolokia.server.core.util.jmx.MBeanServerAccess;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * @author roland
 * @since 02.09.11
 */
public class MBeanServerHandlerTestNegative {

    private MBeanRegistry handler;

    @Test
    public void mbeanRegistrationWithFailingTestDetector() throws JMException, IOException {
        TestDetector.setThrowAddException(true);
        // New setup because detection happens at construction time
        init();
        try {
            ObjectName oName = new ObjectName("Bla:type=blub");
            MBeanServerAccess servers = new DefaultMBeanServerAccess();
            final List<Boolean> results = new ArrayList<Boolean>();
            servers.each(oName, new MBeanServerAccess.MBeanEachCallback() {
                public void callback(MBeanServerConnection pConn, ObjectName pName)
                        throws ReflectionException, InstanceNotFoundException, IOException, MBeanException {
                    results.add(pConn.isRegistered(pName));
                }
            });
            assertTrue(results.contains(Boolean.TRUE),"MBean not registered");
        } finally {
            TestDetector.setThrowAddException(false);
            handler.destroy();
        }
    }

    // ===================================================================================================

    private void init() throws MalformedObjectNameException {
        TestDetector.reset();
        handler = new MBeanRegistry();
    }

}
