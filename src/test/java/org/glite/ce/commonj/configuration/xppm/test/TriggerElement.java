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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

public class TriggerElement
    implements MockElement {

    private static Logger logger = Logger.getLogger(TriggerElement.class.getName());

    private String filename;

    private String content;

    public TriggerElement(String filename, String content) throws IOException {
        this.filename = filename;
        this.content = content;

        writeContent(content);
    }

    public TriggerElement(String filename) throws IOException {
        this.filename = filename;
        this.content = readContent();
    }

    public boolean equals(Object tmpo) {
        if (tmpo == null || !(tmpo instanceof TriggerElement)) {
            return false;
        }
        TriggerElement tElement = (TriggerElement) tmpo;

        return filename.equals(tElement.filename) && content.endsWith(tElement.content);

    }

    public String getXMLElement() {
        StringBuffer buff = new StringBuffer("<trigger file=\"");
        buff.append(filename).append("\"/>");
        return buff.toString();
    }

    public String getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }

    private void writeContent(String content)
        throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new File(filename));
            writer.print(content);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }

        logger.debug("Written trigger file: " + filename + " with " + content);

    }

    private String readContent()
        throws IOException {
        BufferedReader reader = null;
        StringBuffer buff = new StringBuffer();

        try {
            reader = new BufferedReader(new FileReader(filename));
            String tmps = reader.readLine();
            while (tmps != null) {
                buff.append(tmps).append(System.getProperty("line.separator"));
                tmps = reader.readLine();
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }

        String tmps = buff.toString().trim();
        logger.debug("Read " + tmps + " from trigger file: " + filename);
        return tmps;

    }
}
