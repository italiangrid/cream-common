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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.configuration.CEConfigResource;

public class ServiceAuthorizationChain
    implements ServiceAuthorizationInterface, CEConfigResource {

    public static final long serialVersionUID = 1318326624;

    private static Logger logger = Logger.getLogger(ServiceAuthorizationChain.class.getName());

    private String id;

    private ServiceInterceptor[] interceptor;

    public synchronized void initialize(ChainConfig config, String name, String id)
        throws InitializeException {

        this.id = id;

        ClassLoader loader = this.getClass().getClassLoader();

        InterceptorConfig[] interceptorConfig = config.getInterceptors();
        if (interceptorConfig == null) {
            throw new InitializeException("No interceptor in configuration");
        }

        this.interceptor = new ServiceInterceptor[interceptorConfig.length];
        try {
            for (int i = 0; i < interceptorConfig.length; i++) {
                if (interceptorConfig[i].isLoaded()) {
                    this.interceptor[i] = interceptorConfig[i].getInterceptor();
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Trying to load: " + interceptorConfig[i].getInterceptorClass());
                    }

                    Class<?> interceptorClass = loader.loadClass(interceptorConfig[i].getInterceptorClass());
                    this.interceptor[i] = (ServiceInterceptor) interceptorClass.newInstance();
                    interceptor[i].initialize(config, interceptorConfig[i].getName(), id);
                }
            }
        } catch (InitializeException initEx) {
            throw initEx;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("Cannot load interceptor chain", e);
            } else {
                logger.error("Cannot load interceptor chain: " + e.getMessage());
            }
            throw new InitializeException("Cannot load interceptor chain", e);
        }

    }

    public synchronized void initialize(String id, List<ServiceInterceptor> interceptorList) {
        this.id = id;

        this.interceptor = new ServiceInterceptor[interceptorList.size()];
        interceptorList.toArray(this.interceptor);
    }

    public String getId() {
        return id;
    }

    public boolean isPermitted(Subject peerSubject, MessageContext context, QName operation)
        throws AuthorizationException {

        try {

            Set<X500Principal> pSet = peerSubject.getPrincipals(X500Principal.class);
            String peerIdentity = pSet.size() > 0 ? pSet.iterator().next().toString() : "unknown";

            int level = ServicePDP.NO_DECISION;

            for (int i = 0; (interceptor != null) && (i < interceptor.length); i++) {
                if (interceptor[i] instanceof ServicePDP) {
                    ServicePDP pdp = (ServicePDP) interceptor[i];
                    level |= pdp.getPermissionLevel(peerSubject, context, operation);
                } else if (interceptor[i] instanceof ServicePIP) {
                    ((ServicePIP) interceptor[i]).collectAttributes(peerSubject, context, operation);
                }
            }

            if (level < ServicePDP.ALLOWED) {
                logger.info("User " + peerIdentity + " not authorized for " + operation);
                return false;
            }
            if (level < ServicePDP.STRONG_DENIED) {
                logger.debug("User " + peerIdentity + " authorized for " + operation);
                return true;
            }
            if (level < ServicePDP.STRONG_ALLOWED) {
                logger.info("User " + peerIdentity + " not authorized for " + operation);
                return false;
            }

            logger.debug("User " + peerIdentity + " authorized for " + operation);
            return true;

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.error("Authorization error", ex);
            } else {
                logger.error("Authorization error: " + ex.getMessage());
            }
            throw new AuthorizationException("Authorization error: " + ex.getMessage(), ex);
        }

    }

    public ServiceInterceptor[] getInterceptors() {
        return this.interceptor;
    }

    public File[] getAllTriggers() {
        ArrayList<File> tmpList = new ArrayList<File>(interceptor.length);
        for (int k = 0; k < interceptor.length; k++) {
            String[] props = interceptor[k].getProperties();
            for (int j = 0; j < props.length; j++) {
                if (interceptor[k].isTriggerable(props[j])) {
                    tmpList.add(new File(interceptor[k].getProperty(props[j])));
                }
            }
        }

        File[] result = new File[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public Object clone() {

        ServiceAuthorizationChain result = new ServiceAuthorizationChain();
        result.id = this.id;
        result.interceptor = new ServiceInterceptor[this.interceptor.length];
        for (int k = 0; k < this.interceptor.length; k++)
            result.interceptor[k] = (ServiceInterceptor) this.interceptor[k].clone();
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceAuthorizationChain)) {
            return false;
        }

        ServiceAuthorizationChain chain = (ServiceAuthorizationChain) obj;
        if (chain.interceptor.length != this.interceptor.length) {
            return false;
        }

        for (int k = 0; k < this.interceptor.length; k++) {
            if (!chain.interceptor[k].equals(this.interceptor[k])) {
                return false;
            }
        }
        return true;
    }

}
