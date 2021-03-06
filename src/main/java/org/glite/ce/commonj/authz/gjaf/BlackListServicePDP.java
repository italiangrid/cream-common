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

package org.glite.ce.commonj.authz.gjaf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;

import eu.emi.security.authn.x509.impl.OpensslNameUtils;

public class BlackListServicePDP
    implements ServicePDP {

    private static Logger logger = Logger.getLogger(BlackListServicePDP.class.getName());

    public static final String BLACK_LIST_FILE = "blackListFile";

    private String id;

    private String blackListFile;

    private HashSet<String> dnTable;

    private long timestamp;

    public BlackListServicePDP() {
        this.id = "undef";
        timestamp = 0;
    }

    public BlackListServicePDP(String id) {
        this.id = id;
        timestamp = 0;
    }

    public void initialize(ChainConfig config, String name, String id)
        throws InitializeException {

        String bannerFile = (String) config.getProperty(name, BLACK_LIST_FILE);
        if (bannerFile == null) {
            logger.error("Blacklist file not specified");
            throw new InitializeException("Blacklist file not specified");
        }

        readBannerFile(bannerFile);
        blackListFile = bannerFile;
        this.id = id;

    }

    public String getId() {
        return id;
    }

    public void setProperty(String name, String value)
        throws InitializeException {
        if (name.equals(BLACK_LIST_FILE)) {
            readBannerFile(value);
            blackListFile = value;
        }
    }

    public String getProperty(String name) {
        if (name.equals(BLACK_LIST_FILE)) {
            return blackListFile;
        }
        return null;
    }

    public String[] getProperties() {
        return new String[] { BLACK_LIST_FILE };
    }

    public boolean isTriggerable(String name) {
        return name.equals(BLACK_LIST_FILE);
    }

    private void readBannerFile(String bannerFile)
        throws InitializeException {

        logger.debug("Initializing blacklist service PDP with " + bannerFile);

        dnTable = new HashSet<String>();
        timestamp = System.currentTimeMillis();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(bannerFile));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();

                if (line.length() > 0 && !line.startsWith("#")) {

                    /*
                     * TODO get rid of openssl format
                     */

                    if (line.startsWith("\""))
                        line = line.substring(1);
                    if (line.endsWith("\""))
                        line = line.substring(0, line.length() - 1);
                    dnTable.add(OpensslNameUtils.opensslToRfc2253(line, false));
                    logger.debug("Registered DN: " + line);
                }
                line = reader.readLine();
            }
        } catch (IOException ioEx) {
            if (logger.isDebugEnabled()) {
                logger.error(ioEx.getMessage(), ioEx);
            } else {
                logger.error(ioEx.getMessage());
            }
            throw new InitializeException(ioEx.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                }
            }
        }

    }

    public int getPermissionLevel(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context,
            QName operation)
        throws AuthorizationException {

        Set<X500Principal> pSet = peerSubject.getPrincipals(X500Principal.class);
        if (pSet == null) {
            throw new AuthorizationException("Cannot retrieve credentials from the authorization layer");
        }

        for (X500Principal principal : pSet) {
            String identity = principal.getName();
            logger.debug("Checking identity: " + identity);
            if (dnTable.contains(identity)) {
                logger.info("Identity banned: " + identity);
                return STRONG_DENIED;
            }
        }

        return NO_DECISION;

    }

    public void close()
        throws CloseException {
    }

    public Object clone() {
        BlackListServicePDP result = new BlackListServicePDP(this.id);
        result.id = this.id;
        result.blackListFile = this.blackListFile;
        result.timestamp = this.timestamp;
        result.dnTable = (HashSet<String>) this.dnTable.clone();
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BlackListServicePDP))
            return false;
        BlackListServicePDP pdp = (BlackListServicePDP) obj;
        return (pdp.blackListFile.equals(this.blackListFile) && pdp.id.equals(this.id) && pdp.timestamp == this.timestamp);
    }

}
