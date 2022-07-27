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

package eu.binjr.core.data.adapters;

import eu.binjr.core.data.workspace.TextFilesWorksheet;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.paint.Color;

public class TextFilesBinding extends SourceBinding<String> {
    public static final String MIME_TYPE = "x-binjr/TextFilesBinding";

    public TextFilesBinding() {
        super();
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    public TextFilesBinding(String label, String legend, String path, String treeHierarchy, DataAdapter<String> adapter) {
        super(label, legend, null, path, treeHierarchy, adapter);
    }

    @Override
    public Class<? extends Worksheet<String>> getWorksheetClass() {
        return TextFilesWorksheet.class;
    }

    @Override
    protected Color[] getDefaultColorPalette() {
        return UserPreferences.getInstance().logFilesColorPalette.get().getPalette();
    }

    public static class Builder extends SourceBinding.Builder<String, TextFilesBinding, Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected TextFilesBinding construct(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<String> adapter) {
            return new TextFilesBinding(label, legend, path, treeHierarchy, adapter);
        }
    }

}
