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

import eu.binjr.common.diagnostic.DiagnosticException;
import eu.binjr.common.diagnostic.HotSpotDiagnostic;
import eu.binjr.common.function.CheckedConsumer;
import eu.binjr.common.logging.LoggingOutputStream;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.logging.TextFlowAppender;
import eu.binjr.common.preferences.Preference;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.controllers.MainViewController;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.action.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.awt.*;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * The entry point fo the application.
 *
 * @author Frederic Thevenet
 */
public class Binjr extends Application {
    public static final Logger runtimeDebuggingFeatures = LogManager.getLogger("runtimeDebuggingFeatures");
    private static final Logger logger = LogManager.getLogger(Binjr.class);
    public static final TextFlowAppender DEBUG_CONSOLE_APPENDER;
    private static final UserPreferences userPrefs = UserPreferences.getInstance();

    static {
        // initialize the debug console appender early to start capturing logs ASAP.
        TextFlowAppender textFlowAppender = null;
        try {
            Configurator.setRootLevel(userPrefs.rootLoggingLevel.get().getLevel());
            userPrefs.rootLoggingLevel.property().addListener((observable, oldLevel, newLevel) -> {
                Configurator.setRootLevel(newLevel.getLevel());
                logger.info("Root logger level set to " + newLevel);
            });
            if (userPrefs.redirectStdOutToLogs.get()) {
                System.setErr(new PrintStream(new LoggingOutputStream(LogManager.getLogger("stderr"), Level.ERROR), true));
                System.setOut(new PrintStream(new LoggingOutputStream(LogManager.getLogger("stdout"), Level.DEBUG), true));
            }
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            if (userPrefs.persistLogsToFile.get()) {
                Path basePath = userPrefs.logPath.get();
                var rollingFileAppender = RollingFileAppender.newBuilder()
                        .setName("RollingFileAppender")
                        .setLayout(PatternLayout.newBuilder()
                                .withPattern("[%d{YYYY-MM-dd HH:mm:ss.SSS}] [%pid] [%-5level] [%t] [%logger{36}] %msg%n")
                                .build())
                        .withFileName(basePath.resolve("binjr.log").toString())
                        .withFilePattern(basePath.resolve("binjr").toString() + "-%d{YYYY-MM-dd_HH-mm-ss}.log.zip")
                        .withPolicy(OnStartupTriggeringPolicy.createPolicy(1))
                        .withStrategy(DefaultRolloverStrategy.newBuilder()
                                .withMax(Integer.toString(userPrefs.rollOverLogFileMax.get().intValue()))
                                .withCustomActions(new Action[]{
                                        DeleteAction.createDeleteAction(
                                                basePath.toString(),
                                                false,
                                                1,
                                                false,
                                                new PathSortByModificationTime(true),
                                                new PathCondition[]{
                                                        IfFileName.createNameCondition(
                                                                null,
                                                                "binjr-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}.log.zip",
                                                                (PathCondition[]) null),
                                                        IfAccumulatedFileCount.createFileCountCondition(
                                                                userPrefs.rollOverLogFileMax.get().intValue())
                                                },
                                                null, loggerContext.getConfiguration())
                                }).build())
                        .build();
                rollingFileAppender.start();
                loggerContext.getRootLogger().addAppender(rollingFileAppender);
            }

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

        } catch (Throwable t) {
            logger.error("Failed to initialize debug console appender", t);
        }
        DEBUG_CONSOLE_APPENDER = textFlowAppender;
    }

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.info(() -> "***********************************");
        logger.info(() -> "*  Starting " + AppEnvironment.APP_NAME);
        logger.info(() -> "***********************************");
        AppEnvironment.getInstance().getSysInfoProperties().forEach(logger::info);
        String jaasCfgPath = System.getProperty("java.security.auth.login.config");
        if (jaasCfgPath == null || jaasCfgPath.trim().length() == 0) {
            System.setProperty("java.security.auth.login.config", Binjr.class.getResource("/jaas_login.conf").toExternalForm());
        }
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        bindPrefToVmOption(userPrefs.heapDumpOnOutOfMemoryError, HotSpotDiagnostic::setHeapDumpOnOutOfMemoryError);
        bindPrefToVmOption(userPrefs.heapDumpPath, HotSpotDiagnostic::setHeapDumpPath);
        launch(args);
    }

    private static <T> void bindPrefToVmOption(Preference<T> pref, CheckedConsumer<T, DiagnosticException> optionSetter) {
        try {
            optionSetter.accept(pref.get());
        } catch (DiagnosticException e) {
            logger.error(e.getMessage(), e);
        }
        pref.property().addListener((val, oldVal, newVal) -> {
            try {
                optionSetter.accept(newVal);
            } catch (DiagnosticException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var env = AppEnvironment.getInstance();
        env.processCommandLineOptions(getParameters());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/eu/binjr/views/MainView.fxml"));
        Parent root = loader.load();
        MainViewController mainViewController = loader.getController();
        mainViewController.setAssociatedFile(env.getAssociatedWorkspace());
        primaryStage.setTitle(AppEnvironment.APP_NAME);

        try (Profiler p = Profiler.start("Set scene", logger::trace)) {
            if (Screen.getScreensForRectangle(
                    userPrefs.windowLastPosition.get().getMinX(),
                    userPrefs.windowLastPosition.get().getMinY(),
                    10, 10).size() > 0) {
                primaryStage.setX(userPrefs.windowLastPosition.get().getMinX());
                primaryStage.setY(userPrefs.windowLastPosition.get().getMinY());
                primaryStage.setWidth(userPrefs.windowLastPosition.get().getWidth());
                primaryStage.setHeight(userPrefs.windowLastPosition.get().getHeight());
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
}
