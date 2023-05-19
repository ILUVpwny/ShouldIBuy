package me.iluvpwny.linebot;

import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping(path = "/")
public class Webhook {

    @Autowired
    private WebhookService webhookService;

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public GoogleCloudDialogflowV2WebhookResponse webhook(@RequestBody String inputJson) throws IOException {
        JSONObject requestBody = new JSONObject(inputJson);

        JSONObject queryResult = requestBody.getJSONObject("queryResult");
        JSONObject intent = queryResult.getJSONObject("intent");

        String intent_name = intent.getString("displayName");
        JSONObject param = queryResult.getJSONObject("parameters");

        return switch (intent_name) {
            case "คุ้ม - คำนวณ" -> webhookService.rot(param);
            case "กราฟเงิน" -> webhookService.moneyGraph(param);
            case "ค่าเงิน - คำนวณ" -> webhookService.moneyExchange(param);
            case "คติเตือนใจ" -> webhookService.mindfulness(param);
            default -> null;
        };
    }

//    @GetMapping(
//            value = "/image",
//            produces = MediaType.IMAGE_PNG_VALUE
//    )
//    public @ResponseBody byte[] getImage(@RequestParam(required = false) String ID, @RequestParam String from, @RequestParam String to) throws IOException {
//        byte[] bytes = webhookService.getExchangeImage(from, to);
//        System.out.println("TESTTEST");
//        if (bytes == null){
//            InputStream in = getClass()
//                    .getResourceAsStream("/image.png");
//            assert in != null;
//            return IOUtils.toByteArray(in);
//        }
//        return bytes;
//    }
}
