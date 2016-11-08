package eu.fthevenet.binjr.data;

import eu.fthevenet.binjr.commons.logging.Profiler;
import javafx.geometry.Point2D;
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

/**
 * Created by FTT2 on 24/10/2016.
 */
public class TimeSeriesBuilder {
    private static final Logger logger = LogManager.getLogger(TimeSeriesBuilder.class);

    static public Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in, XYChart<Date, Number> chart, double epsilon, boolean rdpReduce, String... seriesNames) throws IOException {
        return fromCSV(in, ",", "utf-8", rdpReduce, chart, epsilon, seriesNames);
    }

    public static Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in, String separator, String encoding, boolean RDPReduce, XYChart<Date, Number> chart, double epsilon, String... names) throws IOException {
        try (Profiler profiler = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {

                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;
                Map<String, XYChart.Series<Date, Number>> series = new HashMap<>();
                Map<String, List<Point2D>> rawPointSeries = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                double maxY = 0;
                int nbSamples = 0;
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
                    double timeStamp = ZonedDateTime.parse(data[0], formatter).toEpochSecond();

                    for (int i = 1; i < data.length - 1; i++) {
                        String currentName = seriesNames[i];
                        if (isInNameList(names, currentName)) {
                            Double val = Double.parseDouble(data[i]);
                            val = val.isNaN() ? 0 : val;
                            maxY = Math.max(maxY, val);
                            Point2D point = new Point2D(timeStamp, val);
                            List<Point2D> l = rawPointSeries.get(currentName);
                            if (l == null) {
                                l = new ArrayList<>();
                                rawPointSeries.put(currentName, l);
                            }
                            l.add(point);
                        }
                    }
                    nbSamples++;
                }
                double xScale = 1.0;
                if (maxY != 0) {
                    xScale = chart.getPrefWidth() / maxY;
                }
                double yScale = 1.0;
                if (nbSamples > 0) {
                    yScale = chart.getPrefHeight() / nbSamples;
                }
                for (Map.Entry<String, List<Point2D>> entry : rawPointSeries.entrySet()) {
                    XYChart.Series<Date, Number> s = new XYChart.Series<>();
                    s.setName(entry.getKey());
                    List<Point2D> reduced =entry.getValue();
                    if (RDPReduce) {
                        try (Profiler p2 = Profiler.start("Reducing " +entry.getKey(), logger::trace)) {
                            reduced = RamerDouglasPeucker.reduce(entry.getValue(), epsilon, xScale, yScale);
                        }
                        logger.debug(String.format("%s: %d -> %d", entry.getKey(), entry.getValue().size(), reduced.size()));
                    }
                    List<XYChart.Data<Date, Number>> list = new ArrayList<>();
                    for (Point2D point : reduced) {
                        Date timeStamp = Date.from(Instant.ofEpochSecond((long) point.getX()));
                        list.add(new XYChart.Data<>(timeStamp, point.getY()));
                    }
                    s.getData().addAll(list);
                    series.put(entry.getKey(), s);
                }

                logger.debug(() -> String.format("Built %d serie(s) with %d point(s)", nbSeries, nbpoints.get()));
                logger.debug(() -> "RDP reduction used: " + Boolean.toString(RDPReduce));
                return series;
            }
        }
    }

    private static boolean isInNameList(String[] names, String seriesName) {
        if (names.length == 0) {
            return true;
        }
        for (String s : names) {
            if (s.equalsIgnoreCase(seriesName)) {
                return true;
            }
        }
        return false;
    }
}



