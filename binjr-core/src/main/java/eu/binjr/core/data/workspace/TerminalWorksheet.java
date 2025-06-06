/*
 *    Copyright 2020-2023 Frederic Thevenet
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

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.techsenger.jeditermfx.core.ProcessTtyConnector;
import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.TerminalPanel;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import eu.binjr.core.controllers.TerminalWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.TextFilesBinding;
import eu.binjr.core.data.adapters.VisualizationType;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TerminalWorksheet extends Worksheet<String> {
    private final transient ChangeWatcher status;
    @IsDirtyable
    private final ObservableList<TimeSeriesInfo<String>> seriesInfo = FXCollections.observableList(new LinkedList<>());

    @IsDirtyable
    private final IntegerProperty textViewFontSize = new SimpleIntegerProperty(UserPreferences.getInstance().defaultTextViewFontSize.get().intValue());
    private boolean syntaxHighlightEnabled = true;

    private final JediTermFxWidget terminal;


    public TerminalWorksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")", true);
    }

    protected TerminalWorksheet(String name, boolean editModeEnabled) {
        super(name, editModeEnabled);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
        this.terminal = new JediTermFxWidget(80, 24, new DefaultSettingsProvider() {
            @Override
            public TerminalColor getDefaultBackground() {
                return new TerminalColor(0, 0, 0);
            }

            @Override
            public TerminalColor getDefaultForeground() {
                return new TerminalColor(255, 255, 255);
            }
        });
        terminal.setTtyConnector(createTtyConnector());
        terminal.addHyperlinkFilter(new DefaultHyperlinkFilter());
        terminal.start();

    }

    private TerminalWorksheet(TerminalWorksheet worksheet) {
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
        return TerminalWorksheetController.class;
    }

    @Override
    public TerminalWorksheet duplicate() {
        return new TerminalWorksheet(this);
    }

    @Override
    public void close() {
        terminal.close();
        terminal.getTtyConnector().close();
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

    @XmlTransient
    @Override
    public VisualizationType getVisualizationType() {
        return VisualizationType.TEXT;
    }

    public JediTermFxWidget getTerminal() {
      return this.terminal;
    }


    private TtyConnector createTtyConnector() {
        try {
            Map<String, String> envs = System.getenv();
            String[] command;
            if (com.techsenger.jeditermfx.core.util.Platform.isWindows()) {
                command = new String[]{"cmd.exe"};
            } else {
                command = new String[]{"/bin/bash", "--login"};
                envs = new HashMap<>(System.getenv());
                envs.put("TERM", "xterm-256color");
            }
            PtyProcess process = new PtyProcessBuilder().setCommand(command).setEnvironment(envs).start();
            return new PtyProcessTtyConnector(process, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public class PtyProcessTtyConnector extends ProcessTtyConnector {

        private final PtyProcess myProcess;

        public PtyProcessTtyConnector(PtyProcess process, Charset charset) {
            this(process, charset, null);
        }

        public PtyProcessTtyConnector(PtyProcess process, Charset charset, List<String> commandLine) {
            super(process, charset, commandLine);
            myProcess = process;
        }

        @Override
        public void resize(TermSize termSize) {
            if (isConnected()) {
                myProcess.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
            }
        }

        @Override
        public boolean isConnected() {
            return myProcess.isAlive();
        }

        @Override
        public String getName() {
            return "Local";
        }
    }

}
