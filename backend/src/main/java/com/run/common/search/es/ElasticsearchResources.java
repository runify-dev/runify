package com.run.common.search.es;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

public final class ElasticsearchResources implements AutoCloseable {

    private final Rest5Client restClient;
    private final ElasticsearchTransport transport;
    private final ElasticsearchAsyncClient asyncClient;

    public ElasticsearchResources(
            Rest5Client restClient,
            ElasticsearchTransport transport,
            ElasticsearchAsyncClient asyncClient
    ) {
        this.restClient = restClient;
        this.transport = transport;
        this.asyncClient = asyncClient;
    }

    public Rest5Client getRestClient() {
        return restClient;
    }

    public ElasticsearchTransport getTransport() {
        return transport;
    }

    public ElasticsearchAsyncClient getAsyncClient() {
        return asyncClient;
    }

    @Override
    public void close() throws Exception {
        transport.close();
    }
}
