package jrds;

import eu.fthevenet.binjr.data.JRDSDataProvider;
import eu.fthevenet.binjr.data.TimeSeriesBuilder;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;


/**
 * Created by FTT2 on 24/10/2016.
 */
public class JrdsSourceTester {
    private static final Logger logger = LogManager.getLogger(JrdsSourceTester.class);
    public static void main(String[] args) {
        String jrdsHost = "ngwps006:31001";
        String target = "ngwps006";
        String probe = "memprocPdh";

        Instant end = Instant.now();
        Instant begin = end.minus(24*60*7, ChronoUnit.MINUTES);

        JRDSDataProvider dp = new JRDSDataProvider(jrdsHost);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (dp.getData(target, probe, begin, end, out)) {
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                Map<String,XYChart.Series<Date, Number>> series = TimeSeriesBuilder.fromCSV(in);

            }
            else {
                logger.error(String.format("Failed to retrieve data from JRDS for %s %s %s %s", target, probe, begin.toString(), end.toString()));
            }

        } catch (IOException e) {
            logger.error("");
        }

    }
}
