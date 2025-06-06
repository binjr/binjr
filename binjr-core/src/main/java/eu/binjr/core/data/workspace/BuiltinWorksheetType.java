/*
 *    Copyright 2023 Frederic Thevenet
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

import eu.binjr.core.data.adapters.VisualizationType;

public enum BuiltinWorksheetType implements WorksheetType {
    CHART_WORKSHEET(XYChartsWorksheet.class, "Charts Worksheet", VisualizationType.CHARTS, true),
    EVENTS_WORKSHEET(LogWorksheet.class, "Events Worksheet", VisualizationType.EVENTS, true),
    TEXT_WORKSHEET(TextFilesWorksheet.class, "Text Worksheet", VisualizationType.TEXT, true),
    TERMINAL_WORKSHEET(TerminalWorksheet.class, "Terminal", VisualizationType.TERMINAL, true);

    private final Class<? extends Worksheet<?>> type;
    private final String label;
    private final VisualizationType visualizationType;
    private final Boolean showInMenu;

    BuiltinWorksheetType(Class<? extends Worksheet<?>> type, String label, VisualizationType visualizationType, Boolean showInMenu) {
        this.type = type;
        this.label = label;
        this.showInMenu = showInMenu;
        this.visualizationType = visualizationType;
    }

    @Override
    public Class<? extends Worksheet<?>> getType() {
        return type;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public VisualizationType getVisualizationType() {
        return visualizationType;
    }

    @Override
    public Boolean getShowInMenu() {
        return showInMenu;
    }


    @Override
    public String toString() {
        return label;
    }
}
