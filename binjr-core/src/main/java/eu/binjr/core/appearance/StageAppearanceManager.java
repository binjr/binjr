/*
 *    Copyright 2019-2021 Frederic Thevenet
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

package eu.binjr.core.appearance;

import eu.binjr.common.colors.ColorUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the appearance of registered {@link Stage} instances.
 *
 * @author Frederic Thevenet
 */
public class StageAppearanceManager {

    /**
     * Defines a set of options that governs that what degree a the appearance of a registered {@link Stage} should be
     * affected.
     */
    public enum AppearanceOptions {
        /**
         * Indicates that no appearance changes should be applied
         */
        SET_NONE,
        /**
         * Indicates that the icon should be changed.
         */
        SET_ICON,
        /**
         * Indicates that the theme should be changed.
         */
        SET_THEME,
        /**
         * Indicates that "curtains" should be used to hide the stage until the theme is fully applied.
         */
        USE_STAGE_CURTAIN,
        /**
         * Indicates that all appearance changes should be applied
         */
        SET_ALL;

        public long getValue() {
            return 1 << this.ordinal();
        }
    }

    private static final Logger logger = Logger.create(StageAppearanceManager.class);

    private static class Holder {
        private final static StageAppearanceManager instance = new StageAppearanceManager();
    }

    private final Map<Stage, Set<AppearanceOptions>> registeredStages;

