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

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Vector;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.glite.authz.common.security.PEMFileReader;
import org.glite.authz.pep.client.config.PEPClientConfiguration;
import org.glite.authz.pep.client.config.PEPClientConfigurationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationFactory;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.configuration.CEConfigResource;
import org.glite.security.util.FileCertReader;
import org.glite.voms.PKIStore;
import org.glite.voms.PKIStoreFactory;
import org.glite.voms.VOMSTrustManager;

public class PEPConfigurationItem
    extends PEPClientConfiguration
    implements CEConfigResource, ServiceAuthorizationFactory {

    static final long serialVersionUID = 1272456581;

    private static Logger logger = Logger.getLogger(PEPConfigurationItem.class.getName());

    private static ServiceAuthorizationInterface pepClient = null;

    private static PEPConfigurationItem currentConfig = null;

    private String userCert;

    private String userKey;

    private String pwd;

    private String caDir;

    private String mapClass;

    private String resID;

    private X509TrustManager pepTrustManager;

    private X509KeyManager pepKeyManager;

    public PEPConfigurationItem() {
        super();
    }

    public void setMappingClass(String mClass) {
        mapClass = new String(mClass);
    }

    public String getMappingClass() {
        return new String(mapClass);
    }

    public void setResourceID(String resID) {
        this.resID = new String(resID);
    }

    public String getResourceID() {
        return new String(resID);
    }

    public void setKeyMaterial(String userCert, String userKey, String pwd)
        throws PEPClientConfigurationException {
        this.userCert = new String(userCert);
        this.userKey = new String(userKey);
        this.pwd = new String(pwd);

        try {
            PEMFileReader reader = new PEMFileReader();
            PrivateKey pkey = reader.readPrivateKey(userKey, pwd);
            Vector certsVector = (new FileCertReader()).readCerts(userCert);
            X509Certificate[] certs = new X509Certificate[certsVector.size()];
            certsVector.copyInto(certs);
            char passwd[] = pwd.toCharArray();
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, passwd);
            keystore.setKeyEntry("keycreds", pkey, passwd, certs);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keystore, passwd);
            pepKeyManager = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new PEPClientConfigurationException(ex);
        }

    }

    public X509KeyManager getKeyManager() {
        return pepKeyManager;
    }

    public void setTrustMaterial(String caDir)
        throws PEPClientConfigurationException {
        this.caDir = new String(caDir);
        try {
            PKIStore trustStore = PKIStoreFactory.getStore(caDir, PKIStore.TYPE_CADIR);
            pepTrustManager = new VOMSTrustManager(trustStore);
        } catch (Exception ex) {
            throw new PEPClientConfigurationException(ex);
        }
    }

    public X509TrustManager getTrustManager() {
        return pepTrustManager;
    }

    public String getCADir() {
        return caDir;
    }

    public Object clone() {
        try {

            PEPConfigurationItem res = new PEPConfigurationItem();
            res.setMappingClass(mapClass);
            res.setResourceID(resID);
            for (String tmps : this.getPEPDaemonEndpoints()) {
                res.addPEPDaemonEndpoint(tmps);
            }

            res.userCert = this.userCert;
            res.userKey = this.userKey;
            res.pwd = this.pwd;
            res.pepKeyManager = this.pepKeyManager;
            res.caDir = this.caDir;
            res.pepTrustManager = this.pepTrustManager;

            res.setConnectionTimeout(this.getConnectionTimeout());
            res.setMaxConnectionsPerHost(this.getMaxConnectionsPerHost());
            res.setMaxTotalConnections(this.getMaxTotalConnections());

            return res;

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PEPConfigurationItem))
            return false;
        PEPConfigurationItem tmpItem = (PEPConfigurationItem) obj;

        if (!tmpItem.userCert.equals(userCert) || !tmpItem.userKey.equals(userKey) || !tmpItem.pwd.equals(pwd))
            return false;

        if (!tmpItem.caDir.equals(caDir) || !tmpItem.mapClass.equals(mapClass) || !tmpItem.resID.equals(resID))
            return false;

        if (tmpItem.getConnectionTimeout() != this.getConnectionTimeout())
            return false;

        if (tmpItem.getMaxConnectionsPerHost() != this.getMaxConnectionsPerHost())
            return false;

        if (tmpItem.getMaxTotalConnections() != this.getMaxTotalConnections())
            return false;

        if (!super.getPEPDaemonEndpoints().equals(tmpItem.getPEPDaemonEndpoints()))
            return false;

        return true;
    }

    public ServiceAuthorizationInterface getInstance() {

        if (pepClient == null || currentConfig == null || !this.equals(currentConfig)) {
            currentConfig = (PEPConfigurationItem) this.clone();
            try {
                pepClient = new ArgusPEP(currentConfig);
                logger.debug("Renewed PEP client instance");
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        return pepClient;
    }

}
