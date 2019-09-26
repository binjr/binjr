/*
 *    Copyright 2019 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.common.preferences;

import javafx.beans.property.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * A factory to create and manage preferences exposed as {@link Property} and backed by {@link Preferences}
 *
 * @author Frederic Thevenet
 */
public class MruFactory extends ReloadableItemStore<MostRecentlyUsedList<?>> {
    private static final Logger logger = LogManager.getLogger(MruFactory.class);

    public MruFactory(Preferences backingStore) {
        super(backingStore);
    }

    public MostRecentlyUsedList<String> stringMostRecentlyUsedList(String key, int capacity) {
        var mru = new MostRecentlyUsedList<String>(key, capacity, backingStore) {
            @Override
            protected boolean validate(String value) {
                return (value != null);
            }

            @Override
            protected void saveToBackend(int index, String value) {
                getBackingStore().node(getKey()).put("value_" + index, value);
            }

            @Override
            protected Optional<String> loadFromBackend(int index) {
                var p = getBackingStore().node(getKey()).get("value_" + index, null);
                if (p != null) {
                    return Optional.of(p);
                }
                return Optional.empty();
            }
        };
        storedItems.put(key, mru);
        return mru;
    }

    public MostRecentlyUsedList<URI> uriMostRecentlyUsedList(String key, int capacity) {
        var mru = new MostRecentlyUsedList<URI>(key, capacity, backingStore) {
            @Override
            protected boolean validate(URI value) {
                return true;
            }

            @Override
            protected void saveToBackend(int index, URI value) {
                getBackingStore().node(getKey()).put("value_" + index, value.toString());
            }

            @Override
            protected Optional<URI> loadFromBackend(int index) {
                var p = getBackingStore().node(getKey()).get("value_" + index, "");
                if (!p.isEmpty()) {
                    try {
                        return Optional.of(new URI(p));
                    } catch (URISyntaxException e) {
                        logger.debug(() -> "Error reloading URI: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }
        };
        storedItems.put(key, mru);
        return mru;
    }

    public MostRecentlyUsedList<Path> pathMostRecentlyUsedList(String key, int capacity, boolean mustBeDirectory) {
        var mru = new MostRecentlyUsedList<Path>(key, capacity, backingStore) {
            @Override
            protected boolean validate(Path value) {
                try {
                    if (value != null && value.toRealPath(LinkOption.NOFOLLOW_LINKS) != null
                            && (!mustBeDirectory || value.toFile().isDirectory())) {
                        return true;
                    }
                } catch (IOException e) {
                    logger.debug(() -> "Cannot insert into most recently used list: " + e.getMessage(), e);
                }
                return false;
            }

            @Override
            protected void saveToBackend(int index, Path value) {
                getBackingStore().node(getKey()).put("value_" + index, value.toString());
            }

            @Override
            protected Optional<Path> loadFromBackend(int index) {
                var p = getBackingStore().node(getKey()).get("value_" + index, "");
                if (!p.isEmpty()) {
                    return Optional.of(Path.of(p));
                }
                return Optional.empty();
            }
        };
        storedItems.put(key, mru);
        return mru;
    }

}
