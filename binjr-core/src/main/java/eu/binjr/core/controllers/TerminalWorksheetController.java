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

package eu.binjr.core.controllers;


import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.techsenger.jeditermfx.core.ProcessTtyConnector;
import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import eu.binjr.common.javafx.controls.BinjrLoadingPane;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.TerminalWorksheet;
import eu.binjr.core.data.workspace.Worksheet;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

public class TerminalWorksheetController extends WorksheetController {
    private static final Logger logger = Logger.create(TerminalWorksheetController.class);
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/TerminalWorksheetView.fxml";
    private final TerminalWorksheet worksheet;
    private final Property<TimeRange> timeRangeProperty = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));


    public TerminalWorksheetController(MainViewController parent, TerminalWorksheet worksheet, Collection<DataAdapter<String>> adapters)
            throws NoAdapterFoundException {
        super(parent);
        this.worksheet = worksheet;
        for (var d : worksheet.getSeriesInfo()) {
            UUID id = d.getBinding().getAdapterId();
            DataAdapter<String> da = adapters
                    .stream()
                    .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                    .findAny()
                    .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " +
                            (id != null ? id.toString() : "null")));
            d.getBinding().setAdapter(da);
        }
    }

    @FXML
    private AnchorPane root;

    @FXML
    private Pane viewArea;

    @FXML
    private ToggleButton textSizeButton;

    @FXML
    private ToggleButton wordWrapButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button searchHistoryButton;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button clearSearchButton;

    @FXML
    private ToggleButton searchMatchCaseToggle;

    @FXML
    private ToggleButton searchRegExToggle;

    @FXML
    private Label searchResultsLabel;

    @FXML
    private Button prevOccurrenceButton;

    @FXML
    private Button nextOccurrenceButton;

    @Override
    public Worksheet<?> getWorksheet() {
        return worksheet;
    }

    @FXML
    public BinjrLoadingPane busyIndicator;

    @Override
    public Property<TimeRange> selectedRangeProperty() {
        return timeRangeProperty;
    }

    @Override
    public Optional<ChartViewPort> getAttachedViewport(TitledPane pane) {
        return Optional.empty();
    }

    @Override
    public ContextMenu getChartListContextMenu(Collection<TreeItem<SourceBinding>> treeView) {
        MenuItem item = new MenuItem(worksheet.getName());
        item.setDisable(true);
        return new ContextMenu(item);
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
    }

    @Override
    public void refresh() {
        invalidate(null, false, true);
    }

    public void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        invalidate(null, dontPlotChart, forceRefresh);
    }

    public void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh) {
        if (forceRefresh) {

        }
    }

    @Override
    public void toggleShowPropertiesPane() {

    }

    @Override
    public void setShowPropertiesPane(boolean value) {

    }

    @Override
    public List<ChartViewPort> getViewPorts() {
        return new ArrayList<>();
    }


    @Override
    public void close() {
        super.close();
    }

    @Override
    public String getView() {
        return WORKSHEET_VIEW_FXML;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (getWorksheet() instanceof TerminalWorksheet termWorksheet){
            viewArea.getChildren().add(termWorksheet.getTerminal().getPane());
        }

    }



//    public class TtyConnectorWaitFor {
//
//        private static final Logger logger = Logger.create(TtyConnectorWaitFor.class);
//
//        public TtyConnectorWaitFor( TtyConnector ttyConnector, ExecutorService executor,
//                                   @ IntConsumer terminationCallback) {
//            executor.submit(() -> {
//                int exitCode = 0;
//                try {
//                    while (true) {
//                        try {
//                            exitCode = ttyConnector.waitFor();
//                            break;
//                        } catch (InterruptedException e) {
//                            logger.debug("", e);
//                        }
//                    }
//                } finally {
//                    terminationCallback.accept(exitCode);
//                }
//            });
//        }
//    }

}
