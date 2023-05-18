package me.iluvpwny.linebot.utility;

import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2IntentMessage;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookResponse;

import java.util.HashMap;
import java.util.List;

public class GoogleApiUtility {

    public static GoogleCloudDialogflowV2WebhookResponse createImageResponse(String url){
        GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
        GoogleCloudDialogflowV2IntentMessage msg = new GoogleCloudDialogflowV2IntentMessage();
        HashMap<String, Object> payload = new HashMap<>();
        HashMap<String, Object> contents = new HashMap<>();

        payload.put("line", contents);
        contents.put("type", "image");
        contents.put("originalContentUrl", url);
        contents.put("previewImageUrl", url);

        msg.setPayload(payload);
        response.setFulfillmentMessages(List.of(msg));

        return response;
    }

    public static GoogleCloudDialogflowV2WebhookResponse wrapPayload(HashMap<String, Object> payload){
        GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
        GoogleCloudDialogflowV2IntentMessage msg = new GoogleCloudDialogflowV2IntentMessage();

        msg.setPayload(payload);
        response.setFulfillmentMessages(List.of(msg));

        return response;
    }

}
