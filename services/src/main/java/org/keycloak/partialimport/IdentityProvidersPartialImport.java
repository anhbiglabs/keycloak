/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.partialimport;

import java.util.List;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.services.resources.admin.IdentityProviderResource;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class IdentityProvidersPartialImport extends AbstractPartialImport<IdentityProviderRepresentation> {

    @Override
    public List<IdentityProviderRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getIdentityProviders();
    }

    @Override
    public String getName(IdentityProviderRepresentation idpRep) {
        return idpRep.getAlias();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, IdentityProviderRepresentation idpRep) {
        return realm.getIdentityProviderByAlias(getName(idpRep)) != null;
    }

    @Override
    public String existsMessage(IdentityProviderRepresentation idpRep) {
        return "Identity Provider '" + getName(idpRep) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.IDP;
    }

    @Override
    public void overwrite(RealmModel realm, KeycloakSession session, IdentityProviderRepresentation idpRep) {
        IdentityProviderResource.updateIdpFromRep(idpRep, realm, session);
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, IdentityProviderRepresentation idpRep) {
        IdentityProviderModel identityProvider = RepresentationToModel.toModel(idpRep);
        realm.addIdentityProvider(identityProvider);
    }

}
