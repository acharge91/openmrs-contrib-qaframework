package org.openmrs.contrib.qaframework.rag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.openmrs.contrib.qaframework.helper.responseModels.ClaudeResponse;
import org.openmrs.contrib.qaframework.helper.responseModels.TitanResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.net.http.HttpResponse;

public class InvokeClaude {

    private final String promptString;
    private final Gson gson;
    private final BedrockRuntimeClient client;

    public InvokeClaude(String prompt) {
        this.promptString = prompt;
        this.gson = new GsonBuilder().serializeNulls().create();
        client = getBedrockRuntimeClient();
    }

    public ClaudeResponse invokeModel() {

        // Create a Bedrock Runtime client in the AWS Region you want to use.
        // Replace the DefaultCredentialsProvider with your preferred credentials provider.


        // Set the model ID, e.g., Claude 3 Haiku.
        String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";

        // The InvokeModel API uses the model's native payload.
        String nativeRequest = getRequest();

        try {
            // Encode and send the request to the Bedrock Runtime.
            InvokeModelResponse response = client.invokeModel(request -> request
                .body(SdkBytes.fromUtf8String(nativeRequest))
                .modelId(modelId)
            );

            return gson.fromJson(response.body().asUtf8String(), ClaudeResponse.class);

        } catch (SdkClientException e) {
            System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
            closeClient();
            throw new RuntimeException(e);
        } catch (BedrockRuntimeException e) {
            System.out.printf("Bedrock runtime ERROR. Status code: %s. Reason: %s", e.statusCode(), e.getMessage());
            closeClient();
            throw new RuntimeException(e);
        }
    }

    private String getRequest() {
        JSONObject claudeObject = new JSONObject();
        claudeObject.put("anthropic_version", "bedrock-2023-05-31");
        claudeObject.put("max_tokens", 512);
        claudeObject.put("temperature", 0.5);
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", promptString);
        JSONArray messageArray = new JSONArray(messageObject);
        claudeObject.put("messages", messageArray);
        return claudeObject.toString();
    }

    private BedrockRuntimeClient getBedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
    }

    public void closeClient() {
        client.close();
    }
}
