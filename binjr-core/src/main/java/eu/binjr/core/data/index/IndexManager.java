/*
 *    Copyright 2020 Frederic Thevenet
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

/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.core.data.index;

import eu.binjr.common.concurrent.CloseableResourceManager;
import eu.binjr.common.function.CheckedLambdas;


import java.io.IOException;

public enum IndexManager {
    LOG_INDEX("log_index");

    private final CloseableResourceManager<LogFileIndex> indexManager;
    private final String key;

    IndexManager(String key) {
        this.indexManager = new CloseableResourceManager<>();
        this.key = key;
    }

    public LogFileIndex acquire() throws IOException {
        return indexManager.acquire(key, CheckedLambdas.wrap(LogFileIndex::new));
    }

    public int release() throws Exception {
        return indexManager.release(key);
    }

    public LogFileIndex get(){
        return indexManager.get(key);
    }

}
