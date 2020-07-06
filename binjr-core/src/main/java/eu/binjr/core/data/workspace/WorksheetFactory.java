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

package eu.binjr.core.data.workspace;

import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;

import java.lang.reflect.InvocationTargetException;

public class WorksheetFactory {


    public static WorksheetFactory getInstance() {
        return WorksheetFactory.WorksheetFactoryHolder.instance;
    }

    public <T extends Worksheet> T createWorksheet(Class<T> type, String title, BindingsHierarchy... rootItems) throws DataAdapterException {
        try {
            var w = type.getDeclaredConstructor().newInstance();
            if (rootItems != null) {
                w.initWithBindings(title, rootItems);
            }
            return w;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new CannotInitializeDataAdapterException("Could not create instance of Worksheet for " + type.getSimpleName(), e);
        }
    }

    private static class WorksheetFactoryHolder {
        private static final WorksheetFactory instance = new WorksheetFactory();
    }

}
