/*
 *    Copyright 2020-2021 Frederic Thevenet
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

import eu.binjr.core.controllers.TextWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.TextFilesBinding;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import jakarta.xml.bind.annotation.*;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TextFilesWorksheet extends Worksheet<String> {
    private final transient ChangeWatcher status;
    @IsDirtyable
    private final ObservableList<TimeSeriesInfo<String>> seriesInfo = FXCollections.observableList(new LinkedList<>());

    @IsDirtyable
    private final IntegerProperty textViewFontSize = new SimpleIntegerProperty(10);
    private boolean syntaxHighlightEnabled = true;


    public TextFilesWorksheet() {
        this("New File (" + globalCounter.getAndIncrement() + ")", true);
    }

    protected TextFilesWorksheet(String name, boolean editModeEnabled) {
        super(name, editModeEnabled);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);

    }

    private TextFilesWorksheet(TextFilesWorksheet worksheet) {
        this(worksheet.getName(), worksheet.isEditModeEnabled());
        seriesInfo.addAll(worksheet.getSeriesInfo());
    }

    @XmlElementWrapper(name = "Files")
    @XmlElements(@XmlElement(name = "Files"))
    public ObservableList<TimeSeriesInfo<String>> getSeriesInfo() {
        return seriesInfo;
    }

    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return TextWorksheetController.class;
    }

    @Override
    public Worksheet<String> duplicate() {
        return new TextFilesWorksheet(this);
    }

    @Override
    public void close() {

    }

    @Override
    public TextFilesWorksheet clone(){
        return new TextFilesWorksheet(this);
    }

    @Override
    public void initWithBindings(String title, BindingsHierarchy... bindingsHierarchies) throws DataAdapterException {
        this.setName(title);
        for (var root : bindingsHierarchies) {
            // we're only interested in the leaves
            for (var b : root.getBindings()) {
                if (b instanceof TextFilesBinding textBinding) {
                    this.seriesInfo.add(TimeSeriesInfo.fromBinding(textBinding));
                }
            }
        }
    }

    @Override
    protected List<TimeSeriesInfo<String>> listAllSeriesInfo() {
        return getSeriesInfo();
    }

    // region Dirtyable
    @XmlTransient
    @Override
    public Boolean isDirty() {
        return status.isDirty();
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return status.dirtyProperty();
    }

    @Override
    public void cleanUp() {
        status.cleanUp();
    }



    @XmlAttribute
    public int getTextViewFontSize() {
        return textViewFontSize.get();
    }

    public IntegerProperty textViewFontSizeProperty() {
        return textViewFontSize;
    }

    public void setTextViewFontSize(int textViewFontSize) {
        this.textViewFontSize.set(textViewFontSize);
    }


    public boolean isSyntaxHighlightEnabled() {
        return syntaxHighlightEnabled;
    }

    public void setSyntaxHighlightEnabled(boolean syntaxHighlightEnabled) {
        this.syntaxHighlightEnabled = syntaxHighlightEnabled;
    }
}
