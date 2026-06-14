package io.crest.api.permissions.relation.api;

import io.crest.exception.CrestException;

public interface RelationApi {
    Long getDsResource(Long id);

    Long datasetResource(Long id);

    void checkAuth() throws CrestException;
}
