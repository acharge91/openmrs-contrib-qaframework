package org.openmrs.contrib.qaframework.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


public class VectorEmbedding {

    private final List<TextSegment> textChunks;
    private final String query;
    @Getter
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    @Getter
    private Embedding queryEmbedding;
    @Getter
    private List<EmbeddingMatch<TextSegment>> searchResultList;

    public VectorEmbedding(List<TextSegment> textChunks, String query) {
        this.textChunks = textChunks;
        this.query = query;
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    public void embedVectors() {
        InvokeModel bedrockInvoker = new InvokeModel();
        for (TextSegment chunk : textChunks) {
            float[] embeddingArray = bedrockInvoker.invokeModel(chunk.text()).getEmbedding();
            Embedding embedding = new Embedding(embeddingArray);
            embeddingStore.add(embedding, chunk);
        }
        queryEmbedding = new Embedding(bedrockInvoker.invokeModel(query).getEmbedding());
        bedrockInvoker.closeClient();
    }

    public String findRelevantContext() {
        ArrayList<String> contextArray = new ArrayList<>();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest
                .builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.7)
                .build();
        searchResultList = embeddingStore.search(searchRequest).matches();
        searchResultList.forEach(e -> {
            contextArray.add(e.embedded().text());
        });
        return String.join("\n", contextArray);
    }
}