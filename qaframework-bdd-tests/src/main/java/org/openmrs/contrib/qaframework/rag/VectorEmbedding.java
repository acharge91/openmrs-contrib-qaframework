package org.openmrs.contrib.qaframework.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


public class VectorEmbedding {

    private final List<TextSegment> textChunks;
    private final String gherkinQuery;
    @Getter
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    @Getter
    private Embedding gherkinQueryEmbedding;
    private Embedding buildQueryEmbedding;

    public VectorEmbedding(List<TextSegment> textChunks, String gherkinQuery) {
        this.textChunks = textChunks;
        this.gherkinQuery = gherkinQuery;
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }


    public void embedVectors() {
        InvokeModelMock mockInvoker = new InvokeModelMock();
        for (TextSegment chunk : textChunks) {
            float[] embeddingArray = mockInvoker.invokeModel(chunk.text()).getEmbedding();
            Embedding embedding = new Embedding(embeddingArray);
            embeddingStore.add(embedding, chunk);
        }
        gherkinQueryEmbedding = new Embedding(mockInvoker.invokeModel(gherkinQuery).getEmbedding());
        buildQueryEmbedding = new Embedding(mockInvoker.invokeModel(getBuildQuery()).getEmbedding());
        mockInvoker.closeClient();
    }

    public String findGherkinQueryContext() {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest
                .builder()
                .queryEmbedding(gherkinQueryEmbedding)
                .maxResults(15)
                .minScore(0.3)
                .build();
        List<EmbeddingMatch<TextSegment>> searchResultList = embeddingStore.search(searchRequest).matches();
        ArrayList<String> contextArray = new ArrayList<>();
        searchResultList.stream()
                .filter(e -> !e.embedded().text().trim().startsWith("@Before"))
                .filter(e -> !e.embedded().text().trim().startsWith("@After"))
                .forEach(e -> contextArray.add(e.embedded().text()));
        return String.join("\n", contextArray);
    }

    private List<EmbeddingMatch<TextSegment>> findContext(Embedding contextQuery) { // Unused for now to filter searches for less powerful models
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest
                .builder()
                .queryEmbedding(contextQuery)
                .maxResults(15)
                .minScore(0.3)
                .build();
        return embeddingStore.search(searchRequest).matches();
    }

    public String findBuildContext() {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest
                .builder()
                .queryEmbedding(buildQueryEmbedding)
                .maxResults(15)
                .minScore(0.3)
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("build_config"))
                .build();
        List<EmbeddingMatch<TextSegment>> searchResultList = embeddingStore.search(searchRequest).matches();
        ArrayList<String> contextArray = new ArrayList<>();
        searchResultList.forEach(e -> contextArray.add(e.embedded().text()));
        return String.join("\n", contextArray);
    }

    private String getBuildQuery() {
        return "artifactId groupId version dependency maven pom xml compile scope selenium cucumber junit";
    }
}