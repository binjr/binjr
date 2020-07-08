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

import eu.binjr.core.data.workspace.Worksheet;

import javax.xml.bind.annotation.*;
import java.util.UUID;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Binding")
public abstract class SourceBinding {
    @XmlAttribute(name = "sourceId")
    protected final UUID adapterId;
    @XmlAttribute
    protected final String label;
    @XmlAttribute
    protected final String path;
    @XmlAttribute
    protected final String treeHierarchy;
    @XmlAttribute
    protected final String legend;
    @XmlTransient
    protected DataAdapter adapter;

    protected SourceBinding() {
        this.adapter = null;
        this.label = "";
        this.path = "";
        this.treeHierarchy = "";
        adapterId = null;
        this.legend = "";
    }

    protected SourceBinding(String label, String legend, String path, String treeHierarchy, DataAdapter adapter) {
        this(label, legend , path, treeHierarchy, adapter, null);
    }


    protected SourceBinding(String label, String legend, String path, String treeHierarchy, DataAdapter adapter, UUID adapterId) {
        this.label = label;
        this.path = path;
        this.treeHierarchy = treeHierarchy;
        this.adapter = adapter;
        UUID id = adapterId;
        if (id == null && adapter != null) {
            id = adapter.getId();
        }
        this.adapterId = id;
        this.legend = legend;
    }

    /**
     * Returns the label of the binding
     *
     * @return the label of the binding
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns the path of the binding
     *
     * @return the path of the binding
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Returns the {@link SerializedDataAdapter} of the binding
     *
     * @return the {@link SerializedDataAdapter} of the binding
     */
    @XmlTransient
    public DataAdapter getAdapter() {
        return this.adapter;
    }

    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Gets the {@link SerializedDataAdapter}'s id
     *
     * @return the {@link SerializedDataAdapter}'s id
     */
    public UUID getAdapterId() {
        return adapterId;
    }

    /**
     * Returns the full hierarchy in the tree for the binding
     *
     * @return the full hierarchy in the tree for the binding
     */
    public String getTreeHierarchy() {
        return treeHierarchy;
    }

    /**
     * Returns the legend of the bound series as defined in the source.
     *
     * @return the legend of the bound series as defined in the source.
     */
    public String getLegend() {
        return this.legend;
    }

    @Override
    public String toString() {
        return getLegend();
    }

    public abstract Class<? extends Worksheet> getWorksheetClass();

    public abstract static class Builder<T extends SourceBinding, B extends Builder<T,B>> {
        private String label = "";
        private String path = "";
        private String legend = null;
        private String treeHierarchy = "";
        private DataAdapter adapter;
        private String parent = null;

        protected abstract B self();

        public B withAdapter(DataAdapter adapter) {

            this.adapter =adapter;
            return self();
        }

        public B withLabel(String label) {
            this.label = label;
            return self();
        }

        public B withPath(String path) {
            this.path = path;
            return self();
        }

        public B withLegend(String legend) {
            this.legend = legend;
            return self();
        }


        public B withHierarchy(String treeHierarchy) {
            this.treeHierarchy = treeHierarchy;
            return self();
        }

        public B withParent(String parent) {
            this.parent = parent;
            return self();
        }

        public T build()        {
            if (legend == null) {
                legend = label;
            }
            if (parent != null) {
                treeHierarchy = parent + "/" + legend;
            }
            return construct(label, legend, path, treeHierarchy, adapter);
        }

        protected abstract T construct(String label, String legend, String path, String treeHierarchy, DataAdapter adapter);
    }
}
