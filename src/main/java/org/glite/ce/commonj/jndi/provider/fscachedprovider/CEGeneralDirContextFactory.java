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

package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;

import org.apache.log4j.Logger;

public class CEGeneralDirContextFactory 
    implements InitialContextFactory {

    private static Logger logger =
        Logger.getLogger(CEGeneralDirContextFactory.class.getName());

    public CEGeneralDirContextFactory(){}

    public Context getInitialContext(Hashtable env) throws NamingException{

        String url = (String)env.get(Context.PROVIDER_URL);
        if( url==null ) 
            throw new NoInitialContextException();
        return new CEGeneralDirContext(url, env);

    }

}
