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

package org.glite.ce.commonj.configuration.xppm;

import java.io.File;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.glite.ce.commonj.configuration.CommonConfigException;
import org.w3c.dom.NodeList;

public abstract class ConfigurationHandler {

    private static final int ENABLED = 0;

    private static final int DISABLED = 1;

    private static final int SCHEDULED_ADD = 2;

    private static final int SCHEDULED_DEL = 3;

    private static final int SCHEDULED_UP = 4;
    
    protected static XPathFactory getXPathFactory() throws XPathFactoryConfigurationException {
        
        return XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        
    }

    private int status;

    public abstract XPathExpression getXPath();

    public abstract Class<?> getCategory();

    public abstract Object[] getConfigurationElement();

    public abstract boolean process(NodeList parsedElements) throws CommonConfigException;

    public abstract boolean processTriggers() throws CommonConfigException;

    public abstract void commit();

    public abstract void rollback();

    public abstract File[] getTriggers();

    public abstract void clean();

    public void enable() {
        status = ENABLED;
    }

    void disable() {
        clean();
        status = DISABLED;
    }

    void scheduleForAdd() {
        status = SCHEDULED_ADD;
    }

    void scheduleForDelete() {
        status = SCHEDULED_DEL;
    }

    void scheduleForUpdate() {
        status = SCHEDULED_UP;
    }

    boolean isEnabled() {
        return status == ENABLED;
    }

    boolean isScheduledForAdd() {
        return status == SCHEDULED_ADD;
    }

    boolean isScheduledForDelete() {
        return status == SCHEDULED_DEL;
    }

    boolean isScheduledForUpdate() {
        return status == SCHEDULED_UP;
    }

}
