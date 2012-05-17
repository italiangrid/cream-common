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
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SimpleElementConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(SimpleElementConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/simple";

    private XPathExpression expr;

    private SimpleElement[] currElements;

    private SimpleElement[] tmpElements;

    public SimpleElementConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
        currElements = null;
        tmpElements = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return SimpleElement.class;
    }

    public Object[] getConfigurationElement() {
        return currElements;
    }

    public boolean process(NodeList parsedElements) {

        ArrayList<SimpleElement> tmpList = new ArrayList<SimpleElement>();

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element sElem = (Element) parsedElements.item(k);
            SimpleElement item = new SimpleElement(sElem.getAttribute("attribute"), sElem.getTextContent());
            logger.debug("Found element " + item.content);
            tmpList.add(item);
        }

        tmpElements = new SimpleElement[tmpList.size()];
        tmpList.toArray(tmpElements);
        boolean result = tmpElements != currElements;
        logger.debug("Commit SimpleElement list: " + result);
        return result;
    }

    public boolean processTriggers() {
        return false;
    }

    public void commit() {
        currElements = tmpElements;
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
