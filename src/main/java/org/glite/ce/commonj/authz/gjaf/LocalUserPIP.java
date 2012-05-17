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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.glite.ce.commonj.authz.AuthZConstants;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;

public class LocalUserPIP
    implements ServicePIP {

    private static final Logger logger = Logger.getLogger(LocalUserPIP.class.getName());

    public static final String GLEXEC_BIN_PATH = "glexec_bin_path";

    public static final String GLEXEC_PROBE_CMD = "glexec_probe_cmd";

    public static final String ALLOWED_OPS = "methods";

    public static final Pattern uidPattern = Pattern.compile("uid=\\d+\\(([^)]+)\\)");

    public static final Pattern gidPattern = Pattern.compile("gid=\\d+\\(([^)]+)\\)");

    public static final Pattern groupsPattern = Pattern.compile("groups=(\\S+)");

    public static final Pattern groupsSubPattern = Pattern.compile("\\d+\\(([^)]+)\\)");

    private String id;

    private String glexecPath;

    private String probeCmd;

    private String[] opList;

    public LocalUserPIP() {
        this("undef");
    }

    public LocalUserPIP(String id) {
        this.id = id;
        logger.debug("Created " + id);
    }

    public void initialize(ChainConfig config, String name, String id)
        throws InitializeException {
        logger.debug("Initializing LocalUserPIP");
        glexecPath = (String) config.getProperty(name, GLEXEC_BIN_PATH);
        if (glexecPath == null) {
            logger.error("Missing parameter " + GLEXEC_BIN_PATH);
            throw new InitializeException("Missing parameter " + GLEXEC_BIN_PATH);
        }

        probeCmd = (String) config.getProperty(name, GLEXEC_PROBE_CMD);
        if (probeCmd == null) {
            logger.error("Missing parameter " + GLEXEC_PROBE_CMD);
            throw new InitializeException("Missing parameter " + GLEXEC_PROBE_CMD);
        }

        String tmps = (String) config.getProperty(name, ALLOWED_OPS);
        if (tmps == null) {
            logger.error("Missing parameter " + ALLOWED_OPS);
            throw new InitializeException("Missing parameter " + ALLOWED_OPS);
        }

        opList = parseOpList(tmps);

        logger.debug("Initialized LocalUserPIP: " + id);
    }

    public String getId() {
        return id;
    }

    public void setProperty(String name, String value)
        throws InitializeException {
        if (name.equals(GLEXEC_BIN_PATH)) {
            glexecPath = value;
        }
        if (name.equals(GLEXEC_PROBE_CMD)) {
            probeCmd = value;
        }
        if (name.equals(ALLOWED_OPS)) {
            opList = parseOpList(value);
        }
    }

    public String getProperty(String name) {
        if (name.equals(GLEXEC_BIN_PATH)) {
            return glexecPath;
        }
        if (name.equals(GLEXEC_PROBE_CMD)) {
            return probeCmd;
        }
        if (name.equals(ALLOWED_OPS)) {
            StringBuffer buffer = new StringBuffer();
            for (int k = 0; k < opList.length; k++) {
                if (k == 0) {
                    buffer.append(opList[k]);
                } else {
                    buffer.append(",").append(opList[k]);
                }
            }
            return buffer.toString();
        }
        return null;
    }

    public String[] getProperties() {
        return new String[] { GLEXEC_BIN_PATH, GLEXEC_PROBE_CMD, ALLOWED_OPS };
    }

    public boolean isTriggerable(String name) {
        return false;
    }

    public void collectAttributes(Subject peerSubject, ServiceAuthorizationInterface.MessageContext context,
            QName operation)
        throws AuthorizationException {

        boolean missing = true;
        for (String tmps : opList) {
            missing = missing && (!tmps.equals(operation.getLocalPart()));
        }
        if (missing) {
            logger.debug("Operation not enabled: " + operation.getLocalPart());
            return;
        }

        X509Certificate[] certList = (X509Certificate[]) context.getProperty(AuthZConstants.USER_CERTCHAIN_LABEL);
        if (certList == null) {
            logger.error("Cannot retrieve proxy certificate from context");
            throw new AuthorizationException("Cannot retrieve proxy certificate from context");
        }

        File tmpFile = null;
        BufferedWriter tmpFileWriter = null;
        try {
            tmpFile = File.createTempFile("userproxy", ".pem");
            tmpFileWriter = new BufferedWriter(new FileWriter(tmpFile));
            for (X509Certificate certItem : certList) {
                byte[] pemBytes = Base64.encode(certItem.getEncoded());
                tmpFileWriter.write("-----BEGIN CERTIFICATE-----");
                tmpFileWriter.newLine();
                for (int n = 0; n < pemBytes.length; n = n + 64) {
                    if ((pemBytes.length - n) < 64) {
                        tmpFileWriter.write(new String(pemBytes, n, pemBytes.length - n));
                    } else {
                        tmpFileWriter.write(new String(pemBytes, n, 64));
                    }
                    tmpFileWriter.newLine();
                }
                tmpFileWriter.write("-----END CERTIFICATE-----");
                tmpFileWriter.newLine();
                tmpFileWriter.flush();
            }
            logger.debug("Created temporary proxy " + tmpFile.getAbsolutePath());
        } catch (Throwable th) {
            logger.error("Cannot store proxy certificate", th);
            throw new AuthorizationException("Cannot store proxy certificate");
        } finally {
            if (tmpFileWriter != null) {
                try {
                    tmpFileWriter.close();
                } catch (IOException ioEx) {
                    logger.error(ioEx.getMessage(), ioEx);
                }
            }
        }

        int retcod = 0;
        Process chmodProc = null;
        /*
         * TODO chmod as parameter
         */
        String chmodCmd = "chmod 0600 " + tmpFile.getAbsolutePath();
        try {
            chmodProc = Runtime.getRuntime().exec(chmodCmd);
            retcod = chmodProc.waitFor();
        } catch (InterruptedException e) {
            logger.warn("Interrupted call to " + chmodCmd);
            retcod = -1;
        } catch (Exception ex) {
            logger.error("Cannot set permissions to the store proxy certificate", ex);
            retcod = -1;
        } finally {
            if (chmodProc != null) {
                try {
                    chmodProc.getInputStream().close();
                } catch (IOException e) {
                    logger.error("Cannot close instream for " + chmodCmd);
                }
                try {
                    chmodProc.getErrorStream().close();
                } catch (IOException e) {
                    logger.error("Cannot close errstream for " + chmodCmd);
                }
                try {
                    chmodProc.getOutputStream().close();
                } catch (IOException e) {
                    logger.error("Cannot close outstream for " + chmodCmd);
                }
            }
        }

        if (retcod != 0)
            throw new AuthorizationException("Cannot set permissions to the store proxy certificate");

        String[] envp = new String[] { "GLEXEC_MODE=lcmaps_get_account",
                "GLEXEC_CLIENT_CERT=" + tmpFile.getAbsolutePath() };
        String[] cmdLine = new String[] { glexecPath, probeCmd };

        BufferedReader in = null;
        BufferedOutputStream out = null;
        BufferedReader err = null;
        String failureDescr = null;
        try {
            Process proc = Runtime.getRuntime().exec(cmdLine, envp);
            in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            out = new BufferedOutputStream(proc.getOutputStream());
            err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            proc.waitFor();
            if (proc.exitValue() != 0) {
                StringBuffer buff = new StringBuffer("glexec error: ");
                String line = err.readLine();
                while (line != null) {
                    buff.append(line);
                    line = err.readLine();
                }
                failureDescr = buff.toString();
                logger.error(failureDescr);
            } else {
                String tmpDN = (String) context.getProperty(AuthZConstants.USERDN_RFC2253_LABEL);
                UserInfo userInfo = parseGlexecOutput(in);
                if (userInfo.uid == null) {
                    logger.warn("Cannot retrieve user ID from glexec output for: " + tmpDN);
                } else {
                    logger.debug("Mapped " + tmpDN + " into " + userInfo.uid);
                    context.setProperty(AuthZConstants.LOCAL_USER_ID, userInfo.uid);
                }

                if (userInfo.gid == null) {
                    logger.warn("Cannot retrieve group ID from glexec output for: " + tmpDN);
                } else {
                    logger.debug("Gid for " + tmpDN + ": " + userInfo.gid);
                    context.setProperty(AuthZConstants.LOCAL_GROUP_ID, userInfo.gid);
                }

                if (userInfo.groups.size() > 0) {
                    String[] tmpgrp = new String[userInfo.groups.size()];
                    userInfo.groups.toArray(tmpgrp);
                    context.setProperty(AuthZConstants.LOCAL_GROUPS_LIST, tmpgrp);
                }
            }

        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            failureDescr = th.getMessage();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioEx) {
                    logger.error(ioEx.getMessage(), ioEx);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioEx) {
                    logger.error(ioEx.getMessage(), ioEx);
                }
            }
            if (err != null) {
                try {
                    err.close();
                } catch (IOException ioEx) {
                    logger.error(ioEx.getMessage(), ioEx);
                }
            }

            if (tmpFile != null && !tmpFile.delete()) {
                logger.error("Cannot delete file " + tmpFile.getAbsolutePath());
            }
        }

        if (failureDescr != null) {
            throw new AuthorizationException("Failed to get the local user id via glexec: " + failureDescr);
        }
    }

    public void close()
        throws CloseException {

    }

    public Object clone() {
        LocalUserPIP result = new LocalUserPIP(id);
        result.glexecPath = glexecPath;
        result.probeCmd = probeCmd;
        result.opList = opList;
        return result;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LocalUserPIP))
            return false;

        LocalUserPIP pip = (LocalUserPIP) obj;
        return this.id.equals(pip.id);
    }

    private String[] parseOpList(String opStr) {
        StringTokenizer strok = new StringTokenizer(opStr, ",");
        ArrayList<String> tmpList = new ArrayList<String>();
        while (strok.hasMoreTokens()) {
            String tmps = strok.nextToken().trim();
            if (tmps.length() > 0) {
                tmpList.add(tmps);
                logger.debug("Registered allowed operation " + tmps);
            }
        }

        String[] result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    private UserInfo parseGlexecOutput(BufferedReader reader)
        throws IOException {
        UserInfo result = new UserInfo();
        String line = reader.readLine();
        while (line != null) {
            Matcher uidMatcher = uidPattern.matcher(line);
            if (uidMatcher.find()) {
                result.uid = uidMatcher.group(1);
            }
            Matcher gidMatcher = gidPattern.matcher(line);
            if (gidMatcher.find()) {
                result.gid = gidMatcher.group(1);
            }
            Matcher groupMatcher = groupsPattern.matcher(line);
            if (groupMatcher.find()) {
                String grpStr = groupMatcher.group(1);
                groupMatcher = groupsSubPattern.matcher(grpStr);
                while (groupMatcher.find()) {
                    String tmps = groupMatcher.group(1);
                    result.groups.add(tmps);
                }
            }
            line = reader.readLine();
        }

        if (result.groups.size() == 0) {
            result.groups.add(result.gid);
        }

        return result;
    }

    private class UserInfo {
        public String uid;

        public String gid;

        public ArrayList<String> groups;

        public UserInfo() {
            uid = null;
            gid = null;
            groups = new ArrayList<String>(1);
        }
    }
}
