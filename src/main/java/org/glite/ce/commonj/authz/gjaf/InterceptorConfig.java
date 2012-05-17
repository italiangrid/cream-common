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

/**
 * The <code>InterceptorConfig</code> class is used to hold configuration
 * information about an interceptor in a configuration mechanism independent
 * way. It is used by <code>ServicePDPConfig</code>.
 * 
 * @see ChainConfig
 */
public class InterceptorConfig {
    private String interceptorClass;

    private ServiceInterceptor interceptor;

    private String name;

    private boolean loaded = false;

    /**
     * Constructor.
     * 
     * @param configName
     *            the named scope of the interceptor used in configuration
     *            entries
     * @param serviceInterceptor
     *            the class name of the interceptor
     */
    public InterceptorConfig(String configName, ServiceInterceptor serviceInterceptor) {
        this.interceptor = serviceInterceptor;
        this.loaded = true;
        this.name = configName;
    }

    /**
     * Constructor.
     * 
     * @param configName
     *            the named scope of the interceptor used in configuration
     *            entries
     * @param configInterceptorClass
     *            the class name of the interceptor
     */
    public InterceptorConfig(String configName, String configInterceptorClass) {
        this.interceptorClass = configInterceptorClass;
        this.name = configName;
    }

    /**
     * gets the interceptor class.
     * 
     * @return the class name of the interceptor
     */
    public String getInterceptorClass() {
        return this.interceptorClass;
    }

    /**
     * gets the interceptor.
     * 
     * @return the interceptor
     */
    public ServiceInterceptor getInterceptor() {
        return this.interceptor;
    }

    /**
     * gets the interceptor name attached to this interceptor.
     * 
     * @return the named scope of the interceptor used in configuration entries
     */
    public String getName() {
        return this.name;
    }

    /**
     * method to support eager loading of interceptors.
     * 
     * @return whether the interceptor class has been loaded
     */
    public boolean isLoaded() {
        return this.loaded;
    }
}
