package me.iluvpwny.linebot.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BubbleFactory {

    private final HashMap<String, Object> payload;
    private final ArrayList<HashMap<String, String>> msgs = new ArrayList<>();


    public BubbleFactory(String altText) {
        payload = new HashMap<>();
        payload.put("type", "flex");
        payload.put("altText", altText);
        payload.put("contents", new HashMap<String, Object>());

        HashMap<String, Object> contents = (HashMap<String, Object>) payload.get("contents");
        contents.put("type", "bubble");
        contents.put("body", new HashMap<String, Object>());

        HashMap<String, Object> body = (HashMap<String, Object>) contents.get("body");
        body.put("type", "box");
        body.put("layout", "vertical");

    }

    public BubbleFactory add(String msg){
        HashMap<String, String> hmsg = new HashMap<>();
        hmsg.put("type", "text");
        hmsg.put("text", msg);
        msgs.add(hmsg);
        return this;
    }

    public HashMap<String, Object> build(){
        ((HashMap<String, Object>)((HashMap<String, Object>)payload.get("contents")).get("body")).put("contents", msgs);
        HashMap<String, Object> re = new HashMap<>();
        re.put("line", payload);
        return re;
    }
}
