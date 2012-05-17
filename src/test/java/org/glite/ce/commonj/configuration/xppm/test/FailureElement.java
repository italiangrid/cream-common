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


public class FailureElement
    implements MockElement {

    public String failAttr;

    public String content;

    public FailureElement(boolean fail, String content) {
        failAttr = Boolean.toString(fail);
        this.content = content;
    }

    public boolean equals(Object tmpo) {
        if (tmpo == null || !(tmpo instanceof FailureElement)) {
            return false;
        }
        FailureElement fElement = (FailureElement) tmpo;

        return failAttr.equals(fElement.failAttr) && content.equals(fElement.content);
    }

    public String getXMLElement() {
        StringBuffer buff = new StringBuffer("<failure fail=\"");
        buff.append(failAttr).append("\">");
        buff.append(content);
        buff.append("</failure>");
        return buff.toString();
    }

}
