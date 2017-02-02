package eu.fthevenet.binjr.data.adapters;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;

/**
 * Created by FTT2 on 02/02/2017.
 */
@FunctionalInterface
public interface DataAdapterFactory<T extends Number> {
    DataAdapter<T> fromUrl(String url, ZoneId zoneId) throws MalformedURLException;
}
