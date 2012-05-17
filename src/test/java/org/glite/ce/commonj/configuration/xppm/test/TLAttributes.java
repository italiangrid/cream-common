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

package org.glite.ce.commonj.configuration.xppm.test;

public class TLAttributes {

    public static final String ID_LABEL = "id";

    public static final String CODE_LABEL = "code";

    public String id;

    public String code;

    public TLAttributes(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public boolean equals(Object tmpo) {
        if (tmpo == null || !(tmpo instanceof TLAttributes)) {
            return false;
        }
        TLAttributes attrs = (TLAttributes) tmpo;
        return id.equals(attrs.id) && code.equals(attrs.code);
    }

    public String getXMLAttributes() {
        StringBuffer buff = new StringBuffer(ID_LABEL);
        buff.append("=\"").append(id).append("\" ");
        buff.append(CODE_LABEL).append("=\"");
        buff.append(code).append("\" ");
        return buff.toString();
    }
}
