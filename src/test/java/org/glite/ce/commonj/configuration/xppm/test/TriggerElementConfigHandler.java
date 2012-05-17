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
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TriggerElementConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(TriggerElementConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/trigger";

    private XPathExpression expr;

    private TriggerElement[] currElements;

    private TriggerElement[] tmpElements;

    public TriggerElementConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
        currElements = null;
        tmpElements = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return TriggerElement.class;
    }

    public Object[] getConfigurationElement() {
        return currElements;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {
        ArrayList<TriggerElement> tmpList = new ArrayList<TriggerElement>();

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element tElem = (Element) parsedElements.item(k);
            try {
                TriggerElement item = new TriggerElement(tElem.getAttribute("file"));
                logger.debug("Found trigger element: " + item.getContent());
                tmpList.add(item);
            } catch (IOException ioEx) {
                throw new CommonConfigException(ioEx.getMessage());
            }
        }

        tmpElements = new TriggerElement[tmpList.size()];
        tmpList.toArray(tmpElements);
        boolean result = tmpElements != currElements;
        logger.debug("Commit TriggerElement list: " + result);
        return result;
    }

    public boolean processTriggers()
        throws CommonConfigException {
        ArrayList<TriggerElement> tmpList = new ArrayList<TriggerElement>();

        for (TriggerElement oldElem : currElements) {
            try {
                TriggerElement newElem = new TriggerElement(oldElem.getFilename());
                tmpList.add(newElem);
            } catch (IOException ioEx) {
                throw new CommonConfigException(ioEx.getMessage());
            }
        }

        tmpElements = new TriggerElement[tmpList.size()];
        tmpList.toArray(tmpElements);
        boolean result = tmpElements != currElements;
        logger.debug("Commit TriggerElement list: " + result);
        return result;
    }

    public void commit() {
        currElements = tmpElements;
        tmpElements = null;
    }

    public void rollback() {
        tmpElements = null;
    }

    public File[] getTriggers() {
        ArrayList<File> triggers = new ArrayList<File>();

        for (TriggerElement item : currElements) {
            logger.debug("Detected trigger: " + item.getFilename());
            triggers.add(new File(item.getFilename()));
        }

        File[] result = new File[triggers.size()];
        triggers.toArray(result);
        return result;
    }

    public void clean() {
    }

}
