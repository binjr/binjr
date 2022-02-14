/*
 *    Copyright 2020-2022 Frederic Thevenet
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

import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.workspace.LogWorksheet;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlTransient;
import javafx.scene.paint.Color;

import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class LogFilesBinding extends SourceBinding<SearchHit> {

    public static final String MIME_TYPE = "x-binjr/LogFilesBinding";
    private transient boolean indexed = false;

    @XmlTransient
    private final ParsingProfile parsingProfile;

    private LogFilesBinding() {
        super();
        this.parsingProfile = null;
    }

    public LogFilesBinding(String label,
                           String legend,
                           String path,
                           String treeHierarchy,
                           ParsingProfile parsingProfile,
                           DataAdapter<SearchHit> adapter) {
        super(label, legend, null, path, treeHierarchy, adapter);
        this.parsingProfile = parsingProfile;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    public ParsingProfile getParsingProfile() {
        return parsingProfile;
    }

    @Override
    public int hashCode() {
        return super.hashCode() +
                Objects.hashCode(parsingProfile);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) &&
                Objects.equals(parsingProfile, ((LogFilesBinding) obj).parsingProfile);
    }

    @Override
    public Class<? extends Worksheet<SearchHit>> getWorksheetClass() {
        return LogWorksheet.class;
    }

    @Override
    protected Color[] getDefaultColorPalette() {
        return UserPreferences.getInstance().logFilesColorPalette.get().getPalette();
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public static class Builder extends SourceBinding.Builder<SearchHit, LogFilesBinding, Builder> {
        private ParsingProfile parsingProfile;

        @Override
        protected Builder self() {
            return this;
        }

        public LogFilesBinding.Builder withParsingProfile(ParsingProfile parsingProfile) {
            this.parsingProfile = parsingProfile;
            return self();
        }

        @Override
        protected LogFilesBinding construct(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<SearchHit> adapter) {
            return new LogFilesBinding(label, legend, path, treeHierarchy, parsingProfile, adapter);
        }
    }

}
