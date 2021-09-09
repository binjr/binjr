/*
 *    Copyright 2016-2021 Frederic Thevenet
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

import eu.binjr.common.logging.LoggingOutputStream;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.logging.TextFlowAppender;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.controllers.MainViewController;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import eu.binjr.common.logging.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.awt.*;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The entry point for the application.
 *
 * @author Frederic Thevenet
 */
public class Binjr extends Application {
    public static final Logger runtimeDebuggingFeatures = Logger.create("runtimeDebuggingFeatures");
    private static final Logger logger = Logger.create(Binjr.class);
    public static final TextFlowAppender DEBUG_CONSOLE_APPENDER;

    static {
        // initialize the debug console appender early to start capturing logs ASAP.
        TextFlowAppender textFlowAppender = null;
        try {
            Configurator.setRootLevel(UserPreferences.getInstance().rootLoggingLevel.get());
            UserPreferences.getInstance().rootLoggingLevel.property().addListener((observable, oldLevel, newLevel) -> {
                Configurator.setRootLevel(newLevel);
                logger.info("Root logger level set to " + newLevel);
            });
            if (UserPreferences.getInstance().redirectStdOutToLogs.get()) {
                System.setErr(new PrintStream(new LoggingOutputStream(Logger.create("stderr"), Level.ERROR), true));
                System.setOut(new PrintStream(new LoggingOutputStream(Logger.create("stdout"), Level.DEBUG), true));
            }
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            try {
                Configurator.setLevel("runtimeDebuggingFeatures", Level.DEBUG);
                textFlowAppender = TextFlowAppender.createAppender(
                        "InternalConsole",
                        PatternLayout.newBuilder()
                                .withPattern("[%d{YYYY-MM-dd HH:mm:ss.SSS}] [%-5level] [%t] [%logger{36}] %msg%n")
                                .build(), null);
                textFlowAppender.start();

                loggerContext.getConfiguration().addAppender(textFlowAppender);
                loggerContext.getRootLogger()
                        .addAppender(loggerContext.getConfiguration().getAppender(textFlowAppender.getName()));
                loggerContext.updateLoggers();
            } catch (Exception e) {
                logger.error("Failed to initialize internal console appender", e);
            }
            try {
                if (UserPreferences.getInstance().persistLogsToFile.get()) {
                    Path basePath = UserPreferences.getInstance().logFilesLocation.get()
                            .resolve("binjr_" +
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_")) +
                                    ProcessHandle.current().pid() +
                                    ".log");
                    var fileAppender = FileAppender.newBuilder()
                            .setName("FileAppender")
                            .setLayout(PatternLayout.newBuilder()
                                    .withPattern("[%d{YYYY-MM-dd HH:mm:ss.SSS}] [%-5level] [%t] [%logger{36}] %msg%n")
                                    .build())
                            .withFileName(basePath.toString())
                            .build();
                    fileAppender.start();
                    loggerContext.getRootLogger().addAppender(fileAppender);
                    UserHistory.getInstance().logFilesHistory.push(basePath.toRealPath());
                }
            } catch (Exception e) {
                logger.error("Failed to initialize file appender", e);
            }

        } catch (Throwable t) {
            logger.error("Failed to initialize logging console appender", t);
        }
        DEBUG_CONSOLE_APPENDER = textFlowAppender;
    }

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (AppEnvironment.getInstance().getJavaVersion().getMajor() >= 13) {
            System.setProperty("sun.security.jgss.native", "true");
        }
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        AppEnvironment.getInstance().bindHeapDumpPreferences();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info(() -> "***********************************");
        logger.info(() -> "*  Starting " + AppEnvironment.APP_NAME);
        logger.info(() -> "***********************************");
        AppEnvironment.getInstance().getSysInfoProperties().forEach(logger::info);
        String jaasCfgPath = System.getProperty("java.security.auth.login.config");
        if (jaasCfgPath == null || jaasCfgPath.trim().length() == 0) {
            System.setProperty("java.security.auth.login.config", Binjr.class.getResource("/jaas_login.conf").toExternalForm());
        }
        var env = AppEnvironment.getInstance();
        env.processCommandLineOptions(getParameters());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/eu/binjr/views/MainView.fxml"));
        Parent root = loader.load();
        MainViewController mainViewController = loader.getController();
        mainViewController.setAssociatedFile(env.getAssociatedWorkspace());
        primaryStage.setTitle(AppEnvironment.APP_NAME);

        try (Profiler p = Profiler.start("Set scene", logger::perf)) {
            if (Screen.getScreensForRectangle(
                    UserPreferences.getInstance().windowLastPosition.get().getMinX(),
                    UserPreferences.getInstance().windowLastPosition.get().getMinY(),
                    10, 10).size() > 0) {
                primaryStage.setX(UserPreferences.getInstance().windowLastPosition.get().getMinX());
                primaryStage.setY(UserPreferences.getInstance().windowLastPosition.get().getMinY());
                primaryStage.setWidth(UserPreferences.getInstance().windowLastPosition.get().getWidth());
                primaryStage.setHeight(UserPreferences.getInstance().windowLastPosition.get().getHeight());
            }
            primaryStage.setScene(new Scene(root));
            StageAppearanceManager.getInstance().register(primaryStage);
        }
        try (Profiler p = Profiler.start("show", logger::perf)) {
            primaryStage.initStyle(AppEnvironment.getInstance().getWindowsStyle());
            primaryStage.show();
        }
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }
    }
}
