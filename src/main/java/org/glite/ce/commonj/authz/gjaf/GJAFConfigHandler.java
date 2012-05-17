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

package org.glite.ce.commonj.authz.gjaf;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*
 * TODO as soon as the new schema for the configfile is available
 * the xpath expression will be changed
 */
public class GJAFConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(GJAFConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/authzchain";

    private static final String NAME_ATTR = "name";

    private static String PLUGIN_TAG = "plugin";

    private static final String PLUGIN_NAME_ATTR = "name";

    private static final String PLUGIN_CLASSNAME_ATTR = "classname";

    private static String PARAM_TAG = "parameter";

    private static final String PARAM_NAME_ATTR = "name";

    private static final String PARAM_VALUE_ATTR = "value";

    private XPathExpression expr;

    private ServiceAuthorizationChain currentChain;

    private ServiceAuthorizationChain tmpChain;

    public GJAFConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return ServiceAuthorizationInterface.class;
    }

    public Object[] getConfigurationElement() {
        if (currentChain != null) {
            Object[] result = new Object[1];
            result[0] = currentChain;
            return result;
        }
        return null;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {

        ArrayList<ServiceInterceptor> plugins = new ArrayList<ServiceInterceptor>();

        Element authzElem = (Element) parsedElements.item(0);
        if (authzElem == null) {
            throw new CommonConfigException("Missing authzchain element ");
        }

        String chainName = authzElem.getAttribute(NAME_ATTR);
        if (chainName == "") {
            throw new CommonConfigException("Missing " + NAME_ATTR);
        }

        NodeList plugElemList = authzElem.getElementsByTagName(PLUGIN_TAG);
        for (int k = 0; k < plugElemList.getLength(); k++) {
            Element plugElement = (Element) plugElemList.item(k);

            String plugName = plugElement.getAttribute(PLUGIN_NAME_ATTR);
            if (plugName == "") {
                throw new CommonConfigException("Missing " + PLUGIN_NAME_ATTR);
            }

            String plugClassName = plugElement.getAttribute(PLUGIN_CLASSNAME_ATTR);
            if (plugClassName == "") {
                throw new CommonConfigException("Missing " + PLUGIN_CLASSNAME_ATTR + " for " + plugName);
            }

            ServiceInterceptor currentPlugin = createInterceptor(plugClassName, plugName);

            NodeList paramElemList = plugElement.getElementsByTagName(PARAM_TAG);
            for (int j = 0; j < paramElemList.getLength(); j++) {
                Element paraElement = (Element) paramElemList.item(j);

                String paraName = paraElement.getAttribute(PARAM_NAME_ATTR);
                String paraValue = paraElement.getAttribute(PARAM_VALUE_ATTR);

                try {

                    currentPlugin.setProperty(paraName, paraValue);

                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    throw new CommonConfigException("Error setting properties for " + plugName);
                }

            }

            plugins.add(currentPlugin);
        }

        tmpChain = new ServiceAuthorizationChain();
        tmpChain.initialize(chainName, plugins);

        return !tmpChain.equals(currentChain);

    }

    public boolean processTriggers()
        throws CommonConfigException {

        if (currentChain == null) {
            return false;
        }

        ArrayList<ServiceInterceptor> newPlugins = new ArrayList<ServiceInterceptor>();

        for (ServiceInterceptor plugin : currentChain.getInterceptors()) {
            logger.debug("Reloading interceptor: " + plugin.getId());
            ServiceInterceptor newPlugin = createInterceptor(plugin.getClass().getName(), plugin.getId());

            try {
                for (String prop : plugin.getProperties()) {
                    newPlugin.setProperty(prop, plugin.getProperty(prop));
                }

                newPlugins.add(newPlugin);

            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new CommonConfigException("Error setting properties for " + plugin.getId());
            }
        }

        tmpChain = new ServiceAuthorizationChain();
        tmpChain.initialize(currentChain.getId(), newPlugins);

        return true;
    }

    public void commit() {
        currentChain = tmpChain;
        tmpChain = null;
    }

    public void rollback() {
        tmpChain = null;
    }

    public File[] getTriggers() {
        if (currentChain != null) {
            return currentChain.getAllTriggers();
        }
        return null;
    }

    public void clean() {
    }

    private ServiceInterceptor createInterceptor(String plugClassName, String plugName)
        throws CommonConfigException {

        ServiceInterceptor interceptor = null;

        try {

            Class<?> interceptorClass = Class.forName(plugClassName);
            Class<?>[] constrArgClass = new Class<?>[] { String.class };
            Constructor<?> constr = interceptorClass.getConstructor(constrArgClass);
            Object[] constrArgValue = new Object[] { plugName };
            interceptor = (ServiceInterceptor) constr.newInstance(constrArgValue);

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new CommonConfigException("Cannot initialize " + plugName);
        }

        return interceptor;

    }
}
