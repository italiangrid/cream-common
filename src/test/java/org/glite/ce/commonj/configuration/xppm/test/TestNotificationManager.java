/*
 * Copyright (c) Members of the EGEE Collaboration. 2004. 
 * See http://www.eu-egee.org/partners/ for details on the copyright
 * holders.  
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package org.glite.ce.commonj.configuration.xppm.test;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.glite.ce.commonj.configuration.xppm.ConfigurationEvent;
import org.glite.ce.commonj.configuration.xppm.ConfigurationListener;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;

import junit.framework.TestCase;

public class TestNotificationManager
    extends TestCase {

    private static Logger logger = Logger.getLogger(TestNotificationManager.class.getName());

    private ConfigFile confFile;

    private GenericConfigListener listener;

    public TestNotificationManager(String name) {
        super(name);

        confFile = new ConfigFile();
        listener = new GenericConfigListener();

        LogUtils.setup();

    }

    protected void setUp()
        throws Exception {

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[1];
        elements[0] = new FailureElement(false, "failure content");
        confFile.write(attributes, elements, false);

        listener.reset();
    }

    protected void tearDown() {
        confFile.delete();
    }

    public void testNotificationAtStartup()
        throws Exception {

        ConfigurationListener[] lsnrs = { listener };
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 10000, lsnrs);

        Thread.sleep(6000);

        boolean result = true;
        for (ConfigurationEvent event : listener) {
            if (event.getType() != ConfigurationEvent.CREATED_CONFIG) {
                result = false;
                break;
            }
        }
        assertTrue(result);
    }

    public void testNotificationForInsert()
        throws Exception {

        ConfigurationListener[] lsnrs = { listener };
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 3000, lsnrs);

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");
        elements[1] = new FailureElement(false, "failure content");
        confFile.write(attributes, elements, false);

        Thread.sleep(6000);

        ConfigurationEvent lastEvn = listener.getLastEvent();
        logger.debug("Last event category " + lastEvn.getCategory().getName());
        boolean result = lastEvn != null;
        result &= lastEvn.getCategory() == SimpleElement.class;
        result &= lastEvn.getType() == ConfigurationEvent.CREATED_CONFIG;

        assertTrue(result);

    }
    
    public void testNotificationForUpdate()
            throws Exception {
        
        ConfigurationListener[] lsnrs = { listener };
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 3000, lsnrs);

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[1];
        elements[0] = new FailureElement(false, "new failure content");
        confFile.write(attributes, elements, false);

        Thread.sleep(6000);

        
        ConfigurationEvent lastEvn = listener.getLastEvent();
        logger.debug("Last event category " + lastEvn.getCategory().getName());
        boolean result = lastEvn != null;
        result &= lastEvn.getCategory() == FailureElement.class;
        result &= lastEvn.getType() == ConfigurationEvent.UPDATED_CONFIG;
        
        assertTrue(result);
    }
    
    public void testNotificationForRemove()
        throws Exception {
        
        ConfigurationListener[] lsnrs = { listener };
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 3000, lsnrs);

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[0];
        confFile.write(attributes, elements, false);

        Thread.sleep(6000);

        
        ConfigurationEvent lastEvn = listener.getLastEvent();
        logger.debug("Last event category " + lastEvn.getCategory().getName());
        boolean result = lastEvn != null;
        result &= lastEvn.getCategory() == FailureElement.class;
        result &= lastEvn.getType() == ConfigurationEvent.DELETED_CONFIG;
        
        assertTrue(result);
        
    }

    public void testShutdown()
        throws Exception {

        ConfigurationListener[] lsnrs = { listener };
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 3000, lsnrs);

        LogUtils.startCollect(ConfigurationManager.class.getName(), "ConfigurationNotifier halted");
        Thread.sleep(1000);

        long ts = System.currentTimeMillis();
        cMan.shutdown();

        Thread.sleep(5000);

        LoggingEvent[] collectedEvents = LogUtils.getLoggerSnapshot();
        if (collectedEvents.length == 0) {
            fail("No shutdown event logged");
            return;
        }

        long shutdownTime = collectedEvents[0].timeStamp - ts;

        assertTrue(shutdownTime < 1000);
    }

}
