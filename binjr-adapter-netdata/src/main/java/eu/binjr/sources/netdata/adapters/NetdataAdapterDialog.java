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

package eu.binjr.sources.netdata.adapters;

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import javafx.scene.Node;

import java.net.URI;
import java.time.ZoneId;

/**
 * A dialog box that returns a {@link NetdataAdapter} built according to user inputs.
 *
 * @author Frederic Thevenet
 */
public class NetdataAdapterDialog extends DataAdapterDialog<URI> {
    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public NetdataAdapterDialog(Node owner) {
        super(owner, Mode.URI, "mostRecentNetdataUrls");
    }

    @Override
    protected DataAdapter getDataAdapter() throws DataAdapterException {
        return NetdataAdapter.fromUrl(getSourceUri(), ZoneId.of(getSourceTimezone()));
    }
}
