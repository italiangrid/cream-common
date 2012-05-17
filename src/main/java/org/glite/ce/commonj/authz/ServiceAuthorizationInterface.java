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

package org.glite.ce.commonj.authz;

import java.util.Iterator;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

public interface ServiceAuthorizationInterface {
    
    /**
     * Temporary interface used to deal with
     * two different definitions of MessageContext
     * It will be removed when CEMon migrate 
     * to axis2
     */
    public interface MessageContext {
        
        public boolean containsProperty(String name);
        public Object getProperty(String name);
        public Iterator<String> getPropertyNames();
        public void removeProperty(String name);
        public void setProperty(String name, Object value);
        
    }
    
    public boolean isPermitted(Subject peerSubject, 
            MessageContext context, QName operation)
        throws AuthorizationException;
    
}
