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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CEConfigResource;

public class PEPConfigurationItem
    implements CEConfigResource {

    static final long serialVersionUID = 1272456581;

    private static Logger logger = Logger.getLogger(PEPConfigurationItem.class.getName());

    private String mapClass = "";

    private String resID = "";

    private ArrayList<String> endpoints;

    private String clientCertPath = "";

    private String clientKeyPath = "";

    private String clientPassword = "";

    private int connTimeout = 0;

    private int maxConn = 0;

    private int maxTotalConn = 0;

    public PEPConfigurationItem() {
        super();

        endpoints = new ArrayList<String>();
    }

    public void setMappingClass(String mClass) {
        if (mClass != null) {
            mapClass = mClass;
        }
    }

    public String getMappingClass() {
        return new String(mapClass);
    }

    public void setResourceID(String id) {
        if (id != null) {
            resID = id;
        }
    }

    public String getResourceID() {
        return new String(resID);
    }

    public String getCertificatePath() {
        return clientCertPath;
    }

    public void setCertificatePath(String path) {
        if (path != null) {
            clientCertPath = path;
        }
    }

    public String getKeyPath() {
        return clientKeyPath;
    }

    public void setKeyPath(String path) {
        if (path != null) {
            clientKeyPath = path;
        }
    }

    public String getPassword() {
        return clientPassword;
    }

    public void setPassword(String pwd) {
        if (pwd != null) {
            clientPassword = pwd;
        }
    }

    public void addEndpoint(String epr) {
        if (epr == null) {
            return;
        }

        String tmps = epr.trim();
        if (tmps.length() > 0) {
            endpoints.add(tmps);
        }
    }

    public ArrayList<String> getEndpoints() {
        return endpoints;
    }

    public void removeEndpoints() {
        endpoints.clear();
    }

    public int getConnectionTimeout() {
        return connTimeout;
    }

    public void setConnectionTimeout(int to) {
        connTimeout = to;
    }

    public int getMaxConnectionsPerHost() {
        return maxConn;
    }

    public void setMaxConnectionsPerHost(int mc) {
        maxConn = mc;
    }

    public int getMaxTotalConnections() {
        return maxTotalConn;
    }

    public void setMaxTotalConnections(int tc) {
        maxTotalConn = tc;
    }

    public Object clone() {
        try {

            PEPConfigurationItem res = new PEPConfigurationItem();
            res.setMappingClass(mapClass);
            res.setResourceID(resID);

            res.setCertificatePath(clientCertPath);
            res.setKeyPath(clientKeyPath);
            res.setPassword(clientPassword);

            for (String tmps : endpoints) {
                res.addEndpoint(tmps);
            }

            res.setConnectionTimeout(connTimeout);
            res.setMaxConnectionsPerHost(maxConn);
            res.setMaxTotalConnections(maxTotalConn);

            return res;

        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof PEPConfigurationItem))
            return false;

        PEPConfigurationItem tmpItem = (PEPConfigurationItem) obj;

        if (!tmpItem.mapClass.equals(mapClass) || !tmpItem.resID.equals(resID))
            return false;

        if (!tmpItem.clientCertPath.equals(clientCertPath))
            return false;

        if (!tmpItem.clientKeyPath.equals(clientKeyPath))
            return false;

        if (!tmpItem.clientPassword.equals(clientPassword))
            return false;

        if (tmpItem.connTimeout != connTimeout)
            return false;

        if (tmpItem.maxConn != maxConn)
            return false;

        if (tmpItem.maxConn != maxTotalConn)
            return false;

        if (!endpoints.equals(endpoints))
            return false;

        return true;
    }

}
