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
 * Authors: Luigi Zangrando (zangrando@pd.infn.it)
 *
 */

package org.glite.ce.commonj.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.glite.ce.commonj.authz.AuthZConstants;
import org.italiangrid.voms.VOMSAttribute;

import eu.emi.security.authn.x509.impl.OpensslNameUtils;

public class CEUtils {
    private static final Logger logger = Logger.getLogger(CEUtils.class);

    public static synchronized File copyFile(String src, String dst)
        throws IOException {
        File srcFile = new File(src);
        File dstFile = new File(dst);

        if (dstFile.isDirectory()) {
            dstFile = new File(dst + "/" + srcFile.getName());
        }

        dstFile.createNewFile();

        InputStream in = new FileInputStream(srcFile);
        OutputStream out = new FileOutputStream(dstFile);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();

        in.close();
        out.close();

        return dstFile;
    }

    public static boolean deleteDir(File dir) {
        emptyDir(dir);
        return dir.delete();
    }

    public static boolean deleteDir(String path) {
        File dir = new File(path);
        return deleteDir(dir);
    }

    public static void emptyDir(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                emptyDir(files[i]);
            }
            files[i].delete();
        }
    }

    public static List<String> getFQAN(String vo) {
        MessageContext messageContext = MessageContext.getCurrentMessageContext();

        @SuppressWarnings("unchecked")
        List<VOMSAttribute> vomsList = (List<VOMSAttribute>) messageContext
                .getProperty(AuthZConstants.USER_VOMSATTRS_LABEL);
        List<String> result = new ArrayList<String>(0);

        if (vomsList != null) {
            for (VOMSAttribute vomsAttr : vomsList) {
                if (vo == null || vo.equals(vomsAttr.getVO())) {
                    result.addAll(vomsAttr.getFQANs());
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<VOMSAttribute> getVOMSAttributes() {
        return (List<VOMSAttribute>) MessageContext.getCurrentMessageContext().getProperty(
                AuthZConstants.USER_VOMSATTRS_LABEL);
    }

    public static String getLocalUser() {
        return (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.LOCAL_USER_ID);
    }

    public static String getLocalUserGroup() {
        return (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.LOCAL_GROUP_ID);
    }

    public static Properties getLoggingParam(Object info) {
        Properties logProps = new Properties();
        Hashtable<String, String> containerConfigEnv = new Hashtable<String, String>(2);
        containerConfigEnv.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        containerConfigEnv.put(Context.URL_PKG_PREFIXES, "org.apache.naming");

        try {
            Context context = (Context) (new InitialContext(containerConfigEnv)).lookup("java:comp/env");
            File logConfFile = new File((String) context.lookup("log_configuration_file"));

            FileInputStream in = null;

            try {
                in = new FileInputStream(logConfFile);
                logProps.load(in);
            } finally {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        } catch (Exception e) {
        }

        if (logProps.size() == 0) {

            logProps.setProperty("log4j.rootLogger", "ERROR, fileout");
            logProps.setProperty("log4j.logger.org.glite", "INFO, fileout");
            logProps.setProperty("log4j.appender.fileout", "org.apache.log4j.RollingFileAppender");
            logProps.setProperty("log4j.appender.fileout.File", "${catalina.base}/logs/glite-ce-common.log");
            logProps.setProperty("log4j.appender.fileout.MaxFileSize", "500KB");
            logProps.setProperty("log4j.appender.fileout.MaxBackupIndex", "1");
            logProps.setProperty("log4j.appender.fileout.layout", "org.apache.log4j.PatternLayout");
            logProps.setProperty("log4j.appender.fileout.layout.ConversionPattern",
                    "%d{dd MMM yyyy HH:mm:ss,SSS} %c - %m%n");

        }

        return logProps;
    }

    public static Object getMessageContextProperty(String name) {
        return name == null ? null : MessageContext.getCurrentMessageContext().getProperty(name);
    }

    public static String getPEM(X509Certificate[] certChain)
        throws CertificateEncodingException {
        if (certChain == null) {
            return "";
        }

        StringBuffer result = new StringBuffer();

        for (int k = 0; k < certChain.length; k++) {
            byte[] pemBytes = Base64.encode(certChain[k].getEncoded());

            result.append("-----BEGIN CERTIFICATE-----\n");
            for (int n = 0; n < pemBytes.length; n = n + 64) {

                if ((pemBytes.length - n) < 64) {
                    result.append(new String(pemBytes, n, pemBytes.length - n));
                } else {
                    result.append(new String(pemBytes, n, 64));
                }

                result.append("\n");
            }

            result.append("-----END CERTIFICATE-----\n");
        }

        return result.toString();
    }

    public static String getRemoteRequestAddress() {
        return (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.REMOTE_REQUEST_ADDRESS);
    }

    public static X509Certificate getUserCert() {
        return (X509Certificate) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.USER_CERT_LABEL);
    }

    public static X509Certificate[] getUserCertChain() {
        return (X509Certificate[]) MessageContext.getCurrentMessageContext().getProperty(
                AuthZConstants.USER_CERTCHAIN_LABEL);
    }

    public static String getUserDefaultVO() {
        return (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.USER_VO_LABEL);
    }

    public static String getUserId() {
        String userFQAN = "";
        String userVO = getUserDefaultVO();
        List<String> fqanlist = null;

        if (userVO != null) {
            fqanlist = getFQAN(userVO);
        }

        if (fqanlist != null && fqanlist.size() > 0) {
            userFQAN = fqanlist.get(0).toString();
        }

        return normalize(getUserDN_RFC2253() + userFQAN);
    }

    public static String getUserDN_RFC2253() {
        return (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.USERDN_RFC2253_LABEL);
    }

    public static String getUserDN_X500() {
        String dn = (String) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.USERDN_RFC2253_LABEL);
        if (dn == null) {
            return null;
        }

        return convertDNfromRFC2253(dn);
    }

    public static boolean isAdmin() {
        Boolean b = (Boolean) MessageContext.getCurrentMessageContext().getProperty(AuthZConstants.IS_ADMIN);

        if (b == null) {
            return false;
        }

        return b.booleanValue();
    }

    public static Object loadObject(String filename)
        throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        ois.close();
        fis.close();

        return obj;
    }

    public static synchronized File makeDir(String path)
        throws IOException {
        File dir = null;

        if (path != null) {
            dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

    public static void makeFile(File file, String message, boolean append, boolean lock)
        throws IOException, IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException("file not specified!");
        }

        if (message == null) {
            throw new IllegalArgumentException("message not specified!");
        }

        if (lock) {
            File tmpFile = File.createTempFile(file.getName(), null, file.getParentFile());
            String chmod_command = "chmod 0600 " + tmpFile.toString();
            int retcod = -1;
            Process chmodProc = null;

            try {
                chmodProc = Runtime.getRuntime().exec(chmod_command);
            } catch (Exception ex) {
                logger.error("Cannot set permissions to the store proxy certificate: " + ex.getMessage());
                retcod = -1;
                throw new IOException("Cannot set permissions to the store proxy certificate: " + ex.getMessage());
            } finally {
                if (chmodProc != null) {
                    try {
                        retcod = chmodProc.waitFor();
                    } catch (Throwable t) {
                        logger.error("makeFile error: " + t.getMessage());
                    }

                    try {
                        chmodProc.getInputStream().close();
                    } catch (IOException e) {
                        logger.error("Cannot close instream for " + chmod_command);
                    }
                    try {
                        chmodProc.getErrorStream().close();
                    } catch (IOException e) {
                        logger.error("Cannot close errstream for " + chmod_command);
                    }
                    try {
                        chmodProc.getOutputStream().close();
                    } catch (IOException e) {
                        logger.error("Cannot close outstream for " + chmod_command);
                    }
                }
            }

            if (retcod != 0) {
                tmpFile.delete();
                throw new IOException("Could not set permissions of file " + tmpFile.toString());
            }

            FileWriter fw = new FileWriter(tmpFile, append);
            fw.write(message);
            fw.flush();
            fw.close();

            // Get a file channel for the file
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

            // Use the file channel to create a lock on the file.
            // This method blocks until it can retrieve the lock.
            FileLock fileLock = channel.lock();

            // runtime.exec("mv " + tmpFile.getAbsoluteFile() + " " +
            // file.getAbsoluteFile());
            try {
                tmpFile.renameTo(file);
            } finally {
                // Release the lock
                fileLock.release();

                // Close the file
                channel.close();
            }
        } else {
            FileWriter fw = new FileWriter(file, append);
            fw.write(message);
            fw.flush();
            fw.close();
        }
    }

    public static void makeFile(String filename, String message, boolean append, boolean lock)
        throws IOException, IllegalArgumentException {
        if (filename == null) {
            throw new IllegalArgumentException("filename not specified!");
        }

        makeFile(new File(filename), message, append, lock);
    }

    public static String normalize(String s) {
        if (s != null) {
            return s.replaceAll("\\W", "_");
        }
        return null;
    }

    public static String readFile(String filename)
        throws IOException {
        String res = "";

        FileReader in = new FileReader(filename);

        char[] buffer = new char[1024];
        int n = 1;

        while (n > 0) {
            n = in.read(buffer, 0, buffer.length);

            if (n > 0) {
                res += new String(buffer, 0, n);
            }
        }

        in.close();
        return res;
    }

    public static synchronized void saveObject(String filename, Object obj)
        throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        fos.close();
    }

    public static String getURIWithoutDN(String uri) {
        String uriWithoutDN = null;
        if ((uri != null) && (!"".equals(uri)) && (uri.indexOf("?DN=") != -1)) {
            uriWithoutDN = uri.substring(0, uri.indexOf("?DN="));
            // see https://savannah.cern.ch/bugs/?59426
            if (uriWithoutDN.charAt(uriWithoutDN.length() - 1) == '\\') {
                uriWithoutDN = uriWithoutDN.substring(0, uriWithoutDN.length() - 1);
            }
            if (uriWithoutDN.charAt(uriWithoutDN.length() - 1) == '\\') {
                uriWithoutDN = uriWithoutDN.substring(0, uriWithoutDN.length() - 1);
            }
        }
        return uriWithoutDN;
    }

    public static XMLGregorianCalendar getXMLGregorianCalendar(Calendar calendar) {
        XMLGregorianCalendar xmlGregorianCalendar = null;

        if (calendar != null) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(calendar.getTimeInMillis());

            try {
                xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            } catch (DatatypeConfigurationException ex) {
                logger.warn(ex.getMessage());
            }
        }

        return xmlGregorianCalendar;
    }

    public static String convertDNfromRFC2253(String dn) {
        /*
         * see CAnL javadoc for any issue related to the conversion
         */
        return OpensslNameUtils.convertFromRfc2253(dn, false);
    }

}
