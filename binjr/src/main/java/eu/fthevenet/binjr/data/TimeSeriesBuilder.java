package eu.fthevenet.binjr.data;

import eu.fthevenet.binjr.commons.logging.Profiler;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    static public Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in) throws IOException {
        return fromCSV(in, ",", "utf-8");
    }

    static public Map<String, XYChart.Series<Date, Number>> fromCSV(InputStream in, String separator, String encoding) throws IOException {
        try (Profiler p = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {

                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;
                Map<String, XYChart.Series<Date, Number>> series = new HashMap<>();
                List<List<XYChart.Data<Date, Number>>> dataBuckets = new ArrayList<>();
                for (int i = 0; i < nbSeries; i++) {
                    dataBuckets.add(new ArrayList<>());
                }
                final AtomicLong nbpoints = new AtomicLong(0);
                long timeOrigin =-1;
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

                    Date timeStamp=  Date.from(ZonedDateTime.parse(data[0], formatter).toInstant());// = ZonedDateTime.parse(data[0], formatter).toEpochSecond();
//                    if (timeOrigin <0){
//                        timeOrigin = timeStamp;
//                    }
//                    timeStamp -= timeOrigin;

                    for (int i = 1; i < data.length - 1; i++) {
                        Double val =  Double.parseDouble(data[i]);
                        if (!val.isNaN()) {
                            XYChart.Data<Date, Number> d = new XYChart.Data<>(timeStamp, val);
                            //  d.setNode(new HoveredThresholdNode(0, (int)timeStamp));
                            dataBuckets.get(i - 1).add(d);
                        }
                    }
                }
                for (int i = 0; i < nbSeries; i++) {
                    XYChart.Series<Date, Number> s = new XYChart.Series<>();
                    s.setName(seriesNames[i + 1]);
                    s.getData().addAll(dataBuckets.get(i));
                    series.put(seriesNames[i + 1], s);
                }

                logger.debug(() -> String.format("Built %d serie(s) with %d point(s)", nbSeries, nbpoints.get()));
                return series;
            }
        }
    }
}

