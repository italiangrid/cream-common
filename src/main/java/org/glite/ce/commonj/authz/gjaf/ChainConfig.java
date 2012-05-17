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
 * This interface is used to encapsulate and shield the interceptor
 * configuration mechanism from the core PDP framework. The configuration is
 * associated with an interceptor between the initialize and close calls.
 * 
 * @see ServiceInterceptor
 * @see ServicePIP
 * @see ServicePDP
 */
public interface ChainConfig {
    /**
     * gets the interceptors class names to be loaded, and their names
     * (configuration scopes).
     * 
     * @return array of interceptor configurations
     * @throws ConfigException
     *             if exception occured when retrieving interceptors from
     *             configuration
     */
    InterceptorConfig[] getInterceptors()
        throws ConfigException;

    /**
     * gets a property based on the scoped name of the interceptor.
     * 
     * @param name
     *            scoped name of interceptor
     * @param property
     *            name of property to get
     * @return the property or null if not found
     */
    Object getProperty(String name, String property);

    /**
     * sets a property based on the scoped name of the interceptor.
     * 
     * @param name
     *            scoped name of interceptor
     * @param property
     *            name of property to set
     * @param value
     *            value of property to set
     */
    void setProperty(String name, String property, Object value);
}
