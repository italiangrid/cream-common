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

package org.glite.ce.commonj.authz.axis2;

import java.util.ArrayList;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.modules.Module;
import org.apache.log4j.Logger;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.glite.ce.commonj.configuration.CommonServiceConfig;
import org.italiangrid.voms.store.impl.DefaultUpdatingVOMSTrustStore;

import eu.emi.security.authn.x509.impl.OpensslCertChainValidator;

public class AuthorizationModule
    implements Module {

    private static final Logger logger = Logger.getLogger(AuthorizationModule.class.getName());

    public static DefaultUpdatingVOMSTrustStore vomsStore = null;

    public static OpensslCertChainValidator validator = null;

    public void init(ConfigurationContext configContext, AxisModule module)
        throws AxisFault {

        try {

            CommonServiceConfig commonConfig = CommonServiceConfig.getConfiguration();

            String caDir = commonConfig.getGlobalAttributeAsString("CApath", "/etc/grid-security/certificates");
            String voDir = commonConfig.getGlobalAttributeAsString("VOpath", "/etc/grid-security/vomsdir");

            long updateFrequency = commonConfig.getGlobalAttributeAsLong("VOreload",
                    DefaultUpdatingVOMSTrustStore.DEFAULT_UPDATE_FREQUENCY);

            validator = new OpensslCertChainValidator(caDir);

            ArrayList<String> localTrustDirs = new ArrayList<String>(1);
            localTrustDirs.add(voDir);

            vomsStore = new DefaultUpdatingVOMSTrustStore(localTrustDirs, updateFrequency);

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException("Cannot configure security support");
        }

    }

    public void shutdown(ConfigurationContext configurationContext)
        throws AxisFault {

        if (vomsStore != null) {
            vomsStore.cancel();
        }

        if (validator != null) {
            validator.dispose();
        }

    }

    public void engageNotify(AxisDescription axisDescription)
        throws AxisFault {
    }

    public void applyPolicy(Policy policy, AxisDescription axisDescription)
        throws AxisFault {
    }

    public boolean canSupportAssertion(Assertion assertion) {
        return false;
    }

}
