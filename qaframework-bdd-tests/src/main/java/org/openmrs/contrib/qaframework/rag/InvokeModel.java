package org.openmrs.contrib.qaframework.rag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openmrs.contrib.qaframework.helper.responseModels.TitanResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class InvokeModel {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static TitanResponse invokeModel(String requestChunk) {

        // Create a Bedrock Runtime client in the AWS Region you want to use.
        // Replace the DefaultCredentialsProvider with your preferred credentials provider.
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1) //TODO add correct region
                .build();

        // Set the model ID, e.g., Titan Text Embeddings V2.
        String modelId = "amazon.titan-embed-text-v1";

        // The InvokeModel API uses the model's native payload.
        // Learn more about the available inference parameters and response fields at:
        // https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-titan-embed-text.html
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
            throw new RuntimeException(e);
        }
    }
}

