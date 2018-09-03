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

import javafx.util.Duration;

import java.util.Arrays;

public enum NotificationDurationChoices {
    FIVE_SECONDS("5 seconds", Duration.seconds(5)),
    TEN_SECONDS("10 seconds", Duration.seconds(10)),
    THIRTY_SECONDS("30 seconds", Duration.seconds(30)),
    NEVER("Never", Duration.INDEFINITE);

    private final String name;
    private final Duration duration;

    NotificationDurationChoices(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return name;
    }

    public static NotificationDurationChoices valueOf(Duration duration) {
        return Arrays.stream(values()).filter(notificationDurationChoices -> notificationDurationChoices.getDuration().equals(duration)).findFirst().orElse(NEVER);
    }
}
