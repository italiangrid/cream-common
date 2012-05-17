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

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;

public class TestConfigurationManager
    extends TestCase {

    private static Logger logger = Logger.getLogger(TestConfigurationManager.class.getName());

    private ConfigFile confFile;

    public TestConfigurationManager(String name) {
        super(name);

        confFile = new ConfigFile();

        LogUtils.setup();

    }

    protected void setUp()
        throws Exception {

        TLAttributes attributes = new TLAttributes("id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");
        elements[1] = new FailureElement(false, "failure content");
        confFile.write(attributes, elements, false);

    }

    protected void tearDown() {
        confFile.delete();
    }

    public void testReadSimpleConfigElement()
        throws Exception {

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());
        Object[] tmpo = cMan.getConfigurationElements(SimpleElement.class);
        if (tmpo.length > 0) {
            SimpleElement sElement = (SimpleElement) tmpo[0];
            assertTrue(sElement.value.equals("myattr") && sElement.content.trim().equals("mycontent"));
        } else {
            fail("Missing simple element");
        }
        assertTrue(true);
    }

    public void testTopLevelAttribute()
        throws Exception {

        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());
        Object[] tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length > 0) {
            TLAttributes attrs = (TLAttributes) tmpo[0];
            assertTrue(attrs.id.equals("id-value"));
        } else {
            fail("Missing attribute id");
        }
    }

    public void testAttributeReloading()
        throws Exception {
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());
        Object[] tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
            return;
        }

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("new-id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");
        elements[1] = new FailureElement(false, "failure content");
        confFile.write(attributes, elements, false);

        Thread.sleep(6000);

        tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
        } else {
            TLAttributes attrs = (TLAttributes) tmpo[0];
            assertTrue(attrs.id.equals("new-id-value"));
        }

    }

    public void testParserErrorOnReloading()
        throws Exception {
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());
        Object[] tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
            return;
        }

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("new-id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");
        elements[1] = new FailureElement(false, "failure content");
        /*
         * TODO catch the exception using a custom Log4j Appender
         */
        confFile.write(attributes, elements, true);

        Thread.sleep(6000);

        tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
        } else {
            TLAttributes attrs = (TLAttributes) tmpo[0];
            assertTrue(attrs.id.equals("id-value"));
        }
    }

    public void testAttributeOnRollBack()
        throws Exception {
        ConfigurationManager cMan = new ConfigurationManager(confFile.getFilename());
        Object[] tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
            return;
        }

        Thread.sleep(6000);

        TLAttributes attributes = new TLAttributes("new-id-value", "code-value");
        MockElement[] elements = new MockElement[2];
        elements[0] = new SimpleElement("myattr", "mycontent");
        elements[1] = new FailureElement(true, "failure content");
        confFile.write(attributes, elements, false);

        Thread.sleep(6000);

        tmpo = cMan.getConfigurationElements(TLAttributes.class);
        if (tmpo.length == 0) {
            fail("Missing attribute id");
        } else {
            TLAttributes attrs = (TLAttributes) tmpo[0];
            assertTrue(attrs.id.equals("id-value"));
        }
    }

}
