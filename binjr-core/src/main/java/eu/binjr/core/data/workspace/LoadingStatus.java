/*
 * Copyright 2026 Frederic Thevenet
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

package eu.binjr.core.data.workspace;


public record LoadingStatus(boolean error, String errorMessage, Throwable cause) {

    public static LoadingStatus OK = new LoadingStatus(false, "", null);

    public static LoadingStatus error(String errorMessage, Throwable cause) {
        return new LoadingStatus(true, errorMessage, cause);
    }

    public static LoadingStatus error(Throwable cause) {
        return new LoadingStatus(true, cause.getMessage(), cause);
    }

}
