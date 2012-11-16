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

/*
 * 
 * Authors: Paolo Andreetto, <paolo.andreetto@pd.infn.it>
 *
 */

package org.glite.ce.commonj.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.AdminTable;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;
import org.glite.ce.commonj.configuration.xppm.GlobalAttributes;
import org.glite.ce.commonj.listeners.CommonContextListener;

public class CommonServiceConfig {

    private static Logger logger = Logger.getLogger(CommonServiceConfig.class.getName());

    protected ConfigurationManager confManager;

    private File configFile;

    protected CommonServiceConfig() throws CommonConfigException {
        String configFilename = CommonContextListener.getConfigPath();

        if (configFilename == null && getSysPropertyName() != null) {
            configFilename = System.getProperty(getSysPropertyName());
        }

        if (configFilename == null) {
            throw new CommonConfigException("Missing configuration source parameters");
        }

        try {
            configFile = new File(configFilename);

            if (!configFile.isFile() || !configFile.canRead()) {
                throw new CommonConfigException("Cannot read configuration file");
            }

            confManager = new ConfigurationManager(configFile.getCanonicalPath());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            throw new CommonConfigException("Wrong configuration file path");
        }

    }

    protected String getSysPropertyName() {
        return null;
    }

    public String getGlobalAttributeAsString(String name) {
        Object[] tmpo = confManager.getConfigurationElements(GlobalAttributes.class);
        if (tmpo.length > 0) {
            String attrString = ((GlobalAttributes) tmpo[0]).get(name);
            if (attrString != null) {
                return attrString;
            }
        }

        return "";
    }

    public String getGlobalAttributeAsString(String name, String defValue) {
        String result = getGlobalAttributeAsString(name);
        if (result.length() == 0) {
            return defValue;
        }
        return result;
    }

    public int getGlobalAttributeAsInt(String name, int defValue) {
        Object[] tmpo = confManager.getConfigurationElements(GlobalAttributes.class);
        if (tmpo.length > 0) {
            GlobalAttributes attrs = (GlobalAttributes) tmpo[0];
            try {
                return Integer.parseInt(attrs.get(name));
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }

        return defValue;

    }

    public long getGlobalAttributeAsLong(String name, long defValue) {
        Object[] tmpo = confManager.getConfigurationElements(GlobalAttributes.class);
        if (tmpo.length > 0) {
            GlobalAttributes attrs = (GlobalAttributes) tmpo[0];
            try {
                return Long.parseLong(attrs.get(name));
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }

        return defValue;

    }

    public ServiceAuthorizationInterface getAuthorizationConfig() {
        Object[] tmpo = confManager.getConfigurationElements(ServiceAuthorizationInterface.class);
        if (tmpo.length > 0) {
            ServiceAuthorizationInterface authz = (ServiceAuthorizationInterface) tmpo[0];
            return authz;
        }

        return null;
    }

    public AdminTable getAdminTable() {
        Object[] tmpo = confManager.getConfigurationElements(AdminTable.class);
        if (tmpo.length > 0) {
            AdminTable adminTable = (AdminTable) tmpo[0];
            return adminTable;
        }

        return null;
    }

    public String getDistributionInfo() {
        StringBuffer result = new StringBuffer("EMI version: ");
        String distrInfoFilename = this.getGlobalAttributeAsString("distribution_filename");
        if (distrInfoFilename == "") {
            distrInfoFilename = System.getProperty("distribution_filename", "");
        }
        if (distrInfoFilename == "") {
            distrInfoFilename = "/etc/emi-version";
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(distrInfoFilename));
            String tmps = reader.readLine();
            if (tmps != null) {
                result.append(tmps.trim());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.append("unknown");
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return result.toString();
    }

    public String getConfigurationPath() {
        try {
            return configFile.getCanonicalPath();
        } catch (Exception ex) {
            return "";
        }
    }

    public String getConfigurationDirectory() {
        return configFile.getParent();
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, DataSource> getDataSources() {
        HashMap<String, DataSource> dataSources = null;

        Object[] dataSourcesArray = confManager.getConfigurationElements(HashMap.class);

        if (dataSourcesArray != null && dataSourcesArray.length > 0) {
            dataSources = (HashMap<String, DataSource>) dataSourcesArray[0];
        }

        return dataSources;
    }

    public void destroy() {
        if (confManager != null) {
            confManager.shutdown();
        }
    }

    public void finalize() {
        if (confManager != null) {
            confManager.shutdown();
        }
    }

    /*
     * Configuration provider section Basic requirement: one common
     * configuration available for each instance of axis. Different services
     * MUST be deployed in different axis web applications
     */

    protected static CommonServiceConfig commonConfiguration = null;

    protected static Class<?> configuratorClass = null;

    public static CommonServiceConfig getConfiguration() {

        if (configuratorClass == null) {
            logger.error("Cannot detect service configuration class");
        }

        if (commonConfiguration == null) {
            synchronized (CommonServiceConfig.class) {
                if (commonConfiguration == null) {
                    try {

                        commonConfiguration = (CommonServiceConfig) configuratorClass.newInstance();

                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        }
        return commonConfiguration;
    }

}
