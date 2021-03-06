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

import java.util.HashMap;

import org.italiangrid.voms.VOMSAttribute;
import org.italiangrid.voms.ac.VOMSValidationResult;
import org.italiangrid.voms.ac.ValidationResultListener;
import org.italiangrid.voms.error.VOMSValidationErrorMessage;

public class VOMSResultCollector
    extends HashMap<String, VOMSValidationResult>
    implements ValidationResultListener {

    static final long serialVersionUID = 1352903459;

    public VOMSResultCollector() {
        super();
    }

    public void notifyValidationResult(VOMSValidationResult result) {
        if (!result.isValid()) {
            VOMSAttribute attributes = result.getAttributes();
            this.put(attributes.getVO(), result);
        }
    }

    public String toString() {

        StringBuffer tmpbuff = new StringBuffer();

        for (String vo : this.keySet()) {
            VOMSValidationResult vRes = this.get(vo);
            for (VOMSValidationErrorMessage msg : vRes.getValidationErrors()) {
                tmpbuff.append("VO ").append(vo).append(": ");
                tmpbuff.append(msg.getMessage()).append("\n");
            }
        }

        return tmpbuff.toString();
    }

}