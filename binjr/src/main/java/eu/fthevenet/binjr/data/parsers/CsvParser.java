package eu.fthevenet.binjr.data.parsers;

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.binjr.logging.Profiler;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessorFactory;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * This class provides an implementation of a {@link DataParser} that generates {@link TimeSeriesProcessor} out of a CSV formatted text stream.
 *
 * @author Frederic Thevenet
 */
public class CsvParser<T extends Number> implements DataParser<T> {
    private final String encoding;
    private final String separator;
    private final Function<String, T> numberParser;
    private final Function<String, ZonedDateTime> dateParser;
    private final TimeSeriesProcessorFactory<T> timeSeriesFactory;
    private static final Logger logger = LogManager.getLogger(CsvParser.class);

    /**
     * Initializes a new instance of the {@link CsvParser} class.
     * @param encoding the encoding used in the CSV stream
     * @param separator the character to separate columns in the CSV stream
     * @param timeSeriesFactory the factory used to fromUrl new {@link TimeSeriesProcessor} instances.
     * @param numberParser the function used to parse numbers from the CSV stream
     * @param dateParser the function used to parse dates from the CSV stream
     */
    public CsvParser(String encoding, String separator, TimeSeriesProcessorFactory<T> timeSeriesFactory, Function<String, T> numberParser, Function<String, ZonedDateTime> dateParser){
        this.encoding = encoding;
        this.separator = separator;
        this.timeSeriesFactory = timeSeriesFactory;
        this.numberParser = numberParser;
        this.dateParser = dateParser;
    }


    @Override
    public Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> parse(InputStream in, List<TimeSeriesInfo<T>> seriesInfo) throws IOException, ParseException {
        try (Profiler profiler = Profiler.start("Building time series from csv data", logger::trace)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
                String header = br.readLine();
                if (header == null || header.isEmpty()) {
                    throw new IOException("CSV File is empty!");
                }
                String[] seriesNames = header.split(separator);
                final int nbSeries = seriesNames.length - 1;

                Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> series = new HashMap<>();
                final AtomicLong nbpoints = new AtomicLong(0);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    nbpoints.incrementAndGet();
                    String[] data = line.split(separator);
                    if (data.length < 2) {
                        throw new IOException("Not enough columns in csv to plot a time series");
                    }
                    ZonedDateTime timeStamp = dateParser.apply(data[0]);
                    for (int i = 1; i < data.length; i++) {
                        TimeSeriesInfo<T> info = getBindingFromName(seriesInfo, seriesNames[i]);
                        if (info != null) {
                            T val = numberParser.apply(data[i]);
                            XYChart.Data<ZonedDateTime, T> point = new XYChart.Data<>(timeStamp, val);
                            TimeSeriesProcessor<T> l = series.computeIfAbsent(info, k -> timeSeriesFactory.create());
                            l.getData().add(point);
                        }
                    }
                }
                logger.trace(() -> String.format("Built %d series with %d samples each (%d total samples)", nbSeries, nbpoints.get(), nbSeries * nbpoints.get()));
                return series;
            }
        }
    }

    private boolean isInNameList(List<TimeSeriesBinding<T>> bindings, String seriesName) {
        if (bindings == null || bindings.size() == 0) {
            return true;
        }
        for (TimeSeriesBinding<T> binding : bindings) {
            if (binding.getLabel().equalsIgnoreCase(seriesName)) {
                return true;
            }
        }
        return false;
    }

    private TimeSeriesInfo<T> getBindingFromName(List<TimeSeriesInfo<T>> seriesInfo, String seriesName) {
        if (seriesInfo != null) {
            for (TimeSeriesInfo<T> b : seriesInfo) {
                if (b.getBinding().getLabel().equalsIgnoreCase(seriesName)) {
                    return b;
                }
            }
        }
        return null;
    }
}
