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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.glite.authz.pep.client.PEPClientException;
import org.glite.ce.commonj.authz.axis2.AuthorizationModule;

public class IssuerCache {

    private static Logger logger = Logger.getLogger(IssuerCache.class.getName());

    private static IssuerCache theCache = null;

    public static IssuerCache getCache()
        throws PEPClientException {
        try {

            synchronized (IssuerCache.class) {
                if (theCache == null) {
                    theCache = new IssuerCache(AuthorizationModule.validator.getUpdateInterval());
                }
                return theCache;
            }

        } catch (Throwable th) {
            throw new PEPClientException("Cannot load mapping class");
        }
    }

    private HashMap<String, String> issuerCache;

    private long cacheTouch;

    private long refreshTime;

    protected IssuerCache(long rTime) {

        issuerCache = new HashMap<String, String>();

        refreshTime = rTime;

        loadIssuerCache();

    }

    private void loadIssuerCache() {

        issuerCache.clear();

        X509Certificate[] trustCerts = AuthorizationModule.validator.getTrustedIssuers();
        for (X509Certificate tCert : trustCerts) {
            String sbjName = tCert.getSubjectX500Principal().getName();
            String issName = tCert.getIssuerX500Principal().getName();
            if (sbjName.equals(issName)) {
                issName = "";
            }
            logger.debug("Caching CA: " + sbjName + "(" + issName + ")");
            issuerCache.put(sbjName, issName);
        }

        cacheTouch = System.currentTimeMillis();

        logger.debug("Loaded " + issuerCache.size() + " CA");

    }

    public synchronized List<String> getIssuerIdList(X509Certificate userCert) {

        ArrayList<String> result = new ArrayList<String>();
        boolean needReload = true;
        String currIssuer = userCert.getIssuerX500Principal().getName();

        while (needReload) {

            /*
             * TODO replace with a notification from the validator
             */
            long deltaTime = System.currentTimeMillis() - cacheTouch - refreshTime;

            needReload &= (deltaTime > 0);

            while (issuerCache.containsKey(currIssuer)) {
                result.add(currIssuer);
                /*
                 * Assumption: subCA chains never change
                 */
                needReload &= false;
                currIssuer = issuerCache.get(currIssuer);
            }

            if (needReload) {
                loadIssuerCache();
            }
        }

        if (result.size() == 0) {
            logger.error("Cannot find CA chain rooted at " + currIssuer);
            result.add(userCert.getIssuerX500Principal().getName());
        }

        return result;
    }

}
