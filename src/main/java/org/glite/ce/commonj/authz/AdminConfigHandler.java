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

package org.glite.ce.commonj.authz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AdminConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(AdminConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/adminlist";

    private static String FILENAME_ATTR = "filename";

    private XPathExpression expr;

    private AdminTable currentTable;

    private AdminTable tmpTable;

    private String currFilename;

    private String tmpFilename;

    public AdminConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {
        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);
    }

    public XPathExpression getXPath() {
        return expr;
    }

    public Class<?> getCategory() {
        return AdminTable.class;
    }

    public Object[] getConfigurationElement() {
        if (currentTable != null) {
            Object[] result = new Object[1];
            result[0] = currentTable;
            return result;
        }
        return null;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {

        Element adminElem = (Element) parsedElements.item(0);
        if (adminElem == null) {
            throw new CommonConfigException("Missing adminlist element");
        }

        tmpFilename = adminElem.getAttribute(FILENAME_ATTR);
        tmpTable = readAdminTable(tmpFilename);

        return !tmpTable.equals(currentTable);
    }

    public boolean processTriggers()
        throws CommonConfigException {
        tmpTable = readAdminTable(currFilename);
        return !tmpTable.equals(currentTable);
    }

    public void commit() {
        currentTable = tmpTable;
        currFilename = tmpFilename;
        tmpTable = null;
    }

    public void rollback() {
        tmpTable = null;
    }

    public File[] getTriggers() {
        if (currFilename != null) {
            File[] result = new File[1];
            result[0] = new File(currFilename);
            return result;
        }
        return null;
    }

    public void clean() {

    }

    private AdminTable readAdminTable(String filename)
        throws CommonConfigException {
        AdminTable result = new AdminTable();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();

                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.startsWith("\"") && line.endsWith("\""))
                        line = line.substring(1, line.length() - 1);
                    result.add(line);
                    logger.debug("Registered DN: " + line);
                }
                line = reader.readLine();
            }
        } catch (IOException ioEx) {

            throw new CommonConfigException(ioEx.getMessage(), ioEx);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {

        org.apache.log4j.PropertyConfigurator.configure("/tmp/log4j.properties");

        try {
            ConfigurationManager cMan = new ConfigurationManager(args[0]);
            Object[] tmpo = cMan.getConfigurationElements(AdminTable.class);
            if (tmpo.length > 0) {
                AdminTable table = (AdminTable) tmpo[0];
                for (String key : table) {
                    logger.info("Found " + key);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
