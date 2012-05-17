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


public class SimpleElement
    implements MockElement {

    public String value;

    public String content;

    public SimpleElement(String attrValue, String content) {
        value = attrValue;
        this.content = content;
    }

    public boolean equals(Object tmpo) {
        if (tmpo == null || !(tmpo instanceof SimpleElement)) {
            return false;
        }
        SimpleElement sElement = (SimpleElement) tmpo;

        return value.equals(sElement.value) && content.equals(sElement.content);

    }

    public String getXMLElement() {
        StringBuffer buff = new StringBuffer("<simple attribute=\"");
        buff.append(value).append("\">");
        buff.append(content);
        buff.append("</simple>");
        return buff.toString();
    }

}
