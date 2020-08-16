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
import javafx.scene.paint.Color;

public class LogFilesBinding extends TextFilesBinding {

    private transient boolean indexed = false;

    public LogFilesBinding() {
        super();
    }

    public LogFilesBinding(String label, String legend, String path, String treeHierarchy, DataAdapter<String> adapter) {
        super(label, legend, path, treeHierarchy, adapter);
    }

    @Override
    public Class<? extends Worksheet<String>> getWorksheetClass() {
        return TextFilesWorksheet.class;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public static class Builder extends SourceBinding.Builder<String, LogFilesBinding, Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected LogFilesBinding construct(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<String> adapter) {
            return new LogFilesBinding(label, legend, path, treeHierarchy, adapter);
        }
    }

}
