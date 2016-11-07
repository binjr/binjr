package eu.fthevenet.binjr.data;

import eu.fthevenet.binjr.commons.logging.Profiler;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

/**
 * Created by FTT2 on 24/10/2016.
 */
public class TimeSeriesBuilder {
    private static final Logger logger = LogManager.getLogger(TimeSeriesBuilder.class);

    static public Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in, XYChart<Date, Number> chart, String... seriesNames) throws IOException {
        return fromCSV(in, ",", "utf-8", true, chart,seriesNames);
    }

    static public Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in, String separator, String encoding, boolean RDPReduce, XYChart<Date, Number> chart, String... names) throws IOException {
        try (Profiler profiler = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {

                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;
                Map<String, XYChart.Series<Date, Number>> series = new HashMap<>();
                List<List<RamerDouglasPeucker.Point>> rawPointSeries = new ArrayList<>();
                for (int i = 0; i < nbSeries; i++) {
                    rawPointSeries.add(new ArrayList<>());
                }
                final AtomicLong nbpoints = new AtomicLong(0);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
                        double timeStamp = ZonedDateTime.parse(data[0], formatter).toEpochSecond();

                        for (int i = 1; i < data.length - 1; i++) {
                            Double val = Double.parseDouble(data[i]);
                            RamerDouglasPeucker.Point point = new RamerDouglasPeucker.Point(timeStamp,  val.isNaN() ? 0 : val);
                            rawPointSeries.get(i-1).add(point);
                        }
                }
                for (int i = 0; i < nbSeries; i++) {
                    if (isInNameList(names, seriesNames[i+1])) {
                        XYChart.Series<Date, Number> s = new XYChart.Series<>();
                        s.setName(seriesNames[i + 1]);
                        List<RamerDouglasPeucker.Point> reduced = rawPointSeries.get(i);
                        if (RDPReduce) {
                            double xScale = getScalingFactor(reduced, chart.getPrefWidth(), p -> p.x);
                            double yScale = getScalingFactor(reduced, chart.getPrefHeight(), p -> p.y);
                            try (Profiler p2 = Profiler.start("Reducing " + seriesNames[i + 1], logger::trace)) {
                                reduced = RamerDouglasPeucker.reduce(rawPointSeries.get(i), 0.1, xScale, yScale);
                            }
                            logger.debug(String.format("%s: %d -> %d", seriesNames[i + 1], rawPointSeries.get(i).size(), reduced.size()));
                        }
                        List<XYChart.Data<Date, Number>> list = new ArrayList<>();
                        for (RamerDouglasPeucker.Point point : reduced) {
                            Date timeStamp = Date.from(Instant.ofEpochSecond((long) point.x));
                            list.add(new XYChart.Data<>(timeStamp, point.y));
                        }
                        s.getData().addAll(list);
                        series.put(seriesNames[i + 1], s);
                    }
                }

                logger.debug(() -> String.format("Built %d serie(s) with %d point(s)", nbSeries, nbpoints.get()));
                logger.debug(() -> "RDP reduction used: " + Boolean.toString(RDPReduce));
                return series;
            }
        }
    }

    private static boolean isInNameList(String[] names, String seriesName) {
        if (names.length ==0){
            return true;
        }
        for (String s : names) {
            if (s.equalsIgnoreCase(seriesName)) {
                return true;
            }
        }
        return false;
    }

    private static double getScalingFactor(List<RamerDouglasPeucker.Point> reduced, double axisDrawSize, ToDoubleFunction<RamerDouglasPeucker.Point> getValue ){
        double  scalingFactor = 1.0;
        Optional<RamerDouglasPeucker.Point> max  = reduced.stream().max(Comparator.comparingDouble(getValue));
        Optional<RamerDouglasPeucker.Point> min  = reduced.stream().min(Comparator.comparingDouble(getValue));
        if (max.isPresent() && min.isPresent() && (getValue.applyAsDouble(max.get()) - getValue.applyAsDouble(min.get()) !=0) ){
            scalingFactor =axisDrawSize /  (getValue.applyAsDouble(max.get()) - getValue.applyAsDouble(min.get()));
        }
        return scalingFactor;
    }
}

