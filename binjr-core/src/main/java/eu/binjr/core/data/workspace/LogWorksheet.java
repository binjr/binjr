/*
 *    Copyright 2020 Frederic Thevenet
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

import eu.binjr.core.controllers.LogWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;

import javax.xml.bind.annotation.XmlTransient;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;

public class LogWorksheet extends Worksheet {

    private final ChangeWatcher status;

     public LogWorksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")", true);
    }

    public LogWorksheet(LogWorksheet worksheet) {
        this(worksheet.getName(), worksheet.isEditModeEnabled());
    }

    public LogWorksheet(String name, boolean chartLegendsVisible) {
        super(name, chartLegendsVisible);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return LogWorksheetController.class;
    }

    @Override
    public Worksheet duplicate() {
        return new LogWorksheet(this);
    }

    @Override
    public void close() {

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
}
