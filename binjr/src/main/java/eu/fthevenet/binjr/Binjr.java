/*
 *    Copyright 2017 Frederic Thevenet
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

package eu.fthevenet.binjr;

import eu.fthevenet.binjr.controllers.MainViewController;
import eu.fthevenet.binjr.dialogs.StageAppearanceManager;
import eu.fthevenet.util.logging.Profiler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

/**
 * The entry point fo the application.
 *
 * @author Frederic Thevenet
 */
public class Binjr extends Application {
    private static final Logger logger = LogManager.getLogger(Binjr.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info(() -> "Starting binjr");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
        Parent root = loader.load();
        MainViewController mainViewController = loader.getController();
        mainViewController.setParameters(getParameters());
        primaryStage.setTitle("binjr");
        StageAppearanceManager.getInstance().register(primaryStage);
        try (Profiler p = Profiler.start("Set scene", logger::trace)) {
            primaryStage.setScene(new Scene(root));
        }
        try (Profiler p = Profiler.start("show", logger::trace)) {
            primaryStage.show();
        }
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }
    }

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String jaasCfgPath = System.getProperty("java.security.auth.login.config");
        if (jaasCfgPath == null || jaasCfgPath.trim().length() == 0) {
            System.setProperty("java.security.auth.login.config", Binjr.class.getResource("/jaas_login.conf").toExternalForm());
        }
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        // Disabling accessibility support to work around hanging issue on Windows 10
        // see "https://bugs.openjdk.java.net/browse/JDK-8132897"
        System.setProperty("glass.accessible.force", "false");
        launch(args);
    }
}
