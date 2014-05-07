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

package org.glite.ce.commonj.authz.argus;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.glite.authz.pep.client.config.PEPClientConfiguration;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.authz.axis2.AuthorizationModule;

import eu.emi.security.authn.x509.helpers.ssl.SSLTrustManager;

public class ArgusPEPFactory {

    private static Logger logger = Logger.getLogger(ArgusPEPFactory.class.getName());

    private static ServiceAuthorizationInterface pepClient = null;

    private static PEPConfigurationItem currentConfig = null;

    public synchronized static ServiceAuthorizationInterface getInstance(PEPConfigurationItem newConfig) {

        if (pepClient == null || currentConfig == null || !newConfig.equals(currentConfig)) {

            currentConfig = (PEPConfigurationItem) newConfig.clone();

            try {

                PEPClientConfiguration pepConf = new ExtPEPClientConfig();

                if (currentConfig.getConnectionTimeout() > 0) {
                    pepConf.setConnectionTimeout(currentConfig.getConnectionTimeout());
                }

                if (currentConfig.getMaxConnectionsPerHost() > 0) {
                    pepConf.setMaxConnectionsPerHost(currentConfig.getMaxConnectionsPerHost());
                }

                if (currentConfig.getMaxTotalConnections() > 0) {
                    pepConf.setMaxTotalConnections(currentConfig.getMaxTotalConnections());
                }

                pepConf.setKeyMaterial(currentConfig.getCertificatePath(), currentConfig.getKeyPath(),
                        currentConfig.getPassword());

                for (String epr : currentConfig.getEndpoints()) {
                    pepConf.addPEPDaemonEndpoint(epr);
                }

                pepClient = new ArgusPEP(pepConf, currentConfig.getResourceID(), currentConfig.getMappingClass());
                logger.debug("Renewed PEP client instance");

            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.error(ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
            }

        }

        return pepClient;
    }

}

class ExtPEPClientConfig
    extends PEPClientConfiguration {

    private X509TrustManager trustManager;

    public ExtPEPClientConfig() {
        super();

        trustManager = new SSLTrustManager(AuthorizationModule.validator);
    }

    public X509TrustManager getTrustManager() {
        return trustManager;
    }
}
