package eu.fthevenet.binjr.data.parsers;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Created by FTT2 on 26/01/2017.
 */
public class CsvParser<T extends Number> implements DataParser<T> {
    private final String encoding;
    private final String separator;
    private final Function<String, T> numberParser;
    private final Function<String, ZonedDateTime> dateParser;
    private static final Logger logger = LogManager.getLogger(CsvParser.class);


    public CsvParser(String encoding, String separator, Function<String, T> numberParser, Function<String, ZonedDateTime> dateParser){

        this.encoding = encoding;
        this.separator = separator;
        this.numberParser = numberParser;
        this.dateParser = dateParser;
    }

    @Override
    public Map<String, TimeSeries<T>> parse(InputStream in, String... names) throws IOException, ParseException {
        try (Profiler profiler = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;
            //    Map<String, TimeSeries<T>> series = new HashMap<>();
                Map<String, List<XYChart.Data<ZonedDateTime, T>>> series = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    ZonedDateTime timeStamp = dateParser.apply(data[0]);
                    for (int i = 1; i < data.length - 1; i++) {
                        String currentName = seriesNames[i];
                        if (isInNameList(names, currentName)) {
                            T val = numberParser.apply(data[i]);

                            XYChart.Data<ZonedDateTime, T> point = new XYChart.Data<>(timeStamp, val);
                            List<XYChart.Data<ZonedDateTime, T>> l = series.computeIfAbsent(currentName, k -> new ArrayList<>());
                            l.add(point);
                        }
                    }
                }
                logger.debug(() -> String.format("Built %d serie(s) with %d point(s)", nbSeries, nbpoints.get()));
            //    return series;
                return null;
            }
        }
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
