/*
 *    Copyright 2017-2020 Frederic Thevenet
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

import eu.binjr.common.version.Version;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Dialog;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * An immutable representation of a {@link SerializedDataAdapter}'s metadata
 *
 * @author Frederic Thevenet
 */
public class BaseDataAdapterInfo implements DataAdapterInfo {
    private final String name;
    private final String description;
    private final Version version;
    private final String copyright;
    private final String license;
    private final String jarLocation;
    private final String siteUrl;
    private final Class<? extends DataAdapter<?>> adapterClass;
    private final Class<? extends Dialog<Collection<DataAdapter>>> adapterDialog;
    private final Version apiLevel;
    private final DataAdapterPreferences adapterPreferences;
    private final SourceLocality sourceLocality;
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);


    /**
     * Initializes a new instance of the DataAdapterInfo class.
     *
     * @param infoClass the {@link Class} onto which annotations should be read.
     * @param <T>       the type for the implementation of {@link DataAdapterInfo}
     * @throws CannotInitializeDataAdapterException if the adapter's initialization failed
     */
    protected <T extends BaseDataAdapterInfo> BaseDataAdapterInfo(Class<T> infoClass) throws CannotInitializeDataAdapterException {
        if (!infoClass.isAnnotationPresent(AdapterMetadata.class)) {
            throw new CannotInitializeDataAdapterException("Could not find annotation on class " + infoClass.getName());
        }
        var meta = infoClass.getAnnotation(AdapterMetadata.class);
        this.name = meta.name();
        this.description = meta.description();
        this.copyright = meta.copyright();
        this.license = meta.license();
        this.siteUrl = meta.siteUrl();
        this.adapterClass = meta.adapterClass();
        this.adapterDialog = meta.dialogClass();
        this.sourceLocality = meta.sourceLocality();
        this.apiLevel = Version.parseVersion(meta.apiLevel());
        var version = Version.parseVersion(meta.version());
        this.version = (version.equals(Version.emptyVersion)) ?
                AppEnvironment.getInstance().getVersion(AppEnvironment.getInstance().getManifest(meta.adapterClass()))
                : version;
        try {
            var ctor = meta.preferencesClass().getDeclaredConstructor(Class.class);
            adapterPreferences = ctor.newInstance(adapterClass);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new CannotInitializeDataAdapterException("Failed to instantiate data adapter preference class", e);
        }
        this.jarLocation = adapterClass.getResource('/' + adapterClass.getName().replace('.', '/') + ".class").toExternalForm();
        enabled.bindBidirectional(adapterPreferences.enabled.property());
    }

    /**
     * Returns the name of the data adapter.
     *
     * @return the name of the data adapter.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the description associated to the data adapter.
     *
     * @return the description associated to the data adapter.
     */
    @Override
    public String getDescription() {
        return description;
    }


    /**
     * Returns the class that implements the data adapter.
     *
     * @return the class that implements the data adapter.
     */
    @Override
    public Class<? extends DataAdapter<?>> getAdapterClass() {
        return adapterClass;
    }

    /**
     * Returns a key to uniquely identify the adapter.
     *
     * @return a key to uniquely identify the adapter.
     */
    @Override
    public String getKey() {
        return adapterClass.getName();
    }

    /**
     * Returns the class that implements the dialog box used to gather the adapter's parameters from the end user.
     *
     * @return the class that implements the dialog box used to gather the adapter's parameters from the end user.
     */
    @Override
    public Class<? extends Dialog<Collection<DataAdapter>>> getAdapterDialog() {
        return adapterDialog;
    }

    @Override
    public BooleanProperty enabledProperty() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public DataAdapterPreferences getPreferences() {
        return adapterPreferences;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getCopyright() {
        return copyright;
    }

    @Override
    public String getLicense() {
        return license;
    }

    @Override
    public String getJarLocation() {
        return jarLocation;
    }

    @Override
    public String getSiteUrl() {
        return siteUrl;
    }

    @Override
    public Version getApiLevel() {
        return this.apiLevel;
    }

    @Override
    public SourceLocality getSourceLocality() {
        return this.sourceLocality;
    }

}
