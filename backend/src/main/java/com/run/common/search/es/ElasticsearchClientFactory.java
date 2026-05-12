package com.run.common.search.es;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ElasticsearchClientFactory {

    private ElasticsearchClientFactory() {
    }

    public static ElasticsearchResources create(String serverUrl) throws URISyntaxException {
        Rest5Client restClient = Rest5Client.builder(HttpHost.create(serverUrl)).build();
        ElasticsearchTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchAsyncClient asyncClient = new ElasticsearchAsyncClient(transport);
        return new ElasticsearchResources(restClient, transport, asyncClient);
    }

    public static ElasticsearchResources create(String host, int port, String scheme) throws URISyntaxException {
        return create(scheme + "://" + host + ":" + port);
    }

    public static ElasticsearchResources create(String serverUrl, String username, String password) throws URISyntaxException {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        Rest5Client restClient = Rest5Client.builder(HttpHost.create(serverUrl))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "Basic " + credentials)
                })
                .build();

        ElasticsearchTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchAsyncClient asyncClient = new ElasticsearchAsyncClient(transport);
        return new ElasticsearchResources(restClient, transport, asyncClient);
    }

    public static ElasticsearchResources create(String host, int port, String scheme, String username, String password) throws URISyntaxException {
        return create(scheme + "://" + host + ":" + port, username, password);
    }
}
