/*
 * Copyright (c) 2024 Contributors to Eclipse Foundation.
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package ee.jakarta.tck.authentication.test.basic.sam.config;

import static ee.jakarta.tck.authentication.test.basic.servlet.JASPICData.LAYER_SERVLET;
import static ee.jakarta.tck.authentication.test.basic.servlet.JASPICData.LAYER_SOAP;
import static java.util.logging.Level.INFO;

import ee.jakarta.tck.authentication.test.common.logging.server.TSLogger;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.ClientAuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import java.util.Iterator;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

/**
 *
 * @author Raja Perumal
 */
public class TSClientAuthConfig implements jakarta.security.auth.message.config.ClientAuthConfig {

    private static String messageLayer;
    private static String appContext;
    private static CallbackHandler callbackHandler;
    private static TSLogger logger;

    private Map<String, Object> properties;

    /**
     * Creates a new instance of ClientAuthConfigImpl
     */
    public TSClientAuthConfig(String layer, String applicationCtxt, CallbackHandler cbkHandler, Map<String, Object> props) {
        messageLayer = layer;
        appContext = applicationCtxt;
        callbackHandler = cbkHandler;
        properties = props;
    }

    public TSClientAuthConfig(String layer, String applicationCtxt, CallbackHandler cbkHandler, Map<String, Object> props, TSLogger tsLogger) {
        this(layer, applicationCtxt, cbkHandler, props);
        logger = tsLogger;
        String str = "TSClientAuthConfig called for  layer=" + layer + " : appContext=" + applicationCtxt;
        logger.log(INFO, str);
    }

    /**
     * Get the authentication context identifier corresponding to the request and response objects encapsulated in
     * messageInfo.
     *
     * @param messageInfo a contextual Object that encapsulates the client request and server response objects.
     *
     * @return the operation identifier related to the encapsulated request and response objects, or null.
     *
     * @throws IllegalArgumentException if the type of the message objects incorporated in messageInfo are not compatible
     * with the message types supported by this authentication context configuration object.
     */
    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        logger.log(INFO, "TSClientAuthConfig.getOperation called");

        if (messageLayer.equals(LAYER_SOAP)) {
            return getOpName((SOAPMessage) messageInfo.getRequestMessage());
        }

        if (messageLayer.equals(LAYER_SERVLET)) {
            HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
            return request.getServletPath() + " " + request.getMethod();
        }

