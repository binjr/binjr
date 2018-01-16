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

package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the appearance of registered {@link Stage} instances.
 */
public class StageAppearanceManager {
    /**
     * Defines a set of options that governs that what degree a the appearance of a registered {@link Stage} should be affected
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
         * Indicates that all appearance changes should be applied
         */
        SET_ALL;

        public long getValue() {
            return 1 << this.ordinal();
        }
    }

    private static final Logger logger = LogManager.getLogger(StageAppearanceManager.class);
    private static class Holder {
        private final static StageAppearanceManager instance = new StageAppearanceManager();
    }
    private final Map<Stage, Set<AppearanceOptions>> registeredStages;

    /**
     * Initializes a new instance of the {@link StageAppearanceManager} class.
     */
    private StageAppearanceManager() {
        registeredStages = new WeakHashMap<>();
        GlobalPreferences.getInstance().userInterfaceThemeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                for (Map.Entry<Stage, Set<AppearanceOptions>> e : registeredStages.entrySet()) {
                    setAppearance(e.getKey(), newValue, e.getValue());
                }
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

    /**
     * Registers a {@link Stage} so that its appearance can be altered by the manager.
     *
     * @param stage the {@link Stage} to register in the {@link StageAppearanceManager}
     */
    public void register(Stage stage) {
        this.register(stage, AppearanceOptions.SET_ALL);
    }

    /**
     * Registers a {@link Stage} so that its appearance can be altered by the manager, according to the provided {@link AppearanceOptions}
     *
     * @param stage   the {@link Stage} to register in the {@link StageAppearanceManager}
     * @param options Appearance {@link AppearanceOptions} to apply the the registered stage.
     */
    public void register(Stage stage, AppearanceOptions... options) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        Set<AppearanceOptions> optionsEnumSet = EnumSet.copyOf(Arrays.asList(options));
        this.registeredStages.put(stage, optionsEnumSet);
        logger.trace(this::dumpRegisteredStages);
        Platform.runLater(() -> setAppearance(stage, GlobalPreferences.getInstance().getUserInterfaceTheme(), optionsEnumSet));
    }

    private String dumpRegisteredStages() {
        return registeredStages.size() + " registered stage(s): " +
                registeredStages.keySet()
                        .stream()
                        .map(s -> s.getTitle() + "(" + s.getWidth() + "x" + s.getHeight() + ")")
                        .collect(Collectors.joining(", "));
    }

    private void setAppearance(Stage stage, UserInterfaceThemes theme, Set<AppearanceOptions> options) {
        if (options.contains(AppearanceOptions.SET_NONE)) {
            return;
        }
        if (options.contains(AppearanceOptions.SET_ALL) || options.contains(AppearanceOptions.SET_THEME)) {
            setUiTheme(stage.getScene(), theme);
        }
        if (options.contains(AppearanceOptions.SET_ALL) || options.contains(AppearanceOptions.SET_ICON)) {
            setIcon(stage);
        }
    }

    private void setIcon(Stage stage) {
        stage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/icons/binjr_16.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_32.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_48.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_128.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_256.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_512.png")));
    }

    private void setUiTheme(Scene scene, UserInterfaceThemes theme) {
        Dialogs.runOnFXThread(() -> {
            scene.getStylesheets().clear();
            Application.setUserAgentStylesheet(null);
            scene.getStylesheets().addAll(
                    getClass().getResource("/css/Icons.css").toExternalForm(),
                    getClass().getResource(theme.getCssPath()).toExternalForm());
        });
    }
}
