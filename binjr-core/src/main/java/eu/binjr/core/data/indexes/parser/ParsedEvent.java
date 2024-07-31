/*
 *    Copyright 2020-2022 Frederic Thevenet
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

package eu.binjr.core.data.indexes.parser;

import java.time.ZonedDateTime;
import java.util.Map;

public class ParsedEvent {
    private final ZonedDateTime timestamp;
    private final long lineNumber;
    private final Map<String, String> textFields;
    private final Map<String, Number> numberFields;
    private final String text;

    public static ParsedEvent withTextFields(long sequence, ZonedDateTime timestamp, String text, Map<String, String> textFields) {
        return new ParsedEvent(sequence, timestamp, text, textFields, Map.of());
    }

    public static ParsedEvent withNumberFields(long sequence, ZonedDateTime timestamp, String text, Map<String, Number> numberFields) {
        return new ParsedEvent(sequence, timestamp, text, Map.of(), numberFields);
    }

    public ParsedEvent(long sequence, ZonedDateTime timestamp, String text, Map<String, String> textFields, Map<String, Number> numberFields) {
        this.lineNumber = sequence;
        this.text = text;
        this.timestamp = timestamp;
        this.textFields = textFields;
        this.numberFields = numberFields;
    }

    public long getSequence() {
        return lineNumber;
    }

    public String getText() {
        return text;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getTextFields() {
        return textFields;
    }

    public String getTextField(String key){
        return textFields.get(key);
    }

    public Map<String, Number> getNumberFields() {
        return numberFields;
    }

    public Number getNumberField(String key){
        return numberFields.get(key);
    }

    @Override
    public String toString() {
        return "ParsedEvent{" + "timestamp=" + timestamp +
                ", textFields=" + textFields +
                ", numberFields=" + numberFields +
                ", text='" + text + '\'' +
                '}';
    }
}
