package org.openmrs.contrib.qaframework.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.openmrs.contrib.qaframework.helper.responseModels.TitanResponse;

public class InvokeModelMock {

    // Local embedding model — runs entirely in the JVM, no API calls, no cost
    private final EmbeddingModel embeddingModel;

    public InvokeModelMock() {
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
    }

    public TitanResponse invokeModel(String inputText) {
        // Embed the input text using the local model
        Embedding embedding = embeddingModel.embed(TextSegment.from(inputText)).content();

        // Convert float[] from LangChain4j Embedding to a TitanResponse
        // so that InvokeModelMock is a drop-in replacement for InvokeModel
        TitanResponse response = new TitanResponse();
        response.setEmbedding(embedding.vector());
        response.setInputTextTokenCount(inputText.split("\\s+").length);
        return response;
    }

    // Matches InvokeModel.closeClient() signature so VectorEmbedding needs no structural change
    public void closeClient() {
        // No client to close — local model holds no external connections
    }
}
