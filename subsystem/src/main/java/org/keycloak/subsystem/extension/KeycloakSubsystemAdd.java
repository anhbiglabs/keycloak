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

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * The Keycloak subsystem add update handler.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
class KeycloakSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final KeycloakSubsystemAdd INSTANCE = new KeycloakSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        System.out.println("*********** KeycloakSubsystemAdd.populateModel ***********");
        System.out.println("operation=" + operation.toString());
        System.out.println("model = " + model.toString());
        System.out.println("***************************************");
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        System.out.println("*********** KeycloakSubsystemAdd.performBoottime ***********");
        System.out.println("full model = " + model.toString());
        System.out.println("***************************************");
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(KeycloakExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, 0, new KeycloakDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.performRuntime(context, operation, model, verificationHandler, newControllers); //To change body of generated methods, choose Tools | Templates.
        System.out.println("*********** KeycloakSubsystemAdd.performRuntime ***********");
        System.out.println("operation=" + operation.toString());
        System.out.println("model = " + model.toString());
        System.out.println("***************************************");
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
