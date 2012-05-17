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

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;

/**
 * Interface that must be implemented by all PDPs in an interceptor chain A PDP
 * is responsible for making decisions whether a subject is allowed to invoke a
 * certain operation. The subject may contain public or private credentials
 * holding attributes collected and verified by PIPs. A PDP is also responsible
 * for managing a policy associated with a service. The service is associated
 * with the PDP in the initialize call in {@link ServiceInterceptor} through the
 * id parameter.
 * 
 * @see ServiceAuthorizationChain
 * @see ServicePIP
 */
public interface ServicePDP
    extends ServiceInterceptor {

    public static final int NO_DECISION = 0;

    public static final int DENIED = 1;

    public static final int ALLOWED = 2;

    public static final int STRONG_DENIED = 4;

    public static final int STRONG_ALLOWED = 8;

    /**
     * this operation is called by the PDP Framework whenever the application
     * needs to call secured operations.
     * 
     * @param peerSubject
     *            authenticated client subject with credentials and attributes
     * @param context
     *            holds properties of this XML message exchange
     * @param operation
     *            operation that the subject wants to invoke
     * @return the permission level for that request
     * @throws AuthorizationException
     *             if a serious error occurred that should stop further
     *             evaluation
     */
    public int getPermissionLevel(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context,
            QName operation)
        throws AuthorizationException;
}
