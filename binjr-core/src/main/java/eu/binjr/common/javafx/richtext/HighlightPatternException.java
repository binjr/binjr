/*
 *    Copyright 2021 Frederic Thevenet
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

package eu.binjr.common.javafx.richtext;

public class HighlightPatternException extends Exception {
    public HighlightPatternException() {
        super("Invalid Pattern for highlight expression");
    }

    public HighlightPatternException(String message) {
        super(message);
    }

    public HighlightPatternException(String message, Throwable cause) {
        super(message, cause);
    }

    public HighlightPatternException(Throwable cause) {
        super("Invalid Pattern for highlight expression", cause);
    }
}
