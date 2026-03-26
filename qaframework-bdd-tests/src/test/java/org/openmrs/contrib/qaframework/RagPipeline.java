package org.openmrs.contrib.qaframework;

import dev.langchain4j.data.segment.TextSegment;
import org.openmrs.contrib.qaframework.rag.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class RagPipeline {
    public static void main(String[] args) {
        List<TextSegment> textChunks = null;
        File queryFile = new File("qaframework-bdd-tests/src/test/resources/features/rag/addLocationWithAttributeType.feature");
        String queryString = "";
        RepoChunker chunker = new RepoChunker("/Users/acharge/Deloitte/openmrs-contrib-qaframework");
        try {
            textChunks = chunker.chunkRepo();
            queryString = Files.readString(queryFile.toPath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        VectorEmbedding vectorEmbedding = new VectorEmbedding(textChunks, queryString);
        vectorEmbedding.embedVectors(); //TODO - connect to actual Bedrock
        String queryContextString = vectorEmbedding.findGherkinQueryContext();
//        String queryContextString = "";
        String buildContextString = vectorEmbedding.findBuildContext();
        String prompt = PromptBuilder.buildPrompt(queryContextString, buildContextString, queryString);
//        InvokeClaude invokeClaude = new InvokeClaude(prompt);
//        String claudeResponse = invokeClaude.invokeModel().getCompletion();
//        invokeClaude.closeClient();
        //TODO - something with the response once access to Bedrock established
        InvokeClaudeMock invokeClaudeMock = new InvokeClaudeMock(prompt);
        String claudeMessage = invokeClaudeMock.invokeModel();

        try {
            writeDiagnosticReport(textChunks, prompt, claudeMessage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write diagnostic report", e);
        }
    }

    public static void writeDiagnosticReport(List<TextSegment> chunks, String prompt, String claudeResponse) throws IOException {
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

        // --- SECTION 2: PROMPT STRING ---
        report.append("=".repeat(80)).append("\n");
        report.append("PROMPT full prompt gathered and formated from vector search:\n");
        report.append("=".repeat(80)).append("\n");
        report.append(prompt).append("\n\n");

        // --- SECTION 3: CLAUDE RESPONSE ---
        report.append("=".repeat(80)).append("\n");
        report.append("CLAUDE RESPONSE:\n");
        report.append("=".repeat(80)).append("\n");
        report.append(claudeResponse).append("\n\n");

        Files.writeString(Paths.get("diagnostic-report.txt"), report.toString());
        System.out.println("Diagnostic report written to diagnostic-report.txt");
    }
}