        return null;
    }

    private String getOpName(SOAPMessage message) {
        if (message == null) {
            return null;
        }

        String opName = null;

        // First look for a SOAPAction header.
        // this is what .net uses to identify the operation
        MimeHeaders headers = message.getMimeHeaders();
        if (headers != null) {
            String[] actions = headers.getHeader("SOAPAction");
            if (actions != null && actions.length > 0) {
                opName = actions[0];
                if (opName != null && opName.equals("\"\"")) {
                    opName = null;
                }
            }
        }

        // If that doesn't work then we default to trying the name
        // of the first child element of the SOAP envelope.
        if (opName == null) {
            Name name = getName(message);
            if (name != null) {
                opName = name.getLocalName();
            }
        }

        return opName;
    }

    private Name getName(SOAPMessage message) {
        Name name = null;

        SOAPPart soap = message.getSOAPPart();
        if (soap != null) {
            try {
                SOAPEnvelope envelope = soap.getEnvelope();
                if (envelope != null) {
                    SOAPBody body = envelope.getBody();
                    if (body != null) {
                        Iterator<?> it = body.getChildElements();
                        while (it.hasNext()) {
                            Object o = it.next();
                            if (o instanceof SOAPElement soapElement) {
                                name = soapElement.getElementName();
                                break;
                            }
                        }
                    }
                }
            } catch (SOAPException se) {
                logger.log(INFO, "WSS: Unable to get SOAP envelope");
            }
        }

        return name;
    }

    /**
     * Causes a dynamic anthentication context configuration object to update the internal state that it uses to process
     * calls to its <code>getAuthContext</code> method.
     *
     * @exception AuthException if an error occured during the update.
     *
     * @exception SecurityException if the caller does not have permission to refresh the configuration object.
     */
    @Override
    public void refresh() {

    }

    /**
     * Get the message layer name of this authentication context configuration object.
     *
     * @return the message layer name of this configuration object, or null if the configuration object pertains to an
     * unspecified message layer.
     */
    @Override
    public String getMessageLayer() {
        return messageLayer;
    }

    /**
     * Get the application context identifier of this authentication context configuration object.
     *
     * @return the String identifying the application context of this configuration object or null if the configuration
     * object pertains to an unspecified application context.
     */
    @Override
    public String getAppContext() {
        return appContext;
    }

    /**
     * Get a ClientAuthContext instance from this ClientAuthConfig.
     *
     * <p>
     * The implementation of this method returns a ClientAuthContext instance that encapsulates the ClientAuthModules used
     * to secure and validate requests/responses associated with the given <i>operation</i>.
     *
     * <p>
     * Specifically, this method accesses this ClientAuthConfig object with the argument <i>operation</i> to determine the
     * ClientAuthModules that are to be encapsulated in the returned ClientAuthContext instance.
     *
     * <P>
     * The ClientAuthConfig object establishes the request and response MessagePolicy objects that are passed to the
     * encapsulated modules when they are initialized by the returned ClientAuthContext instance. It is the modules'
     * responsibility to enforce these policies when invoked.
     *
     * @param operation an operation identifier used to index the provided <i>config</i>, or null. This value must be
     * identical to the value returned by the <code>getOperation</code> method for all <code>MessageInfo</code> objects
     * passed to the <code>secureRequest</code> method of the returned ClientAuthContext.
     *
     * @param clientSubject a Subject that represents the source of the service request to be secured by the acquired
     * authentication context. The principal and/or credentials of the Subject may be used to select or acquire the
     * authentication context. If the Subject is not null, additional Principals or credentials (pertaining to the source of
     * the request) may be added to the Subject. A null value may be passed to for this parameter.
     *
     * @param properties a Map object that may be used by the caller to augment the properties that will be passed to the
     * encapsulated modules at module initialization. The null value may be passed for this parameter.
     *
     * @return a ClientAuthContext instance that encapsulates the ClientAuthModules used to secure and validate
     * requests/responses associated with the given <i>operation</i>, or null (indicating that no modules are configured).
     *
     * @exception AuthException if this operation fails.
     */
    @Override
    public ClientAuthContext getAuthContext(String operation, Subject clientSubject, Map<String, Object> properties) throws AuthException {
        // Copy properties that are passed in this method to this.properties
        //
        // Note: this.properties is obtained from the Provider which gets those
        // properties during provider registration time from the factory.
        this.properties.putAll(properties);

        try {

            String logStr = "TSClientAuthConfig.getAuthContext:  layer=" + messageLayer + " : appContext=" + appContext;
            logger.log(INFO, logStr);

            logger.log(INFO, "TSClientAuthConfig.getAuthContext:  layer=" + messageLayer + " : appContext=" + appContext
                    + " operationId=" + operation);

            ClientAuthContext clientAuthContext = new TSClientAuthContext(messageLayer, appContext, callbackHandler, operation,
                    clientSubject, this.properties, logger);

            logStr = "TSClientAuthConfig.getAuthContext: returned non-null" + " ClientAuthContext for operationId=" + operation;
            logger.log(INFO, logStr);

            return clientAuthContext;
        } catch (Exception e) {
            throw new AuthException(e.getMessage());
        }

    }

    /**
     * Used to determine whether the authentication context configuration object encapsulates any protected authentication
     * contexts.
     *
     * @return true if the configuration object encapsulates at least one protected authentication context. Otherwise, this
     * method returns false.
     */
    @Override
    public boolean isProtected() {
        // To verify protected code path, always return true.
        return true;
    }

}
