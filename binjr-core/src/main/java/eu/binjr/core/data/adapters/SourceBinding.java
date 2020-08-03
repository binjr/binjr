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

import eu.binjr.common.logging.Logger;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.data.workspace.Worksheet;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Binding")
public abstract class SourceBinding<T> {
    private static final Logger logger = Logger.create(SourceBinding.class);
    private static final ThreadLocal<MessageDigest> messageDigest = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Failed to instantiate MD5 message digest");
            throw new IllegalStateException("Failed to create a new instance of Md5HashTargetResolver", e);
        }
    });



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
    protected DataAdapter<T> adapter;
    @XmlAttribute
    protected final Color color;

    protected SourceBinding() {
        this.adapter = null;
        this.label = "";
        this.path = "";
        this.color = null;
        this.treeHierarchy = "";
        adapterId = null;
        this.legend = "";
    }

    protected SourceBinding(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<T> adapter) {
        this(label, legend , color, path, treeHierarchy, adapter, null);
    }


    protected SourceBinding(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<T> adapter, UUID adapterId) {
        this.label = label;
        this.path = path;
        this.color = color;
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
    public DataAdapter<T> getAdapter() {
        return this.adapter;
    }

    public void setAdapter(DataAdapter<T> adapter) {
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

    /**
     * Returns the color of the bound series as defined in the source.
     *
     * @return the color of the bound series as defined in the source.
     */
    public Color getColor() {
        return this.color == null ? computeDefaultColor() : this.color;
    }

    @Override
    public String toString() {
        return getLegend();
    }

    public abstract Class<? extends Worksheet> getWorksheetClass();

    private Color computeDefaultColor() {
        long targetNum = getHashValue(this.getLabel()) % StageAppearanceManager.getInstance().getDefaultChartColors().length;
        if (targetNum < 0) {
            targetNum = targetNum * -1;
        }
        return StageAppearanceManager.getInstance().getDefaultChartColors()[((int) targetNum)];
    }

    private long getHashValue(final String value) {
        long hashVal;
        messageDigest.get().update(value.getBytes(StandardCharsets.UTF_8));
        hashVal = new BigInteger(1, messageDigest.get().digest()).longValue();
        return hashVal;
    }


    public abstract static class Builder<T, S extends SourceBinding<T>, B extends Builder<T,S,B>> {
        private String label = "";
        private String path = "";
        private String legend = null;
        private Color color = null;
        private DataAdapter<T> adapter;
        private String parent = "";

        protected abstract B self();

        public B withAdapter(DataAdapter<T> adapter) {
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

        public B withColor(Color color) {
            this.color = color;
            return self();
        }

        public B withLegend(String legend) {
            this.legend = legend;
            return self();
        }


        public B withParent(SourceBinding<T> parent) {
            this.parent = parent.getTreeHierarchy();
            return self();
        }

        public S build()        {
            if (legend == null) {
                legend = label;
            }
            return construct(label, legend,color, path, parent + "/" + legend, adapter);
        }

        protected abstract S construct(String label, String legend, Color color, String path, String treeHierarchy, DataAdapter<T> adapter);

    }
}
