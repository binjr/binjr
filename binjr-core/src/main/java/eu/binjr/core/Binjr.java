/*
 *    Copyright 2016-2019 Frederic Thevenet
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

package eu.binjr.core;

import eu.binjr.common.logging.Profiler;
import eu.binjr.common.logging.TextFlowAppender;
import eu.binjr.core.controllers.MainViewController;
import eu.binjr.core.dialogs.StageAppearanceManager;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.GlobalPreferences;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * The entry point fo the application.
 *
 * @author Frederic Thevenet
 */
public class Binjr extends Application {
    public static final Logger runtimeDebuggingFeatures = LogManager.getLogger("runtimeDebuggingFeatures");
    private static final Logger logger = LogManager.getLogger(Binjr.class);
    // initialize the debug console appender early to start capturing logs ASAP.
    public static final TextFlowAppender DEBUG_CONSOLE_APPENDER = initTextFlowAppender();

    @Override
    public void start(Stage primaryStage) throws Exception {
        var prefs = GlobalPreferences.getInstance();
        processCommandLineOptions(getParameters());
        logger.info(() -> "Starting " + AppEnvironment.APP_NAME);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/eu/binjr/views/MainView.fxml"));
        Parent root = loader.load();
        MainViewController mainViewController = loader.getController();
        mainViewController.setAssociatedFile(getAssociatedWorkspace(getParameters()));
        primaryStage.setTitle(AppEnvironment.APP_NAME);

        try (Profiler p = Profiler.start("Set scene", logger::trace)) {
            if (Screen.getScreensForRectangle(
                    prefs.getWindowLastPosition().getMinX(),
                    prefs.getWindowLastPosition().getMinY(),
                    10, 10).size() > 0) {
                primaryStage.setX(prefs.getWindowLastPosition().getMinX());
                primaryStage.setY(prefs.getWindowLastPosition().getMinY());
                primaryStage.setWidth(prefs.getWindowLastPosition().getWidth());
                primaryStage.setHeight(prefs.getWindowLastPosition().getHeight());
            }
            primaryStage.setScene(new Scene(root));
            StageAppearanceManager.getInstance().register(primaryStage);
        }
        try (Profiler p = Profiler.start("show", logger::trace)) {
            primaryStage.initStyle(AppEnvironment.getInstance().getWindowsStyle());
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
        launch(args);
    }

    private static TextFlowAppender initTextFlowAppender() {
        try {
            Configurator.setLevel("runtimeDebuggingFeatures", Level.DEBUG);
            TextFlowAppender appender = TextFlowAppender.createAppender(
                    "InternalConsole",
                    PatternLayout.newBuilder().withPattern("[%d{YYYY-MM-dd HH:mm:ss.SSS}] [%-5level] [%t] [%logger{36}] %msg%n").build(),
                    null
            );
            appender.start();
            LoggerContext lc = (LoggerContext) LogManager.getContext(false);
            lc.getConfiguration().addAppender(appender);
            lc.getRootLogger().addAppender(lc.getConfiguration().getAppender(appender.getName()));
            lc.updateLoggers();
            return appender;
        } catch (Throwable t) {
            logger.error("Failed to initialize debug console appender", t);
        }
        return null;
    }

    private void processCommandLineOptions(Parameters parameters) {
        parameters.getNamed().forEach((name, val) -> {
            switch (name.toLowerCase()) {
                case "windows-style":
                    try {
                        AppEnvironment.getInstance().setWindowsStyle(StageStyle.valueOf(val.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.error("Unknown windows style specified: " + val, e);
                    }
                case "resizable-dialogs":
                    AppEnvironment.getInstance().setResizableDialogs(Boolean.valueOf(val));
                    break;
                case "disable-update-check":
                    AppEnvironment.getInstance().setDisableUpdateCheck(Boolean.valueOf(val));
                    break;
                case "log-level":
                    AppEnvironment.getInstance().setLogLevel(Level.toLevel(val, Level.INFO));
                    break;
                case "log-file":
                    break;
            }
        });
    }

    private Optional<String> getAssociatedWorkspace(Parameters parameters) {
        return parameters.getUnnamed()
                .stream()
                .filter(s -> s.endsWith(".bjr"))
                .filter(s -> Files.exists(Paths.get(s)))
                .findFirst();
    }


}
