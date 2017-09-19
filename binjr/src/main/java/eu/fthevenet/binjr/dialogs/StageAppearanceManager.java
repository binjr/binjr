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

public class StageAppearanceManager {
    public enum Options {
        SET_NONE,
        SET_ICON,
        SET_THEME,
        SET_ALL;

        public long getValue() {
            return 1 << this.ordinal();
        }
    }

    private static final Logger logger = LogManager.getLogger(StageAppearanceManager.class);

    private static class Holder {
        private final static StageAppearanceManager instance = new StageAppearanceManager();
    }

    private final Map<Stage, Set<Options>> registeredStages;

    private StageAppearanceManager() {
        registeredStages = new HashMap<>();
        GlobalPreferences.getInstance().userInterfaceThemeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                for (Map.Entry<Stage, Set<Options>> e : registeredStages.entrySet()) {
                    setAppearance(e.getKey(), newValue, e.getValue());
                }
                logger.trace(() -> "Registered stages=" + registeredStages.size());
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

    public void unregister(Stage stage) {
        if (stage == null) {
            logger.warn(() -> "Trying to unregister a stage with null reference");
            return;
        }
        registeredStages.remove(stage);
    }

    public void register(Stage stage) {
        this.register(stage, Options.SET_ALL);
    }

    public void register(Stage stage, Options... options) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null");
        }
        Set<Options> optionsEnumSet = EnumSet.copyOf(Arrays.asList(options));
        this.registeredStages.put(stage, optionsEnumSet);
        Platform.runLater(() -> setAppearance(stage, GlobalPreferences.getInstance().getUserInterfaceTheme(), optionsEnumSet));
    }

    private void setAppearance(Stage stage, UserInterfaceThemes theme, Set<Options> options) {
        if (options.contains(Options.SET_NONE)) {
            return;
        }
        if (options.contains(Options.SET_ALL) || options.contains(Options.SET_THEME)) {
            setUiTheme(stage.getScene(), theme);
        }
        if (options.contains(Options.SET_ALL) || options.contains(Options.SET_ICON)) {
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
