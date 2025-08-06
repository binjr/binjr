/*
 * Copyright 2025 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.colors;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.preferences.UserPreferences;
import javafx.scene.paint.Color;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ColorPalette {
    private final Color[] colors;
    private static final Logger logger = Logger.create(ColorPalette.class);
    private static final ThreadLocal<MessageDigest> threadLocalMessageDigest = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance(UserPreferences.getInstance().colorNamesHashingAlgorithm.get());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to instantiate message digest: Unknown algorithm " +
                    UserPreferences.getInstance().colorNamesHashingAlgorithm.get());
            return null;
        }
    });

    public ColorPalette(Color... colors) {
        this.colors = colors;
    }

    public Color[] getColors() {
        return colors;
    }

    public Color matchEntryToLabel(String label) {
        return colors[matchEntryIndexToLabel(label)];
    }

    public int matchEntryIndexToLabel(String label) {
        long targetNum = getHashValue(label) % colors.length;
        if (targetNum < 0) {
            targetNum = targetNum * -1;
        }
        return (int) targetNum;
    }

    private long getHashValue(final String value) {
        long hashVal;
        var md = threadLocalMessageDigest.get();
        if (md == null) {
            return value.hashCode();
        }
        md.update((value).getBytes(StandardCharsets.UTF_8));
        hashVal = new BigInteger(1, md.digest()).longValue();
        return hashVal;
    }

}
