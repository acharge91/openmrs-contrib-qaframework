package org.openmrs.contrib.qaframework;

import dev.langchain4j.data.segment.TextSegment;
import org.openmrs.contrib.qaframework.rag.RepoChunker;
import org.openmrs.contrib.qaframework.rag.VectorEmbedding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class RagPipeline {
    public static void main(String[] args) {
        List<TextSegment> textChunks = null;
        File queryFile = new File("qaframework-bdd-tests/src/test/resources/features/refapp-2.x/05-location/addLocationWithAttributeType.feature");
        String queryString = "";
        RepoChunker chunker = new RepoChunker("/Users/acharge/Deloitte/openmrs-contrib-qaframework");
        try {
            textChunks = chunker.chunkRepo();
            queryString = Files.readString(queryFile.toPath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        VectorEmbedding vectorEmbedding = new VectorEmbedding(textChunks, queryString);
//        vectorEmbedding.embedVectors();
//        String contextString = vectorEmbedding.findRelevantContext();
        String contextString = "";
        try {
            writeDiagnosticReport(textChunks, queryString, contextString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write diagnostic report", e);
        }
    }

    public static void writeDiagnosticReport(List<TextSegment> chunks, String queryString, String contextString) throws IOException {
        StringBuilder report = new StringBuilder();

        // --- SECTION 1: CHUNKS ---
        report.append("=".repeat(80)).append("\n");
        report.append("CHUNKS (all chunks extracted from repo):\n");
        report.append("=".repeat(80)).append("\n");
        report.append(chunks.stream().map(c ->
                "TYPE: " + c.metadata().getString("type") + "\n" +
                "FILE: " + c.metadata().getString("file") + "\n" +
                "PREVIEW: " + c.text().substring(0, Math.min(200, c.text().length())) + "\n---\n"
        ).collect(Collectors.joining())).append("\n");

        // --- SECTION 2: QUERY STRING ---
        report.append("=".repeat(80)).append("\n");
        report.append("QUERY (feature file contents used to search the vector store):\n");
        report.append("=".repeat(80)).append("\n");
        report.append(queryString).append("\n\n");

        // --- SECTION 3: RETRIEVED CONTEXT ---
        report.append("=".repeat(80)).append("\n");
        report.append("RETRIEVED CONTEXT (top chunks returned by vector store similarity search):\n");
        report.append("=".repeat(80)).append("\n");
        report.append(contextString).append("\n\n");

        Files.writeString(Paths.get("diagnostic-report.txt"), report.toString());
        System.out.println("Diagnostic report written to diagnostic-report.txt");
    }
}
