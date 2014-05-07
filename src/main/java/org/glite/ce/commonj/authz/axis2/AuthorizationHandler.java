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

package org.glite.ce.commonj.authz.axis2;

import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;
import org.glite.ce.commonj.authz.AdminTable;
import org.glite.ce.commonj.authz.AuthZConstants;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.ServiceAuthorizationInterface;
import org.glite.ce.commonj.authz.VOMSResultCollector;
import org.glite.ce.commonj.configuration.CommonServiceConfig;
import org.italiangrid.voms.VOMSAttribute;
import org.italiangrid.voms.VOMSValidators;
import org.italiangrid.voms.ac.VOMSACValidator;

import eu.emi.security.authn.x509.proxy.ProxyUtils;

public abstract class AuthorizationHandler
    extends AbstractHandler {

    private static final Logger logger = Logger.getLogger(AuthorizationHandler.class.getName());

    public AuthorizationHandler() {
        super();
    }

    public Handler.InvocationResponse invoke(MessageContext msgContext)
        throws AxisFault {

        Object requestProperty = msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        if (requestProperty == null || !(requestProperty instanceof HttpServletRequest)) {
            throw getAuthorizationFault("Cannot retrieve credentials", msgContext);
        }

        HttpServletRequest request = (HttpServletRequest) requestProperty;
        try {
            /*
             * trigger the initialization of the certificate stuff in request
             */
            Integer keySize = (Integer) request.getAttribute("javax.servlet.request.key_size");
            String sslId = (String) request.getAttribute("javax.servlet.request.ssl_session");
            logger.debug("Parsing HTTP request: " + sslId + ":" + keySize.toString());
        } catch (Throwable th) {
            if (logger.isDebugEnabled()) {
                logger.error("Cannot parse HTTP request", th);
            } else {
                logger.error("Cannot parse HTTP request: " + th.getMessage());
            }
            throw getAuthorizationFault("Cannot parse HTTP request", msgContext);
        }

        X509Certificate[] userCertChain = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");

        X509Certificate userCert = ProxyUtils.getEndUserCertificate(userCertChain);
        if (userCert == null) {
            throw getAuthorizationFault("Corrupted credentials", msgContext);
        }
        String remoteAddress = request.getRemoteAddr();

        String dnRFC2253 = userCert.getSubjectX500Principal().getName();

        Subject subject = new Subject();
        subject.getPrincipals().add(userCert.getSubjectX500Principal());
        subject.getPublicCredentials().add(userCertChain);

        VOMSResultCollector collector = new VOMSResultCollector();
        VOMSACValidator validator = VOMSValidators.newValidator(AuthorizationModule.vomsStore,
                AuthorizationModule.validator, collector);
        List<VOMSAttribute> vomsList = validator.validate(userCertChain);

        msgContext.setProperty(AuthZConstants.USERDN_RFC2253_LABEL, dnRFC2253);

        msgContext.setProperty(AuthZConstants.USER_CERT_LABEL, userCert);

        msgContext.setProperty(AuthZConstants.USER_CERTCHAIN_LABEL, userCertChain);

        msgContext.setProperty(AuthZConstants.USER_VOMSATTRS_LABEL, vomsList);

        msgContext.setProperty(AuthZConstants.REMOTE_REQUEST_ADDRESS, remoteAddress);

        if (vomsList.size() > 0) {
            msgContext.setProperty(AuthZConstants.USER_VO_LABEL, vomsList.get(0).getVO());
        }

        QName operation = this.getOperation(msgContext);
        if (operation == null) {
            throw getAuthorizationFault("Cannot recognize operation", msgContext);
        }
        logger.debug("Checking operation " + operation);

        CommonServiceConfig commonConfig = CommonServiceConfig.getConfiguration();
        if (commonConfig == null) {
            throw getAuthorizationFault("Authorization layer is not configured", msgContext);
        }

        AdminTable table = commonConfig.getAdminTable();
        if (table != null) {
            Boolean isAdmin = new Boolean(table.contains(dnRFC2253));
            logger.debug("Admin test for " + dnRFC2253 + ": " + isAdmin);

            msgContext.setProperty(AuthZConstants.IS_ADMIN, isAdmin);
        }

        ServiceAuthorizationInterface authzBox = commonConfig.getAuthorizationConfig();
        if (authzBox == null) {
            throw getAuthorizationFault("Authorization layer is not configured", msgContext);
        }

        boolean authorized = false;

        try {

            authorized = authzBox.isPermitted(subject, new MessageContextWrapper(msgContext), operation);

            logger.info(this.getLogInfoString(dnRFC2253, vomsList, operation.toString(), remoteAddress, authorized));

        } catch (AuthorizationException authEx) {
            if (logger.isDebugEnabled()) {
                logger.error("Authorization failure", authEx);
            } else {
                logger.error("Authorization failure: " + authEx.getMessage());
            }
            String fullError = "Authorization failure: " + authEx.getMessage();
            if (collector.size() > 0) {
                fullError += "\n" + collector.toString();
            }
            throw getAuthorizationFault(fullError, msgContext);

        }

        if (!authorized) {

            String fullError = dnRFC2253 + " not authorized for " + operation;
            if (collector.size() > 0) {
                fullError += "\n" + collector.toString();
            }
            throw getAuthorizationFault(fullError, msgContext);
        }

        return Handler.InvocationResponse.CONTINUE;
    }

    protected QName getOperation(MessageContext context) {
        OperationContext opCtx = context.getOperationContext();
        AxisOperation operation = opCtx.getAxisOperation();
        return operation.getName();
    }

    protected abstract AxisFault getAuthorizationFault(String message, MessageContext context);

    // protected abstract CommonServiceConfig getCommonConfiguration();

    private String getLogInfoString(String DN, List<VOMSAttribute> vomsAttrs, String operation, String address,
            boolean authorized) {

        StringBuffer buffer = new StringBuffer("request for OPERATION=");
        buffer.append(operation).append("; REMOTE_REQUEST_ADDRESS=").append(address);
        buffer.append("; USER_DN=").append(DN).append("; ");
        if (vomsAttrs != null && vomsAttrs.size() > 0) {
            buffer.append("USER_FQAN={ ");
            for (VOMSAttribute attr : vomsAttrs) {
                for (String fqan : attr.getFQANs()) {
                    buffer.append(fqan).append("; ");
                }
            }
            buffer.append("}; ");
        }
        if (authorized) {
            buffer.append(" AUTHORIZED");
        } else {
            buffer.append(" NOT AUTHORIZED");
        }

        return buffer.toString();
    }

    public class MessageContextWrapper
        implements ServiceAuthorizationInterface.MessageContext {

        private MessageContext msgCtx;

        MessageContextWrapper(MessageContext ctx) {
            msgCtx = ctx;
        }

        public boolean containsProperty(String name) {
            return msgCtx.getProperty(name) != null;
        }

        public Object getProperty(String name) {
            return msgCtx.getProperty(name);
        }

        public Iterator<String> getPropertyNames() {
            return msgCtx.getPropertyNames();
        }

        public void removeProperty(String name) {
            msgCtx.removeProperty(name);
        }

        public void setProperty(String name, Object value) {
            msgCtx.setProperty(name, value);
        }
    }

}
