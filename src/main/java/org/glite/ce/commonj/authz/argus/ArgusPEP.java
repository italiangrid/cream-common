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

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
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
import org.glite.authz.pep.client.config.PEPClientConfiguration;
import org.glite.authz.pep.profile.AbstractAuthorizationProfile;
import org.glite.ce.commonj.authz.AuthZConstants;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.italiangrid.voms.VOMSAttribute;

import eu.emi.security.authn.x509.impl.CertificateUtils;

public class ArgusPEP
    extends PEPClient
    implements ServiceAuthorizationInterface {

    private static Logger logger = Logger.getLogger(ArgusPEP.class.getName());

    public static final String ID_ISSUER_ID = "http://glite.org/xacml/attribute/subject-issuer";

    private IssuerCache issuerCache;

    private ActionMappingInterface actionMap;

    private String resourceID;

    public ArgusPEP(PEPClientConfiguration pepConf, String resId, String mapClass) throws PEPClientException {

        super(pepConf);
        try {
            Class<?> mClass = Class.forName(mapClass);
            actionMap = (ActionMappingInterface) mClass.newInstance();
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.error("Cannot load mapping class", ex);
            } else {
                logger.error("Cannot load mapping class: " + ex.getMessage());
            }
            throw new PEPClientException("Cannot load mapping class");
        }

        issuerCache = IssuerCache.getCache();

        logger.debug("Initialized argus pep client");

        resourceID = resId;

    }

    public boolean isPermitted(javax.security.auth.Subject peerSubject, MessageContext context, QName operation)
        throws AuthorizationException {

        String tmpDN = (String) context.getProperty(AuthZConstants.USERDN_RFC2253_LABEL);
        logger.debug("Calling argus pep client for " + tmpDN);

        try {

            Boolean tmpBool = (Boolean) context.getProperty(AuthZConstants.IS_ADMIN);
            boolean isAdmin = tmpBool != null && tmpBool.booleanValue();

            AbstractAuthorizationProfile ceProfile = actionMap.getProfile();

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

            X509Certificate[] certs = (X509Certificate[]) context.getProperty(AuthZConstants.USER_CERTCHAIN_LABEL);
            X509Certificate userCert = (X509Certificate) context.getProperty(AuthZConstants.USER_CERT_LABEL);

            Attribute attrIssuerId = new Attribute();
            attrIssuerId.setId(ID_ISSUER_ID);
            attrIssuerId.setDataType(Attribute.DT_X500_NAME);
            attrIssuerId.getValues().addAll(issuerCache.getIssuerIdList(userCert));
            sbj.getAttributes().add(attrIssuerId);

            @SuppressWarnings("unchecked")
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

                for (VOMSAttribute vomsAttr : vomsList) {
                    for (String fqan : vomsAttr.getFQANs()) {
                        /*
                         * Assumption: the first FQAN is the primary FQAN
                         */
                        if (attrPrimaryFQAN.getValues().size() == 0) {
                            attrPrimaryFQAN.getValues().add(fqan);
                        }

                        attrFQAN.getValues().add(fqan);

                    }

                    attrVO.getValues().add(vomsAttr.getVO());
                }

                if (!attrPrimaryFQAN.getValues().isEmpty()) {
                    sbj.getAttributes().add(attrPrimaryFQAN);
                } else {
                    logger.warn("found empty attrPrimaryFQAN DN=" + tmpDN + " operation=" + operation);
                }

                if (!attrFQAN.getValues().isEmpty()) {
                    sbj.getAttributes().add(attrFQAN);
                }

                if (!attrVO.getValues().isEmpty()) {
                    sbj.getAttributes().add(attrVO);
                }
            }

            Attribute attrKeyInfo = new Attribute();
            attrKeyInfo.setId(Attribute.ID_SUB_KEY_INFO);
            attrKeyInfo.setDataType(Attribute.DT_STRING);

            ByteArrayOutputStream inStr = new ByteArrayOutputStream();
            CertificateUtils.saveCertificateChain(inStr, certs, CertificateUtils.Encoding.PEM);
            String keyInfo = inStr.toString();
            attrKeyInfo.getValues().add(keyInfo);
            sbj.getAttributes().add(attrKeyInfo);

            Resource ceResource = ceProfile.createResourceId(resourceID);

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
            if (logger.isDebugEnabled()) {
                logger.error(ex.getMessage(), ex);
            } else {
                logger.error(ex.getMessage());
            }
            throw new AuthorizationException(ex.getMessage(), ex);
        }

        actionMap.checkMandatoryProperties(context.getPropertyNames());

        return true;
    }

}
