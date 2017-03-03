package eu.fthevenet.binjr.data.adapters;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.util.Map;

/**
 * A functional interface to be used as a producer for {@link DataAdapter} instances.
 */
@FunctionalInterface
public interface DataAdapterFactory<T extends Number> {
    /**
     * Initializes a new instance of the {@link DataAdapter} class from the provided URL and timezone
     * @param params a map of parameters required to estacblish a connection to the source
     *
     * @return a new instance of the {@link DataAdapter} class from the provided url and timezone
     */
    DataAdapter<T> fromParams(Map<String, String> params);
}
