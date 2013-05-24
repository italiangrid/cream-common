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

package org.glite.ce.commonj.authz.argus;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ArgusConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(ArgusConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/argus-pep";

    private static String PAP_ENDPOINT_TAG = "endpoint";

    private static final String PAP_URL_ATTR = "url";

    private static final String CERT_FILE_ATTR = "cert";

    private static final String KEY_FILE_ATTR = "key";

    private static final String PWD_ATTR = "passwd";

    private static final String TIMEOUT_ATTR = "timeout";

    private static final String CONN_HOST_ATTR = "connection_per_host";

    private static final String MAX_CONN_ATTR = "max_connection";

    private static final String RES_ID_ATTR = "resource_id";

    private static final String MAPPING_ATTR = "mapping_class";

    private XPathExpression expr;

    private PEPConfigurationItem currConfig;

    private PEPConfigurationItem tmpConfig;

    public ArgusConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);

        currConfig = null;
        tmpConfig = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return PEPConfigurationItem.class;
    }

    public Object[] getConfigurationElement() {
        if (currConfig != null) {
            Object[] result = new Object[1];
            result[0] = currConfig;
            return result;
        }
        return null;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {

        Element argusElem = (Element) parsedElements.item(0);
        if (argusElem == null) {
            throw new CommonConfigException("Missing argus-pep element");
        }

        tmpConfig = new PEPConfigurationItem();

        String tmps = argusElem.getAttribute(RES_ID_ATTR);
        if (tmps == "") {
            throw new CommonConfigException("Missing mandatory attribute: " + RES_ID_ATTR);
        }

        tmpConfig.setResourceID(tmps);

        tmps = argusElem.getAttribute(MAPPING_ATTR);
        if (tmps == "") {
            throw new CommonConfigException("Missing mandatory attribute: " + MAPPING_ATTR);
        }

        tmpConfig.setMappingClass(tmps);

        tmps = argusElem.getAttribute(CERT_FILE_ATTR);
        if (tmps == "") {
            throw new CommonConfigException("Missing mandatory attribute: " + CERT_FILE_ATTR);
        }

        tmpConfig.setCertificatePath(tmps);

        tmps = argusElem.getAttribute(KEY_FILE_ATTR);
        if (tmps == "") {
            throw new CommonConfigException("Missing mandatory attribute: " + KEY_FILE_ATTR);
        }

        tmpConfig.setKeyPath(tmps);

        tmps = argusElem.getAttribute(PWD_ATTR);
        tmpConfig.setPassword(tmps);

        String timeoutStr = argusElem.getAttribute(TIMEOUT_ATTR);
        try {
            tmpConfig.setConnectionTimeout(Integer.parseInt(timeoutStr));
        } catch (Exception ex) {
            logger.warn("Missing or wrong argument " + TIMEOUT_ATTR + "; default value used");
        }

        String connHostStr = argusElem.getAttribute(CONN_HOST_ATTR);
        try {
            tmpConfig.setMaxConnectionsPerHost(Integer.parseInt(connHostStr));
        } catch (Exception ex) {
            logger.warn("Missing or wrong argument " + CONN_HOST_ATTR + "; default value used");
        }

        String maxConnStr = argusElem.getAttribute(MAX_CONN_ATTR);
        try {
            tmpConfig.setMaxTotalConnections(Integer.parseInt(maxConnStr));
        } catch (Exception ex) {
            logger.warn("Missing or wrong argument " + MAX_CONN_ATTR + "; default value used");
        }

        NodeList papElemList = argusElem.getElementsByTagName(PAP_ENDPOINT_TAG);
        for (int k = 0; k < papElemList.getLength(); k++) {
            Element papElement = (Element) papElemList.item(k);
            String papEP = papElement.getAttribute(PAP_URL_ATTR);
            tmpConfig.addEndpoint(papEP);
        }

        boolean result = !tmpConfig.equals(currConfig);
        logger.debug("Argus configuration changed: " + result);
        return result;

    }

    public boolean processTriggers() {
        return false;
    }

    public void commit() {
        if (!tmpConfig.equals(currConfig)) {
            currConfig = tmpConfig;
            tmpConfig = null;
        }
    }

    public void rollback() {
        tmpConfig = null;
    }

    public File[] getTriggers() {
        return null;
    }

    public void clean() {
        currConfig = null;
    }

}
