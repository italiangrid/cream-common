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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

public class ConfigFile {

    private static Logger logger = Logger.getLogger(ConfigFile.class.getName());

    private String confFilename;

    public ConfigFile() {
        confFilename = System.getProperty("xml.test.config.file", "myconfig.xml");
    }

    public void write(TLAttributes topLevelAttrs, MockElement[] elements, boolean simulateError)
        throws IOException {

        logger.debug("Writing config file " + confFilename);

        PrintWriter writer = new PrintWriter(new File(confFilename));
        writer.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        writer.print("<service name=\"testservice\" ");
        writer.print(topLevelAttrs.getXMLAttributes());
        writer.println(">");
        for (MockElement elem : elements) {
            writer.println(elem.getXMLElement());
        }
        if (!simulateError) {
            writer.println("</service>");
        }
        writer.flush();
        writer.close();
    }

    public void delete() {
        File tmpf = new File(confFilename);
        tmpf.delete();
    }

    public String getFilename() {
        return confFilename;
    }

    public String getDirectory() {
        return (new File(confFilename)).getParent();
    }
}
