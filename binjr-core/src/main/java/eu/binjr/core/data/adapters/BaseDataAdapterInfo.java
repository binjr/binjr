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

import eu.binjr.common.preferences.ObservablePreferenceFactory;
import eu.binjr.common.version.Version;
import eu.binjr.core.preferences.AppEnvironment;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Dialog;

import java.util.Objects;

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
    private final Class<? extends Dialog<DataAdapter<?>>> adapterDialog;
    private final Version apiLevel;
    private final DataAdapterPreferences adapterPreferences;
    private final SourceLocality sourceLocality;
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, null, null, null, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, null, null, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  SourceLocality sourceLocality) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, null, sourceLocality, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  SourceLocality sourceLocality,
                                  Version apiLevel) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, null, sourceLocality, apiLevel);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license, String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  DataAdapterPreferences preferences) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, null, preferences, null, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  DataAdapterPreferences preferences) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, preferences, null, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  DataAdapterPreferences preferences,
                                  SourceLocality sourceLocality) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, preferences, sourceLocality, null);
    }

    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  DataAdapterPreferences preferences,
                                  SourceLocality sourceLocality,
                                  Version apiLevel) {
        this(name, description, null, copyright, license, siteUrl, adapterClass, dialogClass, preferences, sourceLocality, apiLevel);
    }

//    protected BaseDataAdapterInfo(String name,
//                                  String description,
//                                  Version version,
//                                  String copyright,
//                                  String license,
//                                  String siteUrl,
//                                  Class<? extends DataAdapter<?>> adapterClass,
//                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
//                                  DataAdapterPreferences preferences,
//                                  SourceLocality sourceLocality) {
//        this(name, description, version, copyright, license, siteUrl, adapterClass, dialogClass, preferences, sourceLocality, null);
//    }

    /**
     * Initializes a new instance of the DataAdapterInfo class.
     *
     * @param name         the name of the data adapter.
     * @param description  the description associated to the data adapter.
     * @param version      the version information related to the data adapter
     * @param copyright    the copyright information related to the data adapter
     * @param license      the license information related to the data adapter
     * @param siteUrl      the version information related to the data adapter
     * @param adapterClass the class that implements the data adapter.
     * @param dialogClass  the class that implements the dialog box used to gather the adapter's parameters from the end user.
     * @param preferences  An instance of {@link DataAdapterPreferences} for this adapter.
     */
    protected BaseDataAdapterInfo(String name,
                                  String description,
                                  Version version,
                                  String copyright,
                                  String license,
                                  String siteUrl,
                                  Class<? extends DataAdapter<?>> adapterClass,
                                  Class<? extends Dialog<DataAdapter<?>>> dialogClass,
                                  DataAdapterPreferences preferences,
                                  SourceLocality sourceLocality,
                                  Version apiLevel) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(copyright);
        Objects.requireNonNull(license);
        Objects.requireNonNull(siteUrl);
        Objects.requireNonNull(adapterClass);

        this.name = name;
        this.description = description;
        this.copyright = copyright;
        this.license = license;
        this.siteUrl = siteUrl;
        this.adapterClass = adapterClass;
        this.adapterDialog = dialogClass;
        this.sourceLocality = Objects.requireNonNullElse(sourceLocality, SourceLocality.UNKNOWN);
        this.apiLevel = Objects.requireNonNullElse(apiLevel, Version.emptyVersion);
        this.version = Objects.requireNonNullElseGet(version, () -> AppEnvironment.getInstance().getVersion(adapterClass));
        this.jarLocation = adapterClass.getResource('/' + adapterClass.getName().replace('.', '/') + ".class").toExternalForm();
        adapterPreferences = Objects.requireNonNullElseGet(preferences, () -> new DataAdapterPreferences(adapterClass));
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
    public Class<? extends Dialog<DataAdapter<?>>> getAdapterDialog() {
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
    public ObservablePreferenceFactory getPreferences() {
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
