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

public class FailureElementConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(FailureElementConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/failure";

    private XPathExpression expr;

    private FailureElement[] currentElements;

    private FailureElement[] tmpElements;

    public FailureElementConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);

        currentElements = null;
        tmpElements = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return FailureElement.class;
    }

    public Object[] getConfigurationElement() {
        return currentElements;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {
        ArrayList<FailureElement> tmpList = new ArrayList<FailureElement>();

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element fElem = (Element) parsedElements.item(k);
            String tmps = fElem.getAttribute("fail");
            if (tmps.equalsIgnoreCase("true")) {
                logger.info("Simulating a parsing error");
                throw new CommonConfigException("Error parsing failure element");
            }
            FailureElement item = new FailureElement(false, fElem.getTextContent());
            logger.debug("Found element " + item.content);
            tmpList.add(item);
        }

        tmpElements = new FailureElement[tmpList.size()];
        tmpList.toArray(tmpElements);

        if (currentElements == null || currentElements.length != tmpElements.length) {
            logger.debug("Commit FailureElement list: true");
            return true;
        }

        boolean result = false;
        for (int k = 0; k < tmpElements.length; k++) {
            result |= !tmpElements[k].equals(currentElements[k]);
        }
        logger.debug("Commit FailureElement list: " + result);
        return result;

    }

    public boolean processTriggers() {
        return false;
    }

    public void commit() {
        currentElements = tmpElements;
        tmpElements = null;
    }

    public void rollback() {
        tmpElements = null;
    }

    public File[] getTriggers() {
        return null;
    }

    public void clean() {
    }

}
