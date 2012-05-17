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

package org.glite.ce.commonj.listeners;

import java.io.FileReader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class CommonContextListener
    implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(CommonContextListener.class.getName());

    private static String serviceConfFilename = null;

    private static String logConfFilename = null;

    public void contextInitialized(ServletContextEvent event) {

        ServletContext context = event.getServletContext();
        if (context.getRealPath("/") == null) {
            logger.error("Cannot retrieve servlet workarea");
            return;
        }

        String axis2XmlPath = context.getInitParameter("axis2.xml.path");
        if (axis2XmlPath == null) {
            axis2XmlPath = "WEB-INF/conf/axis2.xml";
        }
        String axis2ConfFile = context.getRealPath("/") + axis2XmlPath;
        logger.info("Reading axis2 configuration from " + axis2ConfFile);

        FileReader xmlReader = null;
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            SAXParser sp = spf.newSAXParser();

            xmlReader = new FileReader(axis2ConfFile);
            InputSource input = new InputSource(xmlReader);
            input.setSystemId("file://" + axis2ConfFile);

            sp.parse(input, new LocalConfParser());

        } catch (Throwable th) {

            logger.error(th.getMessage(), th);

        } finally {
            if (xmlReader != null)
                try {
                    xmlReader.close();
                } catch (Exception ex) {
                }
        }

        if (logConfFilename != null) {
            try {
                LogManager.resetConfiguration();
                PropertyConfigurator.configure(logConfFilename);
            }catch(Throwable th){
                logger.error(th.getMessage(), th);
                LogManager.resetConfiguration();
            }
        }

        logger.debug("Context intialized");

    }

    public void contextDestroyed(ServletContextEvent event) {

        logger.debug("Context destroyed");

    }

    public static String getConfigPath() {
        return serviceConfFilename;
    }

    private class LocalConfParser
        extends DefaultHandler {

        private String currentParam = null;

        private String currentText = null;

        public void startElement(String uri, String name, String qName, Attributes attributes)
            throws SAXParseException {

            if (qName.equals("parameter")) {
                currentParam = attributes.getValue("name");
                currentText = null;
            }
        }

        public void endElement(String uri, String name, String qName)
            throws SAXParseException {

            if (qName.equals("parameter")) {

                if (currentParam.equals("serviceConfigurationFile")) {
                    serviceConfFilename = currentText;
                    logger.debug("Found  service configuration path " + currentText);
                }

                if (currentParam.equals("log4jConfigurationFile")) {
                    logConfFilename = currentText;
                    logger.debug("Found  log4jconfiguration path " + currentText);
                }

            }
        }

        public void characters(char[] ch, int start, int length)
            throws SAXParseException {

            currentText = new String(ch, start, length);

        }
    }
}
