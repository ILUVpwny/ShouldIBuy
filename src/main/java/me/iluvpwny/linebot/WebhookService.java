package me.iluvpwny.linebot;

import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookResponse;
import me.iluvpwny.linebot.utility.BubbleFactory;
import me.iluvpwny.linebot.utility.GoogleApiUtility;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

@Service
public class WebhookService {

    public GoogleCloudDialogflowV2WebhookResponse test(JSONObject param){
        try{
            return GoogleApiUtility.createImageResponse(LinebotApplication.PUBLICDOMAIN + "/image?ID=" + UUID.randomUUID());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public GoogleCloudDialogflowV2WebhookResponse rot(JSONObject param){

        double vol1 = param.getDouble("vol1");
        double vol2 = param.getDouble("vol2");
        double price1 = param.getDouble("price1");
        double price2 = param.getDouble("price2");
        double rot1 = Math.round((vol1 /price1)*100)/100.0;
        double rot2 = Math.round((vol2/price2)*100)/100.0;

        BubbleFactory factory = new BubbleFactory("อันไหนคุ้มกว่านะ?");
        factory.add("สินค้า 1")
            .add("- ราคา: " + price1)
            .add("- ปริมาณ: " + vol1)
            .add("- ความคุ้ม: " + rot1)
            .add(" ")
            .add("สินค้า 2")
            .add("- ราคา: " + price2)
            .add("- ปริมาณ: " + vol2)
            .add("- ความคุ้ม: " + rot2)
            .add(" ");

        if (rot1 > rot2) {
            factory.add("สินค้า 1 คุ้มกว่านะ");
        }else if (rot1 < rot2) {
            factory.add("สินค้า 2 คุ้มกว่านะ");
        }

        return GoogleApiUtility.wrapPayload(factory.build());
    }

    public GoogleCloudDialogflowV2WebhookResponse moneyGraph(JSONObject param){
        String from = param.getString("from");
        String to = param.getString("to");
        try {
            return GoogleApiUtility.createImageResponse(LinebotApplication.PUBLICDOMAIN + "/image?ID=" + UUID.randomUUID() + "&from=" + from + "&to=" + to);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getExchangeImage(String from, String to) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        JSONObject latest;
        try {
            latest = new JSONObject(IOUtils.toString(new URL("https://v6.exchangerate-api.com/v6/7dc810bca653d8fd84681c53/latest/" + from), StandardCharsets.UTF_8));
        }catch (FileNotFoundException e){
            return null;
        }

        if (latest.getString("result").equals("error") || !latest.getJSONObject("conversion_rates").has(to)){
            return null;
        }

        LocalDate finish =
                Instant.ofEpochMilli(((Integer) latest.get("time_last_update_unix")).longValue()*1000)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate();

        List<LocalDate> totalDates =
                LongStream.iterate(0, i -> i - 1)
                        .limit(30).mapToObj(finish::plusDays).toList();

        var series = new XYSeries("line");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = totalDates.get(i);
            JSONObject res;
            try {
                res = new JSONObject(IOUtils.toString(new URL("https://v6.exchangerate-api.com/v6/7dc810bca653d8fd84681c53/history/" + from + "/" + date.format(formatter)), StandardCharsets.UTF_8));
            }catch (FileNotFoundException e){
                return null;
            }
            if (res.getString("result").equals("error") || !res.getJSONObject("conversion_rates").has(to)){
                return null;
            }
            JSONObject rates = (JSONObject) res.get("conversion_rates");
            series.add(-i, (BigDecimal) rates.get(to));
        }

        var dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart lineChart = ChartFactory.createXYLineChart(from+"-"+to, "วัน", to, dataset, PlotOrientation.VERTICAL, false, false, false);

        XYPlot plot = (XYPlot) lineChart.getPlot();
        plot.getRangeAxis().setAutoRange(true);
        ((NumberAxis)plot.getRangeAxis()).setAutoRangeIncludesZero(false);


        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(bas, lineChart, 600, 400);
        return bas.toByteArray();
    }

    public GoogleCloudDialogflowV2WebhookResponse moneyExchange(JSONObject param){
        String to = param.getString("to");
        double from_amount = param.getJSONObject("from").getDouble("amount");
        String from = param.getJSONObject("from").getString("currency");

        JSONObject conv;
        try {
            conv = new JSONObject(IOUtils.toString(new URL("https://v6.exchangerate-api.com/v6/7dc810bca653d8fd84681c53/pair/" + from + "/" + to + "/" + from_amount), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (conv.getString("result").equals("error")){
            return null;
        }

        double to_amount = conv.getDouble("conversion_result");
        BubbleFactory bubbleFactory = new BubbleFactory("แลกเปลี่ยนเงิน");
        bubbleFactory.add("จาก " + from + " เป็น " + to)
                .add("- จำนวน: " + from_amount + " " + from)
                .add("- ได้: " + Math.round(to_amount*100)/100.0 + " " + to);
        return GoogleApiUtility.wrapPayload(bubbleFactory.build());
    }
}
