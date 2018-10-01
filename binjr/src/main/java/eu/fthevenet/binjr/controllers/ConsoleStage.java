/*
 *    Copyright 2018 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.dialogs.StageAppearanceManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ConsoleStage {
    private static final Logger logger = LogManager.getLogger(ConsoleStage.class);
    private final Stage stage;

    private static class ConsoleStageHolder {
        private final static ConsoleStage instance = new ConsoleStage();
    }

    private ConsoleStage() {
        FXMLLoader loader = new FXMLLoader(OutputConsoleController.class.getResource("/views/OutputConsoleView.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            logger.error("Failed to show console windows", e.getMessage());
            logger.debug(() -> "Exception stack", e);
        }
        OutputConsoleController controller = loader.getController();
        final Scene scene = new Scene(root, 800, 300);
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("binjr console output");
        StageAppearanceManager.getInstance().register(stage);
        stage.setAlwaysOnTop(true);
        stage.initStyle(StageStyle.UTILITY);
        stage.show();
        stage.setOnCloseRequest(event -> {
            StageAppearanceManager.getInstance().unregister(stage);
        });
    }


    public static void show() {
        ConsoleStageHolder.instance.stage.show();
    }

    public static void hide() {
        ConsoleStageHolder.instance.stage.hide();
    }
}
