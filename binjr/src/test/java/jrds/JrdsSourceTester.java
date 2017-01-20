package jrds;

import eu.fthevenet.binjr.data.providers.DataProviderException;
import eu.fthevenet.binjr.data.providers.jrds.JRDSDataProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


/**
 * Created by FTT2 on 24/10/2016.
 */
public class JrdsSourceTester {
    private static final Logger logger = LogManager.getLogger(JrdsSourceTester.class);
    public static void main(String[] args) throws DataProviderException {
        String host = "ngwps006";
        String target = "ngwps006";
        String probe = "memprocPdh";
        int port = 31001;
        String path = "/perf-ui";

        Instant end = Instant.now();
        Instant begin = end.minus(24*60*7, ChronoUnit.MINUTES);

        JRDSDataProvider dp = new JRDSDataProvider(host,port, path);

        try{

            System.out.printf(dp.getJsonTree("hoststab"));
            dp.getJRDSTree();
        }
        catch (DataProviderException e){
            logger.error(e);
        }

//        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            if (dp.getData(target, probe, begin, end, out) >0) {
//                InputStream in = new ByteArrayInputStream(out.toByteArray());
//                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
//                TimeSeriesBuilder<Double> tb = new TimeSeriesBuilder<>(ZoneId.systemDefault(),
//                        s -> {
//                            Double val = Double.parseDouble(s);
//                            return val.isNaN() ? 0 : val;
//                        },
//                        s -> ZonedDateTime.parse(s, formatter));
//                Map<String,XYChart.Series<ZonedDateTime, Double>> series = tb.fromCSV(in).build();
//
//            }
//            else {
//                logger.error(String.format("Failed to retrieve data from JRDS for %s %s %s %s", target, probe, begin.toString(), end.toString()));
//            }
//
//        } catch (IOException | ParseException e) {
//            logger.error(e);
//        }

    }
}
