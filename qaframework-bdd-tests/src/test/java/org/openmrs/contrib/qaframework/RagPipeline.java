package org.openmrs.contrib.qaframework;

import dev.langchain4j.data.segment.TextSegment;
import org.openmrs.contrib.qaframework.rag.RepoChunker;

import java.io.IOException;
import java.util.List;

public class RagPipeline {
    public static void main(String[] args) {
        try {
            runChunker();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void runChunker() throws IOException {
        RepoChunker chunker = new RepoChunker("/Users/acharge/Deloitte/openmrs-contrib-qaframework");
        List<TextSegment> chunks = chunker.chunkRepo();
        java.nio.file.Files.writeString(
                java.nio.file.Paths.get("chunks-report.txt"),
                chunks.stream().map(c ->
                        "TYPE: " + c.metadata().getString("type") + "\n" +
                                "FILE: " + c.metadata().getString("file") + "\n" +
                                "PREVIEW: " + c.text().substring(0, Math.min(200, c.text().length())) + "\n---\n"
                ).collect(java.util.stream.Collectors.joining())
        );
    }
}
