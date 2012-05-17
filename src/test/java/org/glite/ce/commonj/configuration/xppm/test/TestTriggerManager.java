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

import java.io.File;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.xppm.ConfigurationEvent;
import org.glite.ce.commonj.configuration.xppm.ConfigurationListener;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;

import junit.framework.TestCase;

public class TestTriggerManager
    extends TestCase {

    private static Logger logger = Logger.getLogger(TestTriggerManager.class.getName());

    private ConfigFile confFile;

    private String triggerFile1;

    public TestTriggerManager(String name) {
        super(name);

        confFile = new ConfigFile();
        triggerFile1 = confFile.getDirectory() + File.separator + "trigger1.txt";

        LogUtils.setup();

    }

    protected void setUp()
        throws Exception {

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");

        elements[1] = new TriggerElement(triggerFile1, "trigger content");

        confFile.write(attributes, elements, false);

    }

    protected void tearDown() {
        confFile.delete();
    }

    public void testTriggerOnReloading()
        throws Exception {

        String contentStr = "new trigger content";

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());

        Object[] tmpo = cMan.getConfigurationElements(TriggerElement.class);

        Thread.sleep(2000);

        new TriggerElement(triggerFile1, contentStr);

        Thread.sleep(12000);

        tmpo = cMan.getConfigurationElements(TriggerElement.class);
        if (tmpo.length == 0) {
            fail("Missing trigger element");
            return;
        }

        TriggerElement resElem = (TriggerElement) tmpo[0];
        String resContent = resElem.getContent();

        assertTrue(resContent.equals(contentStr));

    }

    public void testTriggerAndAttributeReloading()
        throws Exception {

        String contentStr = "new trigger content";

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());

        Object[] tmpo = cMan.getConfigurationElements(TriggerElement.class);

        Thread.sleep(2000);

        TLAttributes attributes = new TLAttributes("new-id-value", "new-code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");

        elements[1] = new TriggerElement(triggerFile1, contentStr);

        confFile.write(attributes, elements, false);

        Thread.sleep(12000);

        tmpo = cMan.getConfigurationElements(TriggerElement.class);
        if (tmpo.length == 0) {
            fail("Missing trigger element");
            return;
        }

        TriggerElement resElem = (TriggerElement) tmpo[0];
        String resContent = resElem.getContent();

        tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
            return;
        }

        TLAttributes attrs = (TLAttributes) tmpo[0];
        assertTrue(resContent.equals(contentStr) && attrs.id.equals("new-id-value"));

    }

    public void testTriggerOnRollBack()
        throws Exception {

        String contentStr = "new trigger content";

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());

        Object[] tmpo = cMan.getConfigurationElements(TriggerElement.class);

        Thread.sleep(2000);

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[2];

        elements[0] = new TriggerElement(triggerFile1, contentStr);
        elements[1] = new FailureElement(true, "failure content");

        confFile.write(attributes, elements, false);

        Thread.sleep(12000);

        tmpo = cMan.getConfigurationElements(TriggerElement.class);
        if (tmpo.length == 0) {
            fail("Missing trigger element");
            return;
        }

        TriggerElement resElem = (TriggerElement) tmpo[0];
        String resContent = resElem.getContent();

        assertFalse(resContent.equals(contentStr));

    }

    public void testNotificationFromTrigger()
        throws Exception {

        String contentStr = "new trigger content";
        GenericConfigListener listener = new GenericConfigListener();
        ConfigurationListener[] lsnrs = { listener };

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename(), 10000, lsnrs);

        Object[] tmpo = cMan.getConfigurationElements(TriggerElement.class);

        Thread.sleep(1000);

        new TriggerElement(triggerFile1, contentStr);

        Thread.sleep(12000);

        tmpo = cMan.getConfigurationElements(TriggerElement.class);
        if (tmpo.length == 0) {
            fail("Missing trigger element");
            return;
        }

        Thread.sleep(1000);

        ConfigurationEvent lastEvn = listener.getLastEvent();
        logger.debug("Last event category " + lastEvn.getCategory().getName());
        boolean result = lastEvn != null;
        result &= lastEvn.getCategory() == TriggerElement.class;
        result &= lastEvn.getType() == ConfigurationEvent.UPDATED_CONFIG;

        assertTrue(result);
    }
}
