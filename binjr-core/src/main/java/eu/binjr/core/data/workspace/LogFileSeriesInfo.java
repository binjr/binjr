/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.core.data.workspace;

import eu.binjr.common.colors.ColorUtils;
import eu.binjr.core.data.adapters.LogFilesBinding;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

/**
 * A class that represents and holds the current state of the representation of a single time series
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Timeseries")
public class LogFileSeriesInfo extends TimeSeriesInfo<SearchHit> {

    @IsDirtyable
    private final Property<ParsingProfile> parsingProfile;

    private final Property<ReloadStatus> indexingStatus = new SimpleObjectProperty<>(ReloadStatus.OK);

    @XmlTransient
    private final ChangeWatcher status;

    /**
     * Parameter-less constructor (needed for XMl serialization)
     */
    private LogFileSeriesInfo() {
        this("",
                true,
                null,
                null,
                null);
    }

    /**
     * Copy constructor to deep clone a {@link LogFileSeriesInfo} instance.
     * <p><b>Remark:</b></p>
     * <p>All the properties of the new {@link LogFileSeriesInfo} instance are new objects, assigned the same values, except for the {@code binding} property
     * which holds a copy of the reference. <br>In other words, the {@link TimeSeriesBinding} reference is shared amongst all clones produced by this constructor.
     * </p>
     *
     * @param seriesInfo the {@link LogFileSeriesInfo} instance to clone.
     */
    public LogFileSeriesInfo(LogFileSeriesInfo seriesInfo) {
        this(seriesInfo.getDisplayName(),
                seriesInfo.isSelected(),
                ColorUtils.copy(seriesInfo.getDisplayColor()),
                seriesInfo.getParsingProfile(),
                seriesInfo.getBinding());
    }

    /**
     * Initialises a new instance of the {@link LogFileSeriesInfo} class
     *
     * @param displayName  the name for the series
     * @param selected     true if the series is selected, false otherwise
     * @param displayColor the color of the series
     * @param binding      the {@link TimeSeriesBinding}  for the series
     */
    public LogFileSeriesInfo(String displayName,
                             Boolean selected,
                             Color displayColor,
                             ParsingProfile parsingProfile,
                             SourceBinding<SearchHit> binding) {
        super(displayName, selected, displayColor, binding);
        this.parsingProfile = new SimpleObjectProperty<>(parsingProfile);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    /**
     * Returns a new instance of the {@link LogFileSeriesInfo} class built from the specified {@link TimeSeriesBinding}
     *
     * @param binding the {@link TimeSeriesBinding} to build the {@link LogFileSeriesInfo} from
     * @return a new instance of the {@link LogFileSeriesInfo} class built from the specified {@link TimeSeriesBinding}
     */
    public static LogFileSeriesInfo fromBinding(LogFilesBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding cannot be null");
        }
        return new LogFileSeriesInfo(binding.getLegend(),
                true,
                binding.getColor(),
            //   CustomParsingProfile.of(binding.getParsingProfile()),
                binding.getParsingProfile(),
                binding);
    }

    public static String makePathFacetValue(ParsingProfile parsingProfile, TimeSeriesInfo<?> tsInfo) {
        return parsingProfile.getProfileId() + "/" + tsInfo.getBinding().getPath();
    }

    @Override
    public String asTabSeparatedValues() {
        return String.join("\t",
                this.getDisplayName(),
                this.getParsingProfile().getProfileName(),
                this.getBinding().getTreeHierarchy());
    }

    @Override
    public String toString() {
        return "TimeSeriesInfo{" +
                "displayName=" + displayName +
                ", selected=" + selected +
                ", displayColor=" + displayColor +
                ", binding=" + binding +
                '}';
    }

    @Override
    public void close() {
        this.status.close();
    }

    @XmlAttribute
    public ParsingProfile getParsingProfile() {
        return parsingProfile.getValue();
    }

    public void setParsingProfile(ParsingProfile parsingProfile) {
        this.parsingProfile.setValue(parsingProfile);
    }

    public Property<ParsingProfile> parsingProfileProperty() {
        return this.parsingProfile;
    }

    @XmlTransient
    public String getPathFacetValue() {
        return makePathFacetValue(getParsingProfile(), this);
    }

    @XmlTransient
    public ReloadStatus isLoadComplete() {
        return indexingStatus.getValue();
    }

    public Property<ReloadStatus> indexingStatusProperty() {
        return indexingStatus;
    }

    public void setIndexingStatus(ReloadStatus status) {
        this.indexingStatus.setValue(status);
    }
}
