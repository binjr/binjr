package eu.fthevenet.binjr.data.timeseries;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 24/10/2016.
 */
public class TimeSeriesBuilder {
    private static final Logger logger = LogManager.getLogger(TimeSeriesBuilder.class);
    private Map<String, List<XYChart.Data<ZonedDateTime, Number>>> timeSeries;
    private final boolean useReduction;
    private final int reductionThreshold;
    private final ZoneId zoneId;

    public TimeSeriesBuilder(ZoneId zoneId) {
        this(true, 1000, zoneId);
    }

    public TimeSeriesBuilder(boolean useReduction, int reductionThreshold, ZoneId zoneId) {
        this.reductionThreshold = reductionThreshold;
        this.useReduction = useReduction;
        this.zoneId = zoneId;
        this.timeSeries = new HashMap<>();
    }

    public TimeSeriesBuilder transform(TimeSeriesTransform seriesTransform, String... seriesNames) {
        Set<String> nameSet = seriesNames.length == 0 ? timeSeries.keySet() : new HashSet<String>(Arrays.asList(seriesNames));
        Map<String, List<XYChart.Data<ZonedDateTime, Number>>> series = timeSeries.entrySet()
                .stream()
                .filter(s -> nameSet.contains(s.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (series == null || series.size() != nameSet.size()) {
            throw new IllegalArgumentException("Failed to retrieve all timeSeries with name " + Arrays.toString(seriesNames));
        }
        try (Profiler ignored = Profiler.start("Applying transform" + seriesTransform.getName() + " to series " + Arrays.toString(nameSet.toArray()), logger::trace)) {
            Map<String, List<XYChart.Data<ZonedDateTime, Number>>> a = seriesTransform.transform(series);
            timeSeries.putAll(a);
        }
        return this;
    }


    public TimeSeriesBuilder fromCSV(InputStream in) throws IOException, ParseException {
        return fromCSV(in, ",", "utf-8", new String[0]);
    }

    public TimeSeriesBuilder fromCSV(InputStream in, String... seriesNames) throws IOException, ParseException {
        return fromCSV(in, ",", "utf-8", seriesNames);
    }

    public TimeSeriesBuilder fromCSV(InputStream in, String separator, String... seriesNames) throws IOException, ParseException {
        return fromCSV(in, separator, "utf-8", seriesNames);
    }

    public TimeSeriesBuilder fromCSV(InputStream in, String separator, String encoding, String... names) throws IOException, ParseException {
        try (Profiler profiler = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;
                Map<String, List<XYChart.Data<ZonedDateTime, Number>>> series = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);
                    ZonedDateTime timeStamp = ZonedDateTime.parse(data[0], formatter);

                    for (int i = 1; i < data.length - 1; i++) {
                        String currentName = seriesNames[i];
                        if (isInNameList(names, currentName)) {
                            Double val = Double.parseDouble(data[i]);
                            val = val.isNaN() ? 0 : val;

                            XYChart.Data<ZonedDateTime, Number> point = new XYChart.Data<>(timeStamp, val);
                            List<XYChart.Data<ZonedDateTime, Number>> l = series.computeIfAbsent(currentName, k -> new ArrayList<>());
                            l.add(point);
                        }
                    }
                }
                logger.debug(() -> String.format("Built %d serie(s) with %d point(s)", nbSeries, nbpoints.get()));
                timeSeries.putAll(series);
                return this;
            }
        }
    }

    public Map<String, XYChart.Series<ZonedDateTime, Number>> build() {
        return timeSeries.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    XYChart.Series<ZonedDateTime, Number> s = new XYChart.Series<>();
                    s.setName(e.getKey());
                    s.getData().addAll(e.getValue());
                    return s;
                }));
    }

    private boolean isInNameList(String[] names, String seriesName) {
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



