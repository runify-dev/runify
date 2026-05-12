package com.run.dagger.module;


import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.transport.ElasticsearchTransportConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.config.AppConfig;
import com.run.common.config.Search;
import com.run.common.config.SearchType;
import com.run.common.search.ElasticsearchSearchClient;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchClients;
import com.run.common.util.JacksonUtils;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class SearchModule {


    @SneakyThrows
    @Inject
    @Singleton
    @Provides
    public SearchClient searchClient(AppConfig config, Vertx vertx) {
        Search search = config.getSearch();
        if (search == null || search.getType() == SearchType.LOCAL) {
            String dataPath = config.getSystem().getDataPath();
            Path indexPath = Path.of(dataPath + "/lucene");
            if (!Files.exists(indexPath)) {
                Files.createDirectories(indexPath);
            }
            return SearchClients.lucene(
                    indexPath,
                    new StandardAnalyzer(),
                    new ObjectMapper(),
                    List.of("title", "content", "summary")
            );
        } else {
            return new ElasticsearchSearchClient(new ElasticsearchAsyncClient(new ElasticsearchTransportConfig.Builder()
                    .usernameAndPassword(search.getUsername(), search.getPassword())
                    .hosts(search.getHosts().stream().map(url -> {
                        try {
                            return new URI(url);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList()).build()),
                    JacksonUtils.getObjectMapper(), List.of("title", "content", "summary"));
        }

    }
}
