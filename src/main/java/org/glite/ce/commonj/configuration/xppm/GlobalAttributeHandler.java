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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

public class GlobalAttributeHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(GlobalAttributeHandler.class.getName());

    private static final String XPATH_STRING = "/service/@*";

    private XPathExpression expr;

    private GlobalAttributes currentAttrs;

    private GlobalAttributes tmpAttrs;

    public GlobalAttributeHandler() throws XPathExpressionException, XPathFactoryConfigurationException {

        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
        currentAttrs = new GlobalAttributes();
        tmpAttrs = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return GlobalAttributes.class;
    }

    public Object[] getConfigurationElement() {
        Object[] result = new Object[1];
        result[0] = currentAttrs;
        return result;
    }

    public boolean process(NodeList parsedElements) {
        tmpAttrs = new GlobalAttributes();
        for (int k = 0; k < parsedElements.getLength(); k++) {
            Attr attribute = (Attr) parsedElements.item(k);
            tmpAttrs.put(attribute.getName(), attribute.getValue());
            logger.debug("Found attribute: " + attribute.getName() + " = " + attribute.getValue());
        }

        boolean result = !tmpAttrs.equals(currentAttrs);
        logger.debug("GlobalAttributes changed: " + result);
        return result;
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
        currentAttrs.clear();
    }

    public static void main(String[] args) {

        org.apache.log4j.PropertyConfigurator.configure("/tmp/log4j.properties");

        try {
            ConfigurationManager cMan = new ConfigurationManager(args[0]);
            Object[] tmpo = cMan.getConfigurationElements(GlobalAttributes.class);
            if (tmpo.length > 0) {
                GlobalAttributes attrs = (GlobalAttributes) tmpo[0];
                for (String key : attrs.keySet()) {
                    logger.info("Found " + key + " = " + attrs.get(key));
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
