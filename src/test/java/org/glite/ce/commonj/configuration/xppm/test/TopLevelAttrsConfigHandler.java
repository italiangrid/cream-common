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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

public class TopLevelAttrsConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(TopLevelAttrsConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/@*";

    private XPathExpression expr;

    private TLAttributes currentAttrs;

    private TLAttributes tmpAttrs;

    public TopLevelAttrsConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
        currentAttrs = null;
        tmpAttrs = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return TLAttributes.class;
    }

    public Object[] getConfigurationElement() {
        if (currentAttrs != null) {
            Object[] result = new Object[1];
            result[0] = currentAttrs;
            return result;
        }
        return null;
    }

    public boolean process(NodeList parsedElements) {
        String tmpID = "";
        String tmpCode = "";
        for (int k = 0; k < parsedElements.getLength(); k++) {
            Attr attribute = (Attr) parsedElements.item(k);
            if (attribute.getName() == TLAttributes.ID_LABEL) {
                tmpID = attribute.getValue();
            } else if (attribute.getName() == TLAttributes.CODE_LABEL) {
                tmpCode = attribute.getValue();
            }
        }

        tmpAttrs = new TLAttributes(tmpID, tmpCode);

        logger.debug("Commit TLAttributes: " + (!tmpAttrs.equals(currentAttrs)));
        return !tmpAttrs.equals(currentAttrs);
    }

    public boolean processTriggers() {
        return false;
    }

    public void commit() {
        currentAttrs = tmpAttrs;
        tmpAttrs = null;
    }

    public void rollback() {
        tmpAttrs = null;
    }

    public File[] getTriggers() {
        return null;
    }

    public void clean() {
    }
}
