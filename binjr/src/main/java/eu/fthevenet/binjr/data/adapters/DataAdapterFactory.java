package eu.fthevenet.binjr.data.adapters;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;

/**
 * A functional interface to be used as a producer for {@link DataAdapter} instances.
 */
@FunctionalInterface
public interface DataAdapterFactory<T extends Number> {
    /**
     * Initializes a new instance of the {@link DataAdapter} class from the provided URL and timezone
     * @param url the URL the build the adapter fom
     * @param zoneId the timezone of the source
     * @return a new instance of the {@link DataAdapter} class from the provided url and timezone
     * @throws MalformedURLException if the URL syntax is invalid
     */
    DataAdapter<T> fromUrl(String url, ZoneId zoneId) throws MalformedURLException;
}
