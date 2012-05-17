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

package org.glite.ce.commonj.authz.argus;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import org.glite.authz.common.model.Action;
import org.glite.authz.common.model.Attribute;
import org.glite.authz.common.model.AttributeAssignment;
import org.glite.authz.common.model.Environment;
import org.glite.authz.common.model.Obligation;
import org.glite.authz.common.model.Request;
import org.glite.authz.common.model.Resource;
import org.glite.authz.common.model.Response;
import org.glite.authz.common.model.Result;
import org.glite.authz.common.model.Subject;
import org.glite.authz.common.profile.GLiteAuthorizationProfileConstants;
import org.glite.authz.pep.client.PEPClient;
import org.glite.authz.pep.client.PEPClientException;
import org.glite.authz.pep.profile.GridCEAuthorizationProfile;
import org.glite.ce.commonj.authz.AuthZConstants;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.voms.FQAN;
import org.glite.voms.PKIStore;
import org.glite.voms.PKIStoreFactory;
import org.glite.voms.PKIUtils;
import org.glite.voms.VOMSAttribute;

public class ArgusPEP
    extends PEPClient
    implements ServiceAuthorizationInterface {

    private static Logger logger = Logger.getLogger(ArgusPEP.class.getName());

    public static final String ID_ISSUER_ID = "http://glite.org/xacml/attribute/subject-issuer";

    private static HashMap<String, X509Certificate[]> caChains = new HashMap<String, X509Certificate[]>();

    private ActionMappingInterface actionMap;

    private Resource ceResource;

    private String caDir;

    public ArgusPEP(PEPConfigurationItem item) throws PEPClientException {
        super(item);
        try {
            Class<?> mClass = Class.forName(item.getMappingClass());
            actionMap = (ActionMappingInterface) mClass.newInstance();
        } catch (Exception ex) {
            logger.error("Cannot load mapping class", ex);
            throw new PEPClientException("Cannot load mapping class");
        }
        logger.debug("Initializing argus pep client");

        ceResource = GridCEAuthorizationProfile.getInstance().createResourceId(item.getResourceID());

        caDir = item.getCADir();

    }

    public boolean isPermitted(javax.security.auth.Subject peerSubject, MessageContext context, QName operation)
        throws AuthorizationException {

        String tmpDN = (String) context.getProperty(AuthZConstants.USERDN_RFC2253_LABEL);
        logger.debug("Calling argus pep client for " + tmpDN);

        try {

            Boolean tmpBool = (Boolean) context.getProperty(AuthZConstants.IS_ADMIN);
            boolean isAdmin = tmpBool != null && tmpBool.booleanValue();

            GridCEAuthorizationProfile ceProfile = GridCEAuthorizationProfile.getInstance();
            String xAction = actionMap.getXACMLAction(operation);
            if (xAction == null) {
                if (isAdmin) {
                    logger.info("Authorized un-mapped operation " + operation + " for administrator " + tmpDN);
                    return true;
                }
                throw new IllegalArgumentException("Operation " + operation + " not allowed for " + tmpDN);
            }
            Action action = ceProfile.createActionId(xAction);

            Environment env = ceProfile.createEnvironmentProfileId(ceProfile.getProfileId());

            Subject sbj = new Subject();

            Attribute attrSubjectId = new Attribute();
            attrSubjectId.setId(Attribute.ID_SUB_ID);
            attrSubjectId.setDataType(Attribute.DT_X500_NAME);
            attrSubjectId.getValues().add(context.getProperty(AuthZConstants.USERDN_RFC2253_LABEL));
            sbj.getAttributes().add(attrSubjectId);

            X509Certificate[] certs = this.getCompleteCertChain(context);

            Attribute attrIssuerId = new Attribute();
            attrIssuerId.setId(ID_ISSUER_ID);
            attrIssuerId.setDataType(Attribute.DT_X500_NAME);
            for (X509Certificate certItem : certs) {
                if (certItem.getBasicConstraints() >= 0) {
                    logger.debug("Insert issuer ID " + certItem.getIssuerDN().getName());
                    attrIssuerId.getValues().add(certItem.getIssuerDN().getName());
                }
            }
            sbj.getAttributes().add(attrSubjectId);

            List<VOMSAttribute> vomsList = (List<VOMSAttribute>) context
                    .getProperty(AuthZConstants.USER_VOMSATTRS_LABEL);

            if (vomsList != null) {
                Attribute attrPrimaryFQAN = new Attribute();
                attrPrimaryFQAN.setId(GLiteAuthorizationProfileConstants.ID_ATTRIBUTE_PRIMARY_FQAN);
                attrPrimaryFQAN.setDataType(GLiteAuthorizationProfileConstants.DATATYPE_FQAN);

                Attribute attrFQAN = new Attribute();
                attrFQAN.setId(GLiteAuthorizationProfileConstants.ID_ATTRIBUTE_FQAN);
                attrFQAN.setDataType(GLiteAuthorizationProfileConstants.DATATYPE_FQAN);

                Attribute attrVO = new Attribute();
                attrVO.setId(GLiteAuthorizationProfileConstants.ID_ATTRIBUTE_VIRTUAL_ORGANIZATION);
                attrVO.setDataType(Attribute.DT_STRING);

                boolean missingFQAN = true;
                for (VOMSAttribute vomsAttr : vomsList) {
                    List<FQAN> fqanList = (List<FQAN>) vomsAttr.getListOfFQAN();
                    if (fqanList != null) {
                        for (FQAN fqan : fqanList) {
                            /*
                             * Assumption: the first FQAN is the primary FQAN
                             */
                            if (missingFQAN) {
                                attrPrimaryFQAN.getValues().add(fqan.getFQAN());
                                missingFQAN = false;
                            }

                            attrFQAN.getValues().add(fqan.getFQAN());
                        }
                    }

                    attrVO.getValues().add(vomsAttr.getVO());
                }

                sbj.getAttributes().add(attrPrimaryFQAN);
                sbj.getAttributes().add(attrFQAN);
                sbj.getAttributes().add(attrVO);
            }

            Attribute attrKeyInfo = new Attribute();
            attrKeyInfo.setId(Attribute.ID_SUB_KEY_INFO);
            attrKeyInfo.setDataType(Attribute.DT_STRING);
            String keyInfo = this.convertCertToPEMString(certs, 1);
            attrKeyInfo.getValues().add(keyInfo);
            sbj.getAttributes().add(attrKeyInfo);

            Request request = new Request();
            request.setAction(action);
            request.setEnvironment(env);
            request.getResources().add(ceResource);
            request.getSubjects().add(sbj);

            Response response = super.authorize(request);
            List<Result> resList = response.getResults();
            if (resList.size() < 1) {
                throw new IllegalArgumentException("No decision from PEP daemon");
            }

            /*
             * Assumption: just the first result is meaningful
             */
            Result result = resList.get(0);
            if (result.getDecision() != Result.DECISION_PERMIT && !isAdmin) {
                logger.debug("Argus decision: " + result.getDecision());
                return false;
            }

            List<Obligation> obList = result.getObligations();
            for (Obligation obItem : obList) {
                String obId = obItem.getId();
                logger.debug("Found obligation " + obId);
                String propName = actionMap.getAttributeMapping(obId, null);
                if (propName != null) {
                    logger.debug("Registered " + propName + " for " + tmpDN);
                    context.setProperty(propName, "true");
                }

                List<AttributeAssignment> attrList = obItem.getAttributeAssignments();
                for (AttributeAssignment attrItem : attrList) {
                    String attrId = attrItem.getAttributeId();
                    propName = actionMap.getAttributeMapping(obId, attrId);
                    if (propName != null) {
                        String attrValue = attrItem.getValue();
                        logger.debug("Registered " + propName + " for " + tmpDN + " with " + attrValue);
                        context.setProperty(propName, attrValue);
                    }
                }
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new AuthorizationException(ex.getMessage(), ex);
        }

        actionMap.checkMandatoryProperties(context.getPropertyNames());

        return true;
    }

    private String convertCertToPEMString(X509Certificate[] certs, int mode)
        throws IOException {
        StringWriter stringWriter = new StringWriter();
        PEMWriter writer = new PEMWriter(stringWriter);
        for (int k = 0; k < certs.length; k++) {
            X509Certificate cert = certs[k];
            if ((mode == 1 && k == certs.length - 1) || (mode == 2 && cert.getBasicConstraints() >= 0)) {
                continue;
            }
            writer.writeObject(cert);
        }
        try {
            writer.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return stringWriter.toString();
    }

    private X509Certificate[] getCompleteCertChain(MessageContext context) {
        X509Certificate[] certs = (X509Certificate[]) context.getProperty(AuthZConstants.USER_CERTCHAIN_LABEL);
        int caIdx = 0;
        while (caIdx < certs.length && certs[caIdx].getBasicConstraints() < 0) {
            caIdx++;
        }

        X509Certificate[] tail = getChainForCA(certs[caIdx - 1].getIssuerX500Principal());
        X509Certificate[] result = new X509Certificate[caIdx + tail.length];
        for (int k = 0; k < caIdx; k++) {
            result[k] = certs[k];
        }
        for (int k = 0; k < tail.length; k++) {
            result[caIdx + k] = tail[k];
        }
        return result;
    }

    private synchronized X509Certificate[] getChainForCA(X500Principal caPrincipal) {
        long ts = System.currentTimeMillis();
        String caDN = caPrincipal.getName();
        X509Certificate[] result = caChains.get(caDN);

        /*
         * TODO missing daily renewal
         */
        if (result != null && result.length > 0 && result[0].getNotAfter().getTime() > ts) {
            return result;
        }

        try {
            logger.debug("Caching chain for " + caDN);
            ArrayList<X509Certificate> tmpList = new ArrayList<X509Certificate>(5);

            PKIStore localStore = PKIStoreFactory.getStore(caDir, PKIStore.TYPE_CADIR);
            Hashtable<String, Vector<X509Certificate>> caTable = localStore.getCAs();

            X509Certificate currCert = null;
            X500Principal tmpName = caPrincipal;
            do {
                currCert = caTable.get(PKIUtils.getHash(tmpName)).get(0);
                tmpList.add(currCert);
                logger.debug("Cached " + tmpName.getName());
                tmpName = currCert.getIssuerX500Principal();
            } while (!currCert.getIssuerDN().equals(currCert.getSubjectDN()));

            result = new X509Certificate[tmpList.size()];
            tmpList.toArray(result);
            caChains.put(caDN, result);

        } catch (Exception ex) {
            logger.error(ex.getMessage());
            caChains.remove(caDN);
            throw new RuntimeException(ex);
        }
        return result;
    }

}
