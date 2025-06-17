/*
 *    Copyright 2017-2021 Frederic Thevenet
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

import eu.binjr.common.function.CheckedFunction;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A base implementation of the {@link DataAdapter} interface that provides an implementation to the most common methods of the interface.
 *
 * @author Frederic Thevenet
 */
public abstract class BaseDataAdapter<T> implements DataAdapter<T> {
    private static final Logger logger = Logger.create(BaseDataAdapter.class);
    private UUID id = UUID.randomUUID();
    private boolean closed = false;

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

    @Deprecated
    protected String validateParameterNullity(Map<String, String> params, String paramName)
            throws InvalidAdapterParameterException {
        return mapParameter(params, paramName);
    }

    @Deprecated
    protected <R> R validateParameter(Map<String, String> params,
                                      String paramName,
                                      CheckedFunction<String, R, InvalidAdapterParameterException> validator)
            throws InvalidAdapterParameterException {
        String paramValue = params.get(paramName);
        return validator.apply(paramValue);
    }

    public <R> R mapParameter(Map<String, String> params,
                              String paramName,
                              CheckedFunction<String, R, InvalidAdapterParameterException> mapper)
            throws InvalidAdapterParameterException {
        return mapParameter(params, paramName, mapper, Optional.empty());
    }

    public <R> R mapParameter(Map<String, String> params,
                              String paramName,
                              CheckedFunction<String, R, InvalidAdapterParameterException> mapper,
                              Optional<R> defaultValue)
            throws InvalidAdapterParameterException {
        try {
            if (params == null || paramName == null) {
                throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
            }
            String paramValue = params.get(paramName);
            if (paramValue == null) {
                throw new InvalidAdapterParameterException(
                        "Parameter " + paramName + " is missing for adapter " + this.getSourceName());
            }
            return mapper.apply(paramValue);

        } catch (InvalidAdapterParameterException iae) {
            if (defaultValue.isPresent()) {
                logger.warn(() -> iae.getMessage() + ". Returning default value");
                return defaultValue.get();
            }
            throw iae;
        } catch (Exception e) {
            var msg = "Error while mapping parameter " + paramName +
                    " for adapter " + this.getSourceName() + ": " + e.getMessage();
            if (defaultValue.isPresent()) {
                logger.warn(() -> msg + ". Returning default value");
                return defaultValue.get();
            }
            throw new InvalidAdapterParameterException(msg, e);
        }
    }

    public String mapParameter(Map<String, String> params, String paramName) throws InvalidAdapterParameterException {
        return mapParameter(params, paramName, Optional.empty());
    }

    public String mapParameter(Map<String, String> params, String paramName, Optional<String> defaultValue)
            throws InvalidAdapterParameterException {
        return mapParameter(params, paramName, p -> p, defaultValue);
    }

    /**
     * Legacy
     * Use 'loadParams(Map<String, String> params, LoadingContext context)' instead
     *
     * @param params
     * @throws DataAdapterException
     */
    @Deprecated
    public void loadParams(Map<String, String> params) throws DataAdapterException {

    }

    /**
     * Default implementation that loops back to legacy 'loadParams(Map<String, String> params)'
     * Ignores additional context.
     *
     * @param params  the parameters required to establish a connection to the underlying data source
     * @param context contextual data provided to the adapter
     * @throws DataAdapterException
     */
    @Override
    public void loadParams(Map<String, String> params, LoadingContext context) throws DataAdapterException {
        this.loadParams(params);
    }
}
