/*
 *    Copyright 2017-2019 Frederic Thevenet
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

package eu.binjr.core.data.adapters;

import eu.binjr.common.preferences.PreferenceFactory;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.common.function.CheckedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * A base implementation of the {@link DataAdapter} interface that provides an implementation to the most common methods of the interface.
 *
 * @author Frederic Thevenet
 */
public abstract class BaseDataAdapter implements DataAdapter {
    private static final Logger logger = LogManager.getLogger(BaseDataAdapter.class);
    private UUID id = UUID.randomUUID();
    private volatile boolean closed = false;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void onStart() throws DataAdapterException {
        //noop
    }

    @Override
    public void close() {
        logger.trace("Closing DataAdapter " + getId());
        closed = true;
    }

    @Override
    public String toString() {
        return "DataAdapter{" +
                "id=" + id +
                "sourceName" + getSourceName() +
                '}';
    }

    protected String validateParameterNullity(Map<String, String> params, String paramName) throws InvalidAdapterParameterException {
        return validateParameter(params, paramName, s -> {
            if (s == null) {
                throw new InvalidAdapterParameterException("Parameter " + paramName + " is missing for adapter " + this.getSourceName());
            }
            return s;
        });
    }

    protected <R> R validateParameter(Map<String, String> params, String paramName, CheckedFunction<String, R, InvalidAdapterParameterException> validator) throws InvalidAdapterParameterException {
        String paramValue = params.get(paramName);
        return validator.apply(paramValue);
    }
}
