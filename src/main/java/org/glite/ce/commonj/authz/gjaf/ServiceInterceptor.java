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
 * Generic interface to be implemented by all interceptors (PIPs and PDPs) in a
 * chain.
 */
public interface ServiceInterceptor
    extends Cloneable {
    /**
     * initializes the interceptor with configuration information that are valid
     * up until the point when close is called.
     * 
     * @param config
     *            holding interceptor specific configuration values, that may be
     *            obtained using the name parameter
     * @param name
     *            the name that should be used to access all the interceptor
     *            local configuration
     * @param id
     *            the id in common for all interceptors in a chain (it is valid
     *            up until close is called) if close is not called the
     *            interceptor may assume that the id still exists after a
     *            process restart
     * @throws InitializeException
     *             if an exception occured during initialization
     */
    void initialize(ChainConfig config, String name, String id)
        throws InitializeException;

    String getId();

    void setProperty(String name, String value)
        throws InitializeException;

    String getProperty(String name);

    String[] getProperties();

    boolean isTriggerable(String name);

    /**
     * this method is called by the PDP framework to indicate that the
     * interceptor now should remove all state that was allocated in the
     * initialize call.
     * 
     * @throws CloseException
     *             if an error occurred while closing this interceptor
     */
    void close()
        throws CloseException;

    public Object clone();
}
