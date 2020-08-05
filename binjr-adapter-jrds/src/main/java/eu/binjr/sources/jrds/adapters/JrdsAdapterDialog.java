/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.sources.jrds.adapters;

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.net.URI;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;


/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link JrdsDataAdapter}
 *
 * @author Frederic Thevenet
 */
public class JrdsAdapterDialog extends DataAdapterDialog<URI> {
    private final ChoiceBox<JrdsTreeViewTab> tabsChoiceBox;
    private final TextField extraArgumentTextField;

    /**
     * Initializes a new instance of the {@link JrdsAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public JrdsAdapterDialog(Node owner) {
        super(owner, Mode.URI, "mostRecentJrdsUrls");
        this.setDialogHeaderText("Connect to a JRDS source");
        this.tabsChoiceBox = new ChoiceBox<>();
        tabsChoiceBox.getItems().addAll(JrdsTreeViewTab.values());
        this.extraArgumentTextField = new TextField();
        HBox.setHgrow(extraArgumentTextField, Priority.ALWAYS);
        HBox hBox = new HBox(tabsChoiceBox, extraArgumentTextField);
        hBox.setSpacing(10);

        GridPane.setConstraints(hBox, 1, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        tabsChoiceBox.getSelectionModel().select(JrdsTreeViewTab.HOSTS_TAB);
        Label tabsLabel = new Label("Sorted By:");
        GridPane.setConstraints(tabsLabel, 0, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(4, 0, 4, 0));
        getParamsGridPane().getChildren().addAll(tabsLabel, hBox);
        extraArgumentTextField.visibleProperty().bind(Bindings.createBooleanBinding(() -> this.tabsChoiceBox.valueProperty().get().getArgument() != null, this.tabsChoiceBox.valueProperty()));
    }

    @Override
    protected Collection<DataAdapter> getDataAdapters() throws DataAdapterException {
        return List.of(JrdsDataAdapter.fromUrl(
                getSourceUri(),
                ZoneId.of(getSourceTimezone()),
                this.tabsChoiceBox.getValue(),
                this.extraArgumentTextField.getText()));
    }
}
