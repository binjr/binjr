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

package eu.fthevenet.util.javafx.controls;

import javafx.scene.input.DataFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class ReferenceClipboard {
    private final ConcurrentHashMap<DataFormat, Object> clipboard;
    private static final Logger logger = LogManager.getLogger(ReferenceClipboard.class);

    private static class Holder {
        private final static ReferenceClipboard instance = new ReferenceClipboard();
    }

    public static ReferenceClipboard getInstance() {
        return Holder.instance;
    }

    private ReferenceClipboard() {
        clipboard = new ConcurrentHashMap<>();
    }

    public Object put(DataFormat format, Object reference) {
        return this.clipboard.put(format, reference);
    }

    public Object get(DataFormat format) {
        return this.clipboard.get(format);
    }

    public void clear() {
        this.clipboard.clear();
    }

}