    /**
     * Initializes a new instance of the {@link StageAppearanceManager} class.
     */
    private StageAppearanceManager() {
        registeredStages = new WeakHashMap<>();
        UserPreferences.getInstance().userInterfaceTheme.property().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                registeredStages.forEach((stage, options) -> setAppearance(stage, newValue, options));
            }
        });
    }

    /**
     * Get the singleton instance for the {@link StageAppearanceManager} class.
     *
     * @return the singleton instance for the {@link StageAppearanceManager} class.
     */
    public static StageAppearanceManager getInstance() {
        return Holder.instance;
    }

    /**
     * Unregister a {@link Stage} from the {@link StageAppearanceManager}
     *
     * @param stage the {@link Stage} to unregister.
     */
    public void unregister(Stage stage) {
        if (stage == null) {
            logger.warn(() -> "Trying to unregister a stage with null reference");
            return;
        }
        registeredStages.remove(stage);
        logger.trace(this::dumpRegisteredStages);
    }

    private Node installCurtain(Stage stage) {
        if (stage.getScene() != null && stage.getScene().getRoot() instanceof Pane root) {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/eu/binjr/images/avatar_512.png")));
            logo.setFitHeight(256.0);
            logo.setFitWidth(256.0);
            StackPane curtain = new StackPane(logo);
            curtain.setStyle("-fx-background-color: #204656;");
            if (root instanceof DialogPane dlg && dlg.getContent() instanceof Pane pane) {
                pane.getChildren().add(curtain);
            } else {
                root.getChildren().add(curtain);
            }
            AnchorPane.setLeftAnchor(curtain, 0.0);
            AnchorPane.setRightAnchor(curtain, 0.0);
            AnchorPane.setTopAnchor(curtain, 0.0);
            AnchorPane.setBottomAnchor(curtain, 0.0);
            curtain.toFront();
            return curtain;
        }
        return null;
    }

    private void raiseCurtain(Stage stage, Node curtain) {
        if (curtain != null && stage.getScene().getRoot() instanceof Pane root) {
            FadeTransition ft = new FadeTransition(Duration.millis(250), curtain);
            ft.setDelay(Duration.millis(350));
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.play();
            ft.setOnFinished(event -> {
                Pane parent = (Pane) curtain.getParent();
                parent.getChildren().remove(curtain);
            });
        }
    }

    /**
     * Registers a {@link Stage} so that its appearance can be altered by the manager.
     *
     * @param stage the {@link Stage} to register in the {@link StageAppearanceManager}
     */
    public void register(Stage stage) {
        this.register(stage, AppearanceOptions.SET_ALL);
    }

    /**
     * Registers a {@link Stage} so that its appearance can be altered by the manager, according to the provided
     * {@link AppearanceOptions}
     *
     * @param stage   the {@link Stage} to register in the {@link StageAppearanceManager}
     * @param options Appearance {@link AppearanceOptions} to apply the registered stage.
     */
    public void register(Stage stage, AppearanceOptions... options) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        Set<AppearanceOptions> optionsEnumSet = EnumSet.copyOf(Arrays.asList(options));
        if (optionsEnumSet.contains(AppearanceOptions.SET_ALL) ||
                optionsEnumSet.contains(AppearanceOptions.USE_STAGE_CURTAIN)) {
            stage.setOnShown(event -> raiseCurtain(stage, installCurtain(stage)));
        }
        this.registeredStages.put(stage, optionsEnumSet);
        logger.trace(this::dumpRegisteredStages);
        Platform.runLater(() ->
                setAppearance(stage, UserPreferences.getInstance().userInterfaceTheme.get(), optionsEnumSet));
    }

    private String dumpRegisteredStages() {
        return registeredStages.size() + " registered stage(s): " +
                registeredStages.keySet()
                        .stream()
                        .map(s -> s.getTitle() + "(" + s.getWidth() + "x" + s.getHeight() + ")")
                        .collect(Collectors.joining(", "));
    }

    private void setAppearance(Stage stage, String theme, Set<AppearanceOptions> options) {
        if (options.contains(AppearanceOptions.SET_NONE)) {
            return;
        }
        if (options.contains(AppearanceOptions.SET_ALL) || options.contains(AppearanceOptions.SET_THEME)) {
            setUiTheme(stage.getScene(), UserInterfaceThemes.valueOf(theme, BuiltInUserInterfaceThemes.LIGHT));
        }
        if (options.contains(AppearanceOptions.SET_ALL) || options.contains(AppearanceOptions.SET_ICON)) {
            setIcon(stage);
        }
    }

    private void setIcon(Stage stage) {
        stage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_16.png")),
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_32.png")),
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_48.png")),
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_128.png")),
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_256.png")),
                new Image(getClass().getResourceAsStream("/eu/binjr/icons/binjr_512.png")));
    }

    private void setUiTheme(Scene scene, String theme, String... extraCss) {
        setUiTheme(scene, UserInterfaceThemes.valueOf(theme, BuiltInUserInterfaceThemes.LIGHT), extraCss);
    }

    private void setUiTheme(Scene scene, UserInterfaceThemes theme, String... extraCss) {
        Dialogs.runOnFXThread(() -> {
            scene.getStylesheets().clear();
            Application.setUserAgentStylesheet(null);
            scene.getStylesheets().addAll(
                    getClass().getResource(getFontFamilyCssPath()).toExternalForm(),
                    getClass().getResource("/eu/binjr/css/Icons.css").toExternalForm(),
                    theme.getClass().getResource(theme.getCssPath()).toExternalForm(),
                    getClass().getResource("/eu/binjr/css/Common.css").toExternalForm());
            if (extraCss != null && extraCss.length > 0) {
                scene.getStylesheets().addAll(extraCss);
            }
        });
    }

    public static String getFontFamilyCssPath() {
        if (UserPreferences.getInstance().alwaysUseEmbeddedFonts.get()) {
            return "/eu/binjr/css/Fonts-family-embedded.css";
        } else {
            return switch (AppEnvironment.getInstance().getOsFamily()) {
                case WINDOWS -> "/eu/binjr/css/Fonts-family-win.css";
                case LINUX -> "/eu/binjr/css/Fonts-family-linux.css";
                case OSX -> "/eu/binjr/css/Fonts-family-mac.css";
                case UNSUPPORTED -> "/eu/binjr/css/Fonts-family-embedded.css";
            };
        }
    }

    public void applyUiTheme(Scene scene) {
        setUiTheme(scene, UserPreferences.getInstance().userInterfaceTheme.get());
    }

    public void applyExtraCss(String... css) {
        registeredStages.keySet().forEach((stage) -> setUiTheme(
                stage.getScene(),
                UserPreferences.getInstance().userInterfaceTheme.get(),
                css));
    }

}
