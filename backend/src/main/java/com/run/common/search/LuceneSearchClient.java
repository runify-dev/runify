package com.run.common.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.PassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class LuceneSearchClient implements SearchClient {

    private static final String FIELD_UID = "__uid";
    private static final String FIELD_INDEX = "__index";
    private static final String FIELD_ID = "__id";
    private static final String FIELD_SOURCE = "__source";

    private static final FieldType HIGHLIGHT_TEXT_FIELD_TYPE;

    static {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.freeze();
        HIGHLIGHT_TEXT_FIELD_TYPE = fieldType;
    }

    private final Directory directory;
    private final Analyzer analyzer;
    private final ObjectMapper objectMapper;
    private final IndexWriter writer;
    private final List<String> defaultSearchFields;
    private volatile DirectoryReader reader;

    public LuceneSearchClient(
            Path path,
            Analyzer analyzer,
            ObjectMapper objectMapper,
            List<String> defaultSearchFields
    ) throws IOException {
        this.directory = FSDirectory.open(path);
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.defaultSearchFields = defaultSearchFields == null ? List.of() : List.copyOf(defaultSearchFields);

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(directory, config);
        this.reader = DirectoryReader.open(writer);
    }

    @Override
    public CompletionStage<Void> index(SearchDocument document) {
        return supplyVoid(() -> {
            writer.updateDocument(uidTerm(document.getIndex(), document.getId()), toLuceneDocument(document));
            writer.commit();
            refreshReader();
        });
    }

    @Override
    public CompletionStage<Void> bulkIndex(Collection<SearchDocument> documents) {
        return supplyVoid(() -> {
            if (documents == null || documents.isEmpty()) {
                return;
            }
            for (SearchDocument document : documents) {
                writer.updateDocument(uidTerm(document.getIndex(), document.getId()), toLuceneDocument(document));
            }
            writer.commit();
            refreshReader();
        });
    }

    @Override
    public CompletionStage<Void> updateById(String index, String id, Map<String, Object> fields) {
        return supplyVoid(() -> {
            if (fields == null || fields.isEmpty()) {
                return;
            }
            Map<String, Object> current = findSourceById(index, id);
            if (current == null) {
                throw new SearchException("document not found, index=" + index + ", id=" + id);
            }

            Map<String, Object> merged = new LinkedHashMap<>(current);
            merged.putAll(fields);

            SearchDocument updated = new SearchDocument(index, id, merged);
            writer.updateDocument(uidTerm(index, id), toLuceneDocument(updated));
            writer.commit();
            refreshReader();
        });
    }

    @Override
    public CompletionStage<Void> deleteById(String index, String id) {
        return supplyVoid(() -> {
            writer.deleteDocuments(uidTerm(index, id));
            writer.commit();
            refreshReader();
        });
    }

    @Override
    public CompletionStage<Void> deleteByIds(String index, Collection<String> ids) {
        return supplyVoid(() -> {
            if (ids == null || ids.isEmpty()) {
                return;
            }
            List<Term> terms = new ArrayList<>();
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    terms.add(uidTerm(index, id));
                }
            }
            if (!terms.isEmpty()) {
                writer.deleteDocuments(terms.toArray(Term[]::new));
                writer.commit();
                refreshReader();
            }
        });
    }

    @Override
    public CompletionStage<Void> deleteByQuery(SearchQuery query) {
        return supplyVoid(() -> {
            refreshReader();
            SearchRequest request = SearchRequest.from(query);
            IndexSearcher searcher = new IndexSearcher(reader);

            Query filterQuery = buildFilterQuery(request);
            Query keywordQuery = buildKeywordQuery(request);

            BooleanQuery.Builder root = new BooleanQuery.Builder();
            if (filterQuery != null) {
                root.add(filterQuery, BooleanClause.Occur.FILTER);
            }
            if (keywordQuery != null) {
                root.add(keywordQuery, BooleanClause.Occur.MUST);
            }

            Query deleteQuery = root.build().clauses().isEmpty() ? new MatchAllDocsQuery() : root.build();

            TopDocs topDocs = searcher.search(deleteQuery, Integer.MAX_VALUE);
            Term[] uids = new Term[topDocs.scoreDocs.length];
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                uids[i] = new Term(FIELD_UID, doc.get(FIELD_UID));
            }

            if (uids.length > 0) {
                writer.deleteDocuments(uids);
                writer.commit();
                refreshReader();
            }
        });
    }

    @Override
    public CompletionStage<Boolean> exists(String index, String id) {
        return supply(() -> {
            refreshReader();
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(new TermQuery(uidTerm(index, id)), 1);
            return topDocs.totalHits.value() > 0;
        });
    }

    @Override
    public CompletionStage<SearchResult<SearchDocument>> search(SearchRequest request) {
        return supply(() -> {
            refreshReader();

            IndexSearcher searcher = new IndexSearcher(reader);
            SearchPlan plan = buildSearchPlan(request);
            int limit = Math.max(request.getFrom() + request.getPageSize(), request.getPageSize());

            SearchExecution execution = executeSearch(searcher, plan.searchQuery(), request, limit);
            long total = execution.topDocs.totalHits.value();
            ScoreDoc[] scoreDocs = execution.topDocs.scoreDocs;

            int start = Math.min(request.getFrom(), scoreDocs.length);
            int end = Math.min(start + request.getPageSize(), scoreDocs.length);

            Map<Integer, Map<String, List<String>>> highlightMap = buildHighlights(
                    searcher,
                    plan.highlightQuery(),
                    execution.topDocs,
                    request
            );

            List<SearchHit<SearchDocument>> hits = new ArrayList<>();
            for (int i = start; i < end; i++) {
                ScoreDoc scoreDoc = scoreDocs[i];
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                String index = doc.get(FIELD_INDEX);
                String id = doc.get(FIELD_ID);
                String sourceJson = doc.get(FIELD_SOURCE);

                Map<String, Object> source = sourceJson == null
                        ? Map.of()
                        : objectMapper.readValue(sourceJson, new TypeReference<Map<String, Object>>() {
                });

                Map<String, Object> filtered = filterReturnFields(source, request.getReturnFields());
                Map<String, List<String>> highlights = highlightMap.getOrDefault(scoreDoc.doc, Map.of());

                hits.add(new SearchHit<>(
                        index,
                        id,
                        scoreDoc.score,
                        new SearchDocument(index, id, filtered),
                        highlights
                ));
            }

            if (execution.reversePageResults) {
                Collections.reverse(hits);
            }

            return new SearchResult<>(total, request.getPageNo(), request.getPageSize(), hits);
        });
    }

    private SearchExecution executeSearch(IndexSearcher searcher, Query query, SearchRequest request, int limit) throws IOException {
        if (request.getSorts().isEmpty()) {
            return new SearchExecution(searcher.search(query, limit), false);
        }

        if (request.getSorts().size() == 1) {
            SearchSort sort = request.getSorts().get(0);
            if (sort.getType() == SearchSort.Type.SCORE) {
                TopDocs topDocs = searcher.search(query, limit);
                return new SearchExecution(topDocs, !sort.isDesc());
            }
        }

        Sort sort = buildLuceneSort(request.getSorts());
        TopFieldDocs topDocs = searcher.search(query, limit, sort);
        return new SearchExecution(topDocs, false);
    }

    private Sort buildLuceneSort(List<SearchSort> sorts) {
        List<SortField> sortFields = new ArrayList<>();
        for (SearchSort sort : sorts) {
            if (sort.getType() == SearchSort.Type.SCORE) {
                sortFields.add(new SortField(null, SortField.Type.SCORE, !sort.isDesc()));
                continue;
            }
            SortField.Type type = switch (sort.getType()) {
                case STRING -> SortField.Type.STRING;
                case INT -> SortField.Type.INT;
                case LONG -> SortField.Type.LONG;
                case BOOLEAN -> SortField.Type.INT;
                default -> throw new SearchException("unsupported sort type: " + sort.getType());
            };
            sortFields.add(new SortField(sortField(sort.getField()), type, sort.isDesc()));
        }
        return new Sort(sortFields.toArray(SortField[]::new));
    }

    private SearchPlan buildSearchPlan(SearchRequest request) throws ParseException {
        Query filterQuery = buildFilterQuery(request);
        Query keywordQuery = buildKeywordQuery(request);
        List<Query> boostQueries = buildExactBoostQueries(request);

        SearchKnn knn = request.getKnn();
        if (knn == null) {
            BooleanQuery.Builder root = new BooleanQuery.Builder();
            if (filterQuery != null) {
                root.add(filterQuery, BooleanClause.Occur.FILTER);
            }
            if (keywordQuery != null) {
                root.add(keywordQuery, BooleanClause.Occur.MUST);
            }
            for (Query boostQuery : boostQueries) {
                root.add(boostQuery, BooleanClause.Occur.SHOULD);
            }
            Query searchQuery = root.build().clauses().isEmpty() ? new MatchAllDocsQuery() : root.build();
            return new SearchPlan(searchQuery, keywordQuery);
        }

        Query knnQuery = filterQuery == null
                ? new KnnFloatVectorQuery(knn.getField(), knn.getVector(), knn.getK())
                : new KnnFloatVectorQuery(knn.getField(), knn.getVector(), knn.getK(), filterQuery);

        if (keywordQuery == null && boostQueries.isEmpty()) {
            return new SearchPlan(knnQuery, null);
        }

        BooleanQuery.Builder hybrid = new BooleanQuery.Builder();
        if (filterQuery != null) {
            hybrid.add(filterQuery, BooleanClause.Occur.FILTER);
        }
        hybrid.add(knnQuery, BooleanClause.Occur.SHOULD);
        if (keywordQuery != null) {
            hybrid.add(keywordQuery, BooleanClause.Occur.SHOULD);
        }
        for (Query boostQuery : boostQueries) {
            hybrid.add(boostQuery, BooleanClause.Occur.SHOULD);
        }
        return new SearchPlan(hybrid.build(), keywordQuery);
    }

    private Query buildFilterQuery(SearchRequest request) {
        BooleanQuery.Builder root = new BooleanQuery.Builder();
        root.add(new TermQuery(new Term(FIELD_INDEX, request.getIndex())), BooleanClause.Occur.FILTER);

        if (!request.getIds().isEmpty()) {
            BooleanQuery.Builder idsQuery = new BooleanQuery.Builder();
            for (String id : request.getIds()) {
                idsQuery.add(new TermQuery(uidTerm(request.getIndex(), id)), BooleanClause.Occur.SHOULD);
            }
            root.add(idsQuery.build(), BooleanClause.Occur.FILTER);
        }

        for (Map.Entry<String, List<String>> entry : request.getExactFilters().entrySet()) {
            String field = exactField(entry.getKey());
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            BooleanQuery.Builder filterGroup = new BooleanQuery.Builder();
            for (String value : values) {
                filterGroup.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.SHOULD);
            }
            root.add(filterGroup.build(), BooleanClause.Occur.FILTER);
        }

        return root.build();
    }

    private Query buildKeywordQuery(SearchRequest request) throws ParseException {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return null;
        }
        List<String> fields = request.getKeywordFields().isEmpty()
                ? defaultSearchFields
                : request.getKeywordFields();

        if (fields == null || fields.isEmpty()) {
            throw new SearchException("keyword search requires keyword fields");
        }

        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields.toArray(String[]::new), analyzer);
        return parser.parse(QueryParser.escape(request.getKeyword()));
    }

    private List<Query> buildExactBoostQueries(SearchRequest request) {
        List<Query> queries = new ArrayList<>();
        for (SearchExactBoost exactBoost : request.getExactBoosts()) {
            queries.add(new BoostQuery(
                    new TermQuery(new Term(exactField(exactBoost.getField()), exactBoost.getValue())),
                    exactBoost.getBoost()
            ));
        }
        return queries;
    }

    private Map<String, Object> findSourceById(String index, String id) throws IOException {
        refreshReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new TermQuery(uidTerm(index, id)), 1);
        if (topDocs.totalHits.value() == 0) {
            return null;
        }
        Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
        String sourceJson = doc.get(FIELD_SOURCE);
        if (sourceJson == null || sourceJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(sourceJson, new TypeReference<Map<String, Object>>() {
        });
    }

    private Document toLuceneDocument(SearchDocument source) throws IOException {
        Document doc = new Document();

        doc.add(new StringField(FIELD_UID, uid(source.getIndex(), source.getId()), Field.Store.YES));
        doc.add(new StringField(FIELD_INDEX, source.getIndex(), Field.Store.YES));
        doc.add(new StringField(FIELD_ID, source.getId(), Field.Store.YES));
        doc.add(new StoredField(FIELD_SOURCE, objectMapper.writeValueAsString(source.getFields())));

        for (Map.Entry<String, Object> entry : source.getFields().entrySet()) {
            addSearchableField(doc, entry.getKey(), entry.getValue());
        }

        return doc;
    }

    private void addSearchableField(Document doc, String fieldName, Object value) {
        if (fieldName == null || fieldName.isBlank() || value == null) {
            return;
        }

        float[] vector = extractVector(fieldName, value);
        if (vector != null) {
            doc.add(new KnnFloatVectorField(fieldName, vector));
            return;
        }

        if (value instanceof CharSequence sequence) {
            String text = sequence.toString();
            if (!text.isBlank()) {
                doc.add(new Field(fieldName, text, HIGHLIGHT_TEXT_FIELD_TYPE));
                doc.add(new StringField(exactField(fieldName), text, Field.Store.NO));
                doc.add(new SortedDocValuesField(sortField(fieldName), new BytesRef(text)));
            }
            return;
        }

        if (value instanceof Integer integer) {
            String text = String.valueOf(integer);
            doc.add(new StringField(exactField(fieldName), text, Field.Store.NO));
            doc.add(new NumericDocValuesField(sortField(fieldName), integer.longValue()));
            return;
        }

        if (value instanceof Long longValue) {
            String text = String.valueOf(longValue);
            doc.add(new StringField(exactField(fieldName), text, Field.Store.NO));
            doc.add(new NumericDocValuesField(sortField(fieldName), longValue));
            return;
        }

        if (value instanceof Boolean boolValue) {
            String text = String.valueOf(boolValue);
            doc.add(new StringField(exactField(fieldName), text, Field.Store.NO));
            doc.add(new NumericDocValuesField(sortField(fieldName), boolValue ? 1L : 0L));
            return;
        }

        if (value instanceof Number || value instanceof Enum<?>) {
            doc.add(new StringField(exactField(fieldName), String.valueOf(value), Field.Store.NO));
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    String s = String.valueOf(item).trim();
                    if (!s.isEmpty()) {
                        values.add(s);
                    }
                }
            }
            if (!values.isEmpty()) {
                doc.add(new Field(fieldName, String.join(" ", values), HIGHLIGHT_TEXT_FIELD_TYPE));
                for (String item : values) {
                    doc.add(new StringField(exactField(fieldName), item, Field.Store.NO));
                }
            }
            return;
        }

        if (value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                if (item != null) {
                    String s = String.valueOf(item).trim();
                    if (!s.isEmpty()) {
                        values.add(s);
                    }
                }
            }
            if (!values.isEmpty()) {
                doc.add(new Field(fieldName, String.join(" ", values), HIGHLIGHT_TEXT_FIELD_TYPE));
                for (String item : values) {
                    doc.add(new StringField(exactField(fieldName), item, Field.Store.NO));
                }
            }
        }
    }

    private float[] extractVector(String fieldName, Object value) {
        if (value instanceof float[] || value instanceof double[]) {
            return SearchTextUtil.toFloatVector(value);
        }
        if (looksLikeVectorField(fieldName) && (SearchTextUtil.isNumericCollection(value) || SearchTextUtil.isNumericArray(value))) {
            return SearchTextUtil.toFloatVector(value);
        }
        return null;
    }

    private boolean looksLikeVectorField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("vector") || lower.contains("embedding");
    }

    private Map<String, Object> filterReturnFields(Map<String, Object> source, List<String> returnFields) {
        if (returnFields == null || returnFields.isEmpty()) {
            return source;
        }
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String field : new LinkedHashSet<>(returnFields)) {
            if (source.containsKey(field)) {
                filtered.put(field, source.get(field));
            }
        }
        return filtered;
    }

    private Map<Integer, Map<String, List<String>>> buildHighlights(
            IndexSearcher searcher,
            Query highlightQuery,
            TopDocs topDocs,
            SearchRequest request
    ) throws IOException {
        SearchHighlight highlight = request.getHighlight();
        if (highlight == null || highlightQuery == null) {
            return Map.of();
        }
        if (highlight.getFields() == null || highlight.getFields().isEmpty() || topDocs.scoreDocs.length == 0) {
            return Map.of();
        }

        Query rewrittenQuery = searcher.rewrite(highlightQuery);

        UnifiedHighlighter highlighter = new UnifiedHighlighter(UnifiedHighlighter.builder(searcher, analyzer)) {
            @Override
            protected PassageFormatter getFormatter(String field) {
                return new DefaultPassageFormatter(
                        highlight.getPreTag(),
                        highlight.getPostTag(),
                        " ... ",
                        false
                );
            }

            @Override
            protected int getMaxNoHighlightPassages(String field) {
                return 1;
            }
        };

        String[] fields = highlight.getFields().stream()
                .filter(field -> field != null && !field.isBlank())
                .toArray(String[]::new);
        if (fields.length == 0) {
            return Map.of();
        }

        int[] maxPassages = new int[fields.length];
        java.util.Arrays.fill(maxPassages, 1);

        Map<String, String[]> highlighted = highlighter.highlightFields(
                fields,
                rewrittenQuery,
                topDocs,
                maxPassages
        );

        Map<Integer, Map<String, List<String>>> result = new LinkedHashMap<>();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            int docId = topDocs.scoreDocs[i].doc;
            Map<String, List<String>> fieldMap = new LinkedHashMap<>();

            for (String field : fields) {
                String[] snippets = highlighted.get(field);
                if (snippets == null || i >= snippets.length) {
                    continue;
                }
                String snippet = snippets[i];
                if (snippet == null || snippet.isBlank()) {
                    continue;
                }
                fieldMap.put(field, List.of(trimSnippet(snippet, highlight.getFragmentSize(), highlight.getPreTag())));
            }

            if (!fieldMap.isEmpty()) {
                result.put(docId, fieldMap);
            }
        }
        return result;
    }

    private String trimSnippet(String snippet, int fragmentSize, String preTag) {
        if (snippet == null || snippet.isBlank() || fragmentSize <= 0 || snippet.length() <= fragmentSize) {
            return snippet;
        }
        int firstTag = snippet.indexOf(preTag == null ? "<em>" : preTag);
        if (firstTag < 0) {
            return snippet.substring(0, fragmentSize);
        }
        int half = Math.max(fragmentSize / 2, 20);
        int start = Math.max(0, firstTag - half);
        int end = Math.min(snippet.length(), start + fragmentSize);
        if (end - start < fragmentSize) {
            start = Math.max(0, end - fragmentSize);
        }
        String trimmed = snippet.substring(start, end);
        if (start > 0) {
            trimmed = "..." + trimmed;
        }
        if (end < snippet.length()) {
            trimmed = trimmed + "...";
        }
        return trimmed;
    }

    private static String uid(String index, String id) {
        return index + "::" + id;
    }

    private static Term uidTerm(String index, String id) {
        return new Term(FIELD_UID, uid(index, id));
    }

    private static String exactField(String field) {
        return field.endsWith(".keyword") ? field : field + ".keyword";
    }

    private static String sortField(String field) {
        return field + ".sort";
    }

    private synchronized void refreshReader() throws IOException {
        DirectoryReader newReader = DirectoryReader.openIfChanged(reader, writer);
        if (newReader != null) {
            DirectoryReader oldReader = reader;
            reader = newReader;
            oldReader.close();
        }
    }

    private CompletionStage<Void> supplyVoid(IORunnable runnable) {
        try {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(wrap(e));
        }
    }

    private <T> CompletionStage<T> supply(IOSupplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(wrap(e));
        }
    }

    private RuntimeException wrap(Exception e) {
        return e instanceof RuntimeException ? (RuntimeException) e : new SearchException(e);
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
        writer.close();
        directory.close();
    }

    private record SearchExecution(TopDocs topDocs, boolean reversePageResults) {
    }

    private record SearchPlan(Query searchQuery, Query highlightQuery) {
    }

    @FunctionalInterface
    private interface IORunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws Exception;
    }
}
