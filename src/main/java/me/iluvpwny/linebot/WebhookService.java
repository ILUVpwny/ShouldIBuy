package me.iluvpwny.linebot;

import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookResponse;
import me.iluvpwny.linebot.utility.BubbleFactory;
import me.iluvpwny.linebot.utility.GoogleApiUtility;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.LongStream;

@Service
public class WebhookService {

    private final HashMap<String, Long> next_update = new HashMap<>();
    private final HashMap<String, Double> last_min = new HashMap<>();
    private final HashMap<String, Double> last_max = new HashMap<>();
    private final HashMap<String, TimeSeries> next_series = new HashMap<>();

    private final List<String> mindfullSpeech;

    public WebhookService() {
        ArrayList<String> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("mindfullness.txt"))) {
            String line = br.readLine();
            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mindfullSpeech = list;
    }

    public GoogleCloudDialogflowV2WebhookResponse test(JSONObject param){
        return null;
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

        TimeSeries series = new TimeSeries("line");
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        if (next_update.getOrDefault(from+"-"+to, 0L) < Instant.now().getEpochSecond()) {
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

            ArrayList<JSONObject> reses = new ArrayList<>();
            totalDates.parallelStream().forEach(localDate -> {
                try {
                    reses.add(new JSONObject(IOUtils.toString(new URL("https://v6.exchangerate-api.com/v6/7dc810bca653d8fd84681c53/history/" + from + "/" + localDate.format(formatter)), StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            reses.sort((o1, o2) -> {
                try {
                    LocalDate d1 = LocalDate.of(o1.getInt("year"), o1.getInt("month")-1, o1.getInt("day"));
                    LocalDate d2 = LocalDate.of(o2.getInt("year"), o2.getInt("month")-1, o2.getInt("day"));
                    return -d1.compareTo(d2);
                }catch (Exception e){
                    e.printStackTrace();
                    return -1;
                }
            });
            for (int i = 29; i >= 0; i--) {
                JSONObject res = reses.get(i);
                LocalDate date = totalDates.get(i);
                JSONObject rates = (JSONObject) res.get("conversion_rates");
                min = Math.min(min, rates.getDouble(to));
                max = Math.max(max, rates.getDouble(to));
                series.add(new TimeSeriesDataItem(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), BigDecimal.valueOf(rates.getDouble(to))));
            }
            next_update.put(from+"-"+to, latest.getLong("time_next_update_unix"));
            last_min.put(from+"-"+to, min);
            last_max.put(from+"-"+to, max);
            next_series.put(from+"-"+to, series);
        }else {
            min = last_min.get(from+"-"+to);
            max = last_max.get(from+"-"+to);
            series = next_series.get(from+"-"+to);
        }

        var dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart lineChart = ChartFactory.createTimeSeriesChart(from+"-"+to, "วัน", to, dataset, false, false, false);

        XYPlot plot = (XYPlot) lineChart.getPlot();
        double diff = max - min;
        plot.getRangeAxis().setRange(new Range(min-diff*0.1, max+diff*0.25));
        ((NumberAxis)plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        ((NumberAxis)plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        DateAxis dateAxis = new DateAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("d MMM"));
        plot.setDomainAxis(dateAxis);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER, TextAnchor.HALF_ASCENT_CENTER, -Math.PI / 2));
        renderer.setItemLabelInsets(new RectangleInsets(20.0, 20.0, 20.0, 20.0));
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator("{2}", decimalFormat, decimalFormat));

        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(bas, lineChart, 800, 400);
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

    public GoogleCloudDialogflowV2WebhookResponse mindfulness(JSONObject param) {
        return GoogleApiUtility.createImageResponse(mindfullSpeech.get(new Random().nextInt(mindfullSpeech.size())));
    }
}
