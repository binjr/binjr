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

import java.security.NoSuchAlgorithmException;

public class IOUtilsTest {
    @Test
    public void sha256sumTest() throws NoSuchAlgorithmException {
        System.out.println("IOUtils.sha256sum(\"foobar\") = " + IOUtils.sha256("foobar"));
    }
}
