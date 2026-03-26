package org.openmrs.contrib.qaframework.rag;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

import java.util.List;
import java.util.NoSuchElementException;

public class InvokeClaudeMock {


    private final AnthropicClient client;
    private final String prompt;

    public InvokeClaudeMock(String prompt) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey("")
                .build();
        this.prompt = prompt;
    }

    public String invokeModel() {
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(4096L)
                .addUserMessage(prompt)
                .model(Model.CLAUDE_SONNET_4_0)
                .build();
        try {
            List<ContentBlock> message = client.messages().create(params).content();
            if (message.isEmpty()) {
                throw new RuntimeException("Claude returned an empty response — no content blocks present");
            }
            return message.get(0).text()
                    .orElseThrow(() -> new NoSuchElementException("First content block is not a text block — cannot extract response text"))
                    .text();
        } catch (UnauthorizedException e) {
            System.err.printf("Authentication failed — check your Anthropic API key. Status: %s, Reason: %s%n", e.statusCode(), e.getMessage());
            closeClient();
            throw new RuntimeException("Anthropic authentication failure", e);
        } catch (RateLimitException e) {
            System.err.printf("Rate limit exceeded — too many requests. Status: %s, Reason: %s%n", e.statusCode(), e.getMessage());
            closeClient();
            throw new RuntimeException("Anthropic rate limit exceeded", e);
        } catch (AnthropicServiceException e) {
            System.err.printf("Anthropic API error. Status: %s, Reason: %s%n", e.statusCode(), e.getMessage());
            closeClient();
            throw new RuntimeException("Anthropic service error", e);
        } catch (NoSuchElementException e) {
            System.err.printf("Response parsing failed. Reason: %s%n", e.getMessage());
            closeClient();
            throw new RuntimeException("Failed to extract text from Claude response", e);
        } catch (Exception e) {
            System.err.printf("Unexpected error during Claude invocation. Reason: %s%n", e.getMessage());
            closeClient();
            throw new RuntimeException("Unexpected Claude invocation failure", e);
        }
    }

    public void closeClient() {
        client.close();
    }
}

