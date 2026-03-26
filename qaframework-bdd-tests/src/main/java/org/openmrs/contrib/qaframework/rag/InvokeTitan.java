package org.openmrs.contrib.qaframework.rag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openmrs.contrib.qaframework.helper.responseModels.TitanResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import static org.junit.Assert.fail;

public class InvokeTitan {

    private final Gson gson;

    private BedrockRuntimeClient client = null;

    public InvokeTitan() {
        this.gson = new GsonBuilder().serializeNulls().create();
        client = getBedrockClient();
    }

    public TitanResponse invokeModel(String requestChunk) {
        // Set the model ID, e.g., Titan Text Embeddings V2.
        String modelId = "amazon.titan-embed-text-v1";

        // The InvokeTitan API uses the model's native payload.
        String nativeRequestTemplate = "{ \"inputText\": \"{{inputText}}\", \"normalize\": true }";

        // Embed the prompt in the model's native request payload.
        String nativeRequest = nativeRequestTemplate.replace("{{inputText}}", requestChunk);

        try {
            // Encode and send the request to the Bedrock Runtime.
            InvokeModelResponse response = client.invokeModel(request -> request
                    .body(SdkBytes.fromUtf8String(nativeRequest))
                    .modelId(modelId)
            );

            return gson.fromJson(response.body().asUtf8String(), TitanResponse.class);

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

    private BedrockRuntimeClient getBedrockClient() {
        // Create a Bedrock Runtime client in the AWS Region you want to use.
        // Replace the DefaultCredentialsProvider with your preferred credentials provider.
        return BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.EU_WEST_1) //TODO add correct region
                .build();
    }

    public void closeClient() {
        client.close();
    }
}

