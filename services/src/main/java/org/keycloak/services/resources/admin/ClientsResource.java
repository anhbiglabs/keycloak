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
package org.keycloak.services.resources.admin;

import java.io.IOException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.QueryParam;
import org.keycloak.exportimport.PartialExportUtil;
import org.keycloak.representations.idm.PartialImport;

/**
 * Base resource class for managing a realm's clients.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientsResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientsResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @Path("export")
    @GET
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void exportClients(@QueryParam("search") String search,
                            @QueryParam("fileName") String fileName,
                            @QueryParam("condensed") boolean condensed) throws IOException {
        auth.requireView();

        if (search == null) search = "";
        List clients = getClients(search);

        PartialExportUtil.exportRepresentations("clients", clients, fileName, condensed, session, realm);
    }

    /**
     * Get clients belonging to the realm
     *
     * @param search A string that may be a partial match for clientId.
     *
     * @return  a list of clients belonging to the realm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientRepresentation> getClients(@QueryParam("search") String search) {
        auth.requireAny();
        List<ClientRepresentation> rep = new ArrayList<>();
        List<ClientModel> clientModels = realm.getClients();

        boolean view = auth.hasView();
        for (ClientModel clientModel : clientModels) {
            if (view && clientMatches(clientModel, search)) {
                rep.add(ModelToRepresentation.toRepresentation(clientModel));
            }

            if (!view) {
                ClientRepresentation client = new ClientRepresentation();
                client.setId(clientModel.getId());
                client.setClientId(clientModel.getClientId());
                rep.add(client);
            }
        }
        return rep;
    }

    private boolean clientMatches(ClientModel clientModel, String search) {
        if (search == null || search.trim().isEmpty()) return true;

        return clientModel.getClientId().contains(search);
    }

    /**
     * Create a new client
     *
     * Client's client_id must be unique!
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClient(final @Context UriInfo uriInfo, final ClientRepresentation rep) {
        auth.requireManage();

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, realm, rep, true);

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, clientModel.getId()).representation(rep).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    /**
     * Base path for managing a specific client.
     *
     * @param id id of client (not client-id)
     * @return
     */
    @Path("{id}")
    public ClientResource getClient(final @PathParam("id") String id) {
        ClientModel clientModel = realm.getClientById(id);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }

        session.getContext().setClient(clientModel);

        ClientResource clientResource = new ClientResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }

    /**
     * Import Clients from a JSON file.
     *
     * @param uriInfo
     * @param clientImports
     * @return
     */
    @Path("import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importClients(final @Context UriInfo uriInfo, PartialImport clientImports) {
        auth.requireManage();

        boolean overwrite = clientImports.isOverwrite();

        // check all constraints before mass import
        List<ClientRepresentation> clients = clientImports.getClients();
        if (clients == null || clients.isEmpty()) {
            return ErrorResponse.error("No clients to import.", Response.Status.INTERNAL_SERVER_ERROR);
        }

        for (ClientRepresentation rep : clients) {
            if (!overwrite && clientExists(rep)) {
                return ErrorResponse.exists("Client id '" + rep.getClientId() + "' already exists");
            }
        }

        for (ClientRepresentation rep : clients) {
            try {
                if (overwrite && clientExists(rep)) {
                    ClientModel toRemove = realm.getClientByClientId(rep.getClientId());
                    realm.removeClient(toRemove.getId());
                }

                ClientModel client = RepresentationToModel.createClient(session, realm, rep, true);
                adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, client.getId()).representation(rep).success();
            } catch (Exception e) {
                if (session.getTransaction().isActive()) session.getTransaction().setRollbackOnly();
                return ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        if (session.getTransaction().isActive()) {
            session.getTransaction().commit();
        }

        return Response.ok().build();
    }

    private boolean clientExists(ClientRepresentation rep) {
        return realm.getClientByClientId(rep.getClientId()) != null;
    }

}
