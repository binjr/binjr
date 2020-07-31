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

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;

public class TextFilesBinding extends SourceBinding<String> {

    public TextFilesBinding() {
        super();
    }

    public TextFilesBinding(String label, String legend, String path, String treeHierarchy, DataAdapter<String> adapter) {
        super(label, legend, path, treeHierarchy, adapter);
    }

    @Override
    public Class<? extends Worksheet> getWorksheetClass() {
        return TextFilesWorksheet.class;
    }

    public static class Builder extends SourceBinding.Builder<String, TextFilesBinding, Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected TextFilesBinding construct(String label, String legend, String path, String treeHierarchy, DataAdapter<String> adapter) {
            return new TextFilesBinding(label, legend, path, treeHierarchy, adapter);
        }
    }

}
