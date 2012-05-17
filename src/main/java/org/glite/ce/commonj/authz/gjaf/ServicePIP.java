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

import javax.print.AttributeException;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;

/**
 * The <code>ServicePIP</code> interface should be implemeted by interceptors
 * that are responsible for collecting attributes for subject that later on can
 * be used by PDPs to determine whether the subject is allowed to invoke the
 * requested operation. The ServicePIPs can be put into interceptor chains
 * together with PDPs.
 * 
 * @see ServicePDP
 * @see ServiceAuthorizationChain
 */
public interface ServicePIP
    extends ServiceInterceptor {
    /**
     * collects attributes and populates the subject with public or private
     * credentials to be checked by subsequent PDPs in the same interceptor
     * chain.
     * 
     * @param peerSubject
     *            authenticated subject for which attributes should be collected
     * @param context
     *            holds properties of this XML message exchange
     * @param operation
     *            operation that the subject wants to invoke
     * @throws AttributeException
     *             if an error occurred while collecting the attributes
     */
    void collectAttributes(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context, QName operation)
        throws AuthorizationException;
}
