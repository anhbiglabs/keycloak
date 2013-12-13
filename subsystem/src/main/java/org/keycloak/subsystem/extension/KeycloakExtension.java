/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.extension;

import java.util.Collections;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.keycloak.subsystem.logging.KeycloakLogger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.operations.common.Util;

/**
 * Main Extension class for the subsystem.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "keycloak";
    public static final String NAMESPACE = "urn:jboss:domain:keycloak:1.0";
    private static final KeycloakSubsystemParser PARSER = new KeycloakSubsystemParser();
    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = KeycloakExtension.class.getPackage().getName() + ".LocalDescriptions";
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final ResourceDefinition KEYCLOAK_SUBSYSTEM_RESOURCE = new KeycloakSubsystemDefinition();
    static final RealmDefinition REALM_DEFINITION = new RealmDefinition();
    static final SecureDeploymentDefinition SECURE_DEPLOYMENT_DEFINITION = new SecureDeploymentDefinition();

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, KeycloakExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, KeycloakExtension.NAMESPACE, PARSER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final ExtensionContext context) {
        KeycloakLogger.ROOT_LOGGER.debug("Activating Keycloak Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(KEYCLOAK_SUBSYSTEM_RESOURCE);
        registration.registerSubModel(REALM_DEFINITION);
        subsystem.registerXMLElementWriter(PARSER);
    }

    /**
     * The subsystem parser, which uses stax to read and write to and from xml
     */
    private static class KeycloakSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
            // Require no attributes
            ParseUtils.requireNoAttributes(reader);

            ModelNode addKeycloakSub = Util.createAddOperation(PathAddress.pathAddress(PATH_SUBSYSTEM));
            list.add(addKeycloakSub);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (!reader.getLocalName().equals("realm")) {
                    throw ParseUtils.unexpectedElement(reader);
                }

                readRealm(reader, list);

        /*        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    // move to next <realm> element
                    // TODO: find a more proper way to do this
                } */
            }
        }

        private void readRealm(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            String name = null;
            String attr = reader.getAttributeLocalName(0);
            if (attr.equals("name")) {
                name = reader.getAttributeValue(0);
            }

            if (name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
            }

            ModelNode addRealm = new ModelNode();
            addRealm.get(OP).set(ADD);
            PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                    PathElement.pathElement("realm", name));
            addRealm.get(OP_ADDR).set(addr.toModelNode());

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                addRealm.get(reader.getLocalName()).set(reader.getElementText());
            }

     /*       System.out.println("addRealmOp=" + addRealm.toString());
            validateRequired(addRealm, "realm", "realm-public-key");
            validateRequired(addRealm, "realm", "auth-url");
            validateRequired(addRealm, "realm", "code-url"); */

            list.add(addRealm);

        }

        private void validateRequired(ModelNode addOperation, String parentName, String elementName) throws XMLStreamException {
            if (!addOperation.hasDefined(elementName)) {
                throw new XMLStreamException(elementName + " is required for " + parentName);
            }
        }

        private void readSecureDeployment(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            String name = null;
            String url = null;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attr = reader.getAttributeLocalName(i);
                if (attr.equals("name")) {
                    name = reader.getAttributeValue(i);
                    continue;
                }

                if (attr.equals("realm-url")) {
                    url = reader.getAttributeValue(i);
                    continue;
                }

                throw ParseUtils.unexpectedAttribute(reader, i);
            }

            if (name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
            }

            ModelNode addSecureDeployment = new ModelNode();
            addSecureDeployment.get(OP).set(ADD);
            PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                    PathElement.pathElement("secure-deployment", name));
            addSecureDeployment.get(OP_ADDR).set(addr.toModelNode());

      /* what was this for????
            ModelNode descriptionNode = new ModelNode();
            descriptionNode.get("URL").set(url);
       */

            addSecureDeployment.get("realm-url").set(url);

            list.add(addSecureDeployment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(KeycloakExtension.NAMESPACE, false);
            writeRealms(writer, context);
            writer.writeEndElement();
        }

        private void writeRealms(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            if (!context.getModelNode().get("realm").isDefined()) {
                return;
            }
            for (Property realm : context.getModelNode().get("realm").asPropertyList()) {
                writer.writeStartElement("realm");
                writer.writeAttribute("name", realm.getName());

                ModelNode realmElements = realm.getValue();
                for (SimpleAttributeDefinition element : RealmDefinition.ATTRIBUTES) {
                    element.marshallAsElement(realmElements, writer);
                }
              /*  ModelNode realmElements = realm.getValue();
                for (Property element : realmElements.asPropertyList()) {
                    writer.writeStartElement(element.getName());
                    writer.writeCharacters(element.getValue().asString());
                    writer.writeEndElement();
                } */
                writer.writeEndElement();
            }
        }

        private void writeSecureDeployments(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            if (!context.getModelNode().get("secure-deployment").isDefined()) {
                return;
            }
            for (Property deployment : context.getModelNode().get("secure-deployment").asPropertyList()) {
                writer.writeStartElement("secure-deployment");
                writer.writeAttribute("name", deployment.getName());
                writer.writeAttribute("URL", deployment.getValue().get("URL").asString());
                writer.writeEndElement();
            }
        }
    }
}
