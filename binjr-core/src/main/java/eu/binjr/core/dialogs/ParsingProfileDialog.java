/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.core.dialogs;

import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.controllers.ParsingProfilesController;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.preferences.UserPreferences;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

public class ParsingProfileDialog extends Dialog<ParsingProfile> {

    private final DialogPane root;

    public ParsingProfileDialog(Window owner, ParsingProfile selectedProfile) {
        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/eu/binjr/views/ParsingProfilesDialogView.fxml"));
        var controller = new ParsingProfilesController(UserPreferences.getInstance().userParsingProfiles.get(), selectedProfile);
        fXMLLoader.setController(controller);

        try {
            root = fXMLLoader.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load " + fXMLLoader.getLocation());
        }

        this.setTitle("Edit Parsing Profile");
        this.setDialogPane(root);
        this.setResizable(true);
        this.initOwner(owner);
        this.initStyle(StageStyle.UTILITY);
        this.initModality(Modality.APPLICATION_MODAL);
        StageAppearanceManager.getInstance().register(NodeUtils.getStage(root),
                StageAppearanceManager.AppearanceOptions.SET_ICON,
                StageAppearanceManager.AppearanceOptions.SET_THEME);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            if (!controller.applyChanges()){
                ae.consume();
            }
        });

        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        UserPreferences.getInstance().userParsingProfiles.set(controller.getCustomProfiles());
                        return controller.getSelectedProfile();
                    }
                    return null;
                }
        );
    }

}
