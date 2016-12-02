package eu.fthevenet.binjr.data.providers;

import java.io.OutputStream;
import java.time.Instant;

/**
 * Created by FTT2 on 14/10/2016.
 */
public interface DataProvider  {


    boolean getData(String targetHost, String probe, Instant begin, Instant end, OutputStream out );


}
