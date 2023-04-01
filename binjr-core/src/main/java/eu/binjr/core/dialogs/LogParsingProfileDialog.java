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

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.controllers.LogParsingProfilesController;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

public class LogParsingProfileDialog extends Dialog<ParsingProfile> {

    private final DialogPane root;

    public LogParsingProfileDialog(Window owner, ParsingProfile selectedProfile) {
        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/eu/binjr/views/LogParsingProfilesDialogView.fxml"));
        var controller = new LogParsingProfilesController(BuiltInParsingProfile.values(),
                UserPreferences.getInstance().userLogEventsParsingProfiles.get(),
                BuiltInParsingProfile.ALL,
                selectedProfile,
                StandardCharsets.UTF_8,
                ZoneId.systemDefault());
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
            if (!controller.applyChanges()) {
                ae.consume();
            }
        });

        BindingManager manager = new BindingManager();
        var stage = NodeUtils.getStage(root);
        stage.setUserData(manager);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, manager.registerHandler(e -> {
            if (e.getCode() == KeyCode.F1) {
                UserPreferences.getInstance().showInlineHelpButtons.set(!UserPreferences.getInstance().showInlineHelpButtons.get());
            }
            e.consume();
        }));
        this.setOnCloseRequest(event -> manager.registerHandler(e -> manager.close()));

        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        UserPreferences.getInstance().userLogEventsParsingProfiles.set(controller.getCustomProfiles().toArray(ParsingProfile[]::new));
                        return controller.getSelectedProfile();
                    }
                    return null;
                }
        );
    }

}
