/*
 * Copyright 2023 Frederic Thevenet
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

package eu.binjr.common.io;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AesHelperTests {
    final static String MESSAGE = "This is my message. There are many like it, but this one is mine.";
    final static String PASS_PHRASE = "A pretty bad pass phrase.";

    @Test
    public void aesEncryptDecryptTest() throws GeneralSecurityException {
        String salt  = UUID.randomUUID().toString();
        var encrypted = AesHelper.encrypt(MESSAGE, AesHelper.deriveKeyFromPassword(PASS_PHRASE, salt));
        var decrypted = AesHelper.decrypt(encrypted, AesHelper.deriveKeyFromPassword(PASS_PHRASE, salt));
        assertEquals(decrypted, MESSAGE);
    }
}
