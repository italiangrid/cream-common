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
import org.glite.ce.commonj.authz.AuthZConstants;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;

import eu.emi.security.authn.x509.helpers.CertificateHelpers;

public class AdminCheckerPIP
    implements ServicePIP {

    private static Logger logger = Logger.getLogger(AdminCheckerPIP.class.getName());

    public static final String ADMIN_FILE = "adminList";

    private String id;

    private String adminFile;

    private HashSet<String> dnTable;

    private long timestamp;

    public AdminCheckerPIP() {
        this.id = "undef";
        timestamp = 0;
    }

    public AdminCheckerPIP(String id) {
        this.id = id;
        timestamp = 0;
    }

    public AdminCheckerPIP(String id, String adminFile) throws InitializeException {
        this.id = id;
        timestamp = 0;
        if (adminFile == null) {
            logger.error("Admin file not specified");
            throw new InitializeException("Admin file not specified");
        }
        readAdminFile(adminFile);
    }

    public void initialize(ChainConfig config, String name, String id)
        throws InitializeException {

        String admFile = (String) config.getProperty(name, ADMIN_FILE);
        if (admFile == null) {
            logger.error("Admin file not specified");
            throw new InitializeException("Admin file not specified");
        }

        readAdminFile(admFile);
        adminFile = admFile;
        this.id = id;

    }

    public String getId() {
        return id;
    }

    public void setProperty(String name, String value)
        throws InitializeException {
        if (name.equals(ADMIN_FILE)) {
            readAdminFile(value);
            adminFile = value;
        }
    }

    public String getProperty(String name) {
        if (name.equals(ADMIN_FILE)) {
            return adminFile;
        }
        return null;
    }

    public String[] getProperties() {
        return new String[] { ADMIN_FILE };
    }

    public boolean isTriggerable(String name) {
        return name.equals(ADMIN_FILE);
    }

    private void readAdminFile(String admFile)
        throws InitializeException {

        logger.debug("Initializing admin service PIP with " + admFile);

        dnTable = new HashSet<String>();
        timestamp = System.currentTimeMillis();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(admFile));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();

                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.startsWith("\"") && line.endsWith("\""))
                        line = line.substring(1, line.length() - 1);
                    dnTable.add(CertificateHelpers.opensslToRfc2253(line, false));
                    logger.debug("Registered DN: " + line);
                }
                line = reader.readLine();
            }
        } catch (IOException ioEx) {
            logger.error(ioEx.getMessage(), ioEx);
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

    public void collectAttributes(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context,
            QName operation)
        throws AuthorizationException {

        Set<X500Principal> pSet = peerSubject.getPrincipals(X500Principal.class);
        if (pSet == null) {
            throw new AuthorizationException("Cannot retrieve credentials from the authorization layer");
        }

        context.setProperty(AuthZConstants.IS_ADMIN, new Boolean(false));

        for (X500Principal principal : pSet) {
            String identity = principal.getName();
            if (dnTable.contains(identity)) {
                logger.info("Admin test for " + identity + ":  true");
                context.setProperty(AuthZConstants.IS_ADMIN, new Boolean(true));
            } else {
                logger.debug("Admin test for " + identity + ":  false");
            }
        }

    }

    public void close()
        throws CloseException {
    }

    public Object clone() {
        AdminCheckerPIP result = new AdminCheckerPIP(this.id);
        result.id = this.id;
        result.adminFile = this.adminFile;
        result.timestamp = this.timestamp;
        result.dnTable = (HashSet<String>) this.dnTable.clone();
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AdminCheckerPIP))
            return false;
        AdminCheckerPIP pip = (AdminCheckerPIP) obj;
        return (pip.adminFile.equals(this.adminFile) && pip.id.equals(this.id) && pip.timestamp == this.timestamp);
    }

}
