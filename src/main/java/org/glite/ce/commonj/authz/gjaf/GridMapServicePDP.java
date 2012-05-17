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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.security.util.X500Principal;

public class GridMapServicePDP
    implements ServicePDP {

    private static Logger logger = Logger.getLogger(GridMapServicePDP.class.getName());

    public static final String GRID_MAP_FILE = "gridMapFile";

    private static final Pattern mapFilePattern = Pattern
            .compile("^\\s*\"(/[^=/\"]+=[^/\"]+/[^\"]+)\"\\s+([^\\s]+)\\s*$");

    private String id;

    private String gridMapFile;

    private HashMap<String, String> dnTable;

    private long timestamp;

    public GridMapServicePDP() {
        this.id = "undef";
        timestamp = 0;
    }

    public GridMapServicePDP(String id) {
        this.id = id;
        timestamp = 0;
    }

    public void initialize(ChainConfig config, String name, String id)
        throws InitializeException {

        String mapFile = (String) config.getProperty(name, GRID_MAP_FILE);
        if (mapFile == null) {
            logger.error("Gridmap file not specified");
            throw new InitializeException("Gridmap file not specified");
        }

        readGridmapFile(mapFile);
        gridMapFile = mapFile;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setProperty(String name, String value)
        throws InitializeException {
        if (name.equals(GRID_MAP_FILE)) {
            readGridmapFile(value);
            gridMapFile = value;
        }
    }

    public String getProperty(String name) {
        if (name.equals(GRID_MAP_FILE)) {
            return gridMapFile;
        }
        return null;
    }

    public String[] getProperties() {
        return new String[] { GRID_MAP_FILE };
    }

    public boolean isTriggerable(String name) {
        return name.equals(GRID_MAP_FILE);
    }

    public void readGridmapFile(String mapFile)
        throws InitializeException {

        logger.debug("Initializing gridmap service PDP with " + mapFile + "(" + this.hashCode() + ")");

        dnTable = new HashMap<String, String>();
        timestamp = System.currentTimeMillis();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(mapFile));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = mapFilePattern.matcher(line);
                if (matcher.matches()) {
                    String oldValue = dnTable.put(matcher.group(1), matcher.group(2));
                    if (oldValue != null) {
                        logger.warn("Replaced value for " + matcher.group(1) + ": " + oldValue);
                    }
                    logger.debug("Registered DN: " + matcher.group(1) + "(" + this.hashCode() + ")");
                }
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

    public int getPermissionLevel(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context,
            QName operation)
        throws AuthorizationException {

        Set<X500Principal> pSet = peerSubject.getPrincipals(X500Principal.class);
        if (pSet == null) {
            logger.warn("Cannot authorize: missing X500Principal in subject");
            return NO_DECISION;
        }

        Iterator<X500Principal> allPrincipals = pSet.iterator();
        while (allPrincipals.hasNext()) {
            String identity = allPrincipals.next().getName();
            logger.debug("Checking identity: " + identity);
            if (dnTable.get(identity) != null) {
                logger.info("Identity authorized: " + identity + "(" + this.hashCode() + ")");
                return STRONG_ALLOWED;
            }
        }

        return DENIED;
    }

    public void close()
        throws CloseException {
    }

    public Object clone() {
        GridMapServicePDP result = new GridMapServicePDP(this.id);
        result.gridMapFile = this.gridMapFile;
        result.timestamp = this.timestamp;
        result.dnTable = (HashMap<String, String>) this.dnTable.clone();
        logger.debug("Cloned " + this.hashCode() + " in " + result.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof GridMapServicePDP))
            return false;
        GridMapServicePDP pdp = (GridMapServicePDP) obj;
        return (pdp.gridMapFile.equals(this.gridMapFile) && pdp.id.equals(this.id) && pdp.timestamp == this.timestamp);
    }

}
