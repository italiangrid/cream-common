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

package org.glite.ce.commonj.db;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DatasourceConfigHandler extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(DatasourceConfigHandler.class.getName());

    private static final String[] DATASOURCE_ATTRIBUTES = {
        "factory", "driverClassName", "username", "password", "maxActive", "maxIdle", "maxWait", "logAbandoned",
        "removeAbandoned", "removeAbandonedTimeout", "url", "validationQuery", "testOnBorrow", "testWhileIdle",
        "timeBetweenEvictionRunsMillis", "minEvictableIdleTimeMillis"
    };

    private static final String XPATH_STRING = "/service/dataSource";

    private XPathExpression expr;

    private List<HashMap<String, String>> currentParams;

    private List<HashMap<String, String>> tmpParams;

    private HashMap<String, DataSource> currentDatasources;

    public DatasourceConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);

        currentParams = new ArrayList<HashMap<String, String>>(0);
        tmpParams = null;
        currentDatasources = null;
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return HashMap.class;
    }

    public Object[] getConfigurationElement() {
        HashMap<String, DataSource>[] datasourceMap = null;

        if (currentDatasources == null || currentDatasources.size() == 0) {
            datasourceMap = (HashMap<String, DataSource>[]) new HashMap<?, ?>[0];
        } else {
            datasourceMap = (HashMap<String, DataSource>[]) new HashMap<?, ?>[1];
            datasourceMap[0] = currentDatasources;
        }
        return datasourceMap;
    }

    public boolean process(NodeList parsedElements) throws CommonConfigException {
        Node node = null;
        HashMap<String, String> dataSourceAttributes = null;
        tmpParams = new ArrayList<HashMap<String, String>>(0);

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element dsElem = (Element) parsedElements.item(k);

            NamedNodeMap namedNodeMap = dsElem.getAttributes();
            if (namedNodeMap == null) {
                continue;
            }

            dataSourceAttributes = new HashMap<String, String>(0);

            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                node = namedNodeMap.item(i);
                dataSourceAttributes.put(node.getNodeName(), node.getNodeValue());
            }

            for (int i = 0; i < DATASOURCE_ATTRIBUTES.length; i++) {
                if (!dataSourceAttributes.containsKey(DATASOURCE_ATTRIBUTES[i])) {
                    throw new CommonConfigException("Missing data source attribute: " + DATASOURCE_ATTRIBUTES[i]);
                }
            }

            tmpParams.add(dataSourceAttributes);
        }

        boolean result = !currentParams.equals(tmpParams);
        logger.debug("Datasource configuration changed: " + result);
        return result;
    }

    public boolean processTriggers() {
        return false;
    }

    public void commit() {
        currentParams = tmpParams;
        tmpParams = null;
        currentDatasources = new HashMap<String, DataSource>(currentParams.size());

        for (HashMap<String, String> dataSourceAttribute : currentParams) {
            String name = dataSourceAttribute.remove("name");
            String type = dataSourceAttribute.remove("type");        
            String factory = dataSourceAttribute.remove("factory");    
            Reference reference = new Reference(type);
            
            for (String attributeName : dataSourceAttribute.keySet()) {
                reference.add(new StringRefAddr(attributeName, dataSourceAttribute.get(attributeName)));
            }

            try {
                ObjectFactory objFactory = (ObjectFactory) Class.forName(factory).newInstance();
                currentDatasources.put(name, (DataSource) objFactory.getObjectInstance(reference, null, null, null));
                logger.debug("new dataSource added: " + name);
            } catch (Throwable th) {
                logger.error(th.getMessage(), th);
            }

        }
    }

    public void rollback() {
        tmpParams = null;
    }

    public File[] getTriggers() {
        return null;
    }

    public void clean() {
        currentParams.clear();
        tmpParams = null;
        currentDatasources = null;
    }
}
