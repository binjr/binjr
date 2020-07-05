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

import eu.binjr.core.data.adapters.SourceBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

enum ReflectionHelper {
    INSTANCE;
    public Class<?>[] getClassesToBeBound() {
        return classesToBeBound;
    }


    private final Class<?>[] classesToBeBound;

    ReflectionHelper(){
        Reflections reflections = new Reflections("eu.binjr");
        // Add all Worksheet classes
        Set<Class<?>> classes = new HashSet<>(reflections.getSubTypesOf(Worksheet.class));
        // Add SourceBinding classes
        classes.addAll(reflections.getSubTypesOf(SourceBinding.class));
        // Add Workspace class
        classes.add(Workspace.class);
        classesToBeBound =  classes.toArray(Class<?>[]::new);
    }

}
