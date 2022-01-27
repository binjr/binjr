/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import org.reflections.Reflections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public enum ReflectionHelper {
    INSTANCE;


    public static final String EU_BINJR = "eu.binjr";
    private final Set<Class<?>> classesToBeBound = new HashSet<>();

    ReflectionHelper() {
        // Add Workspace class
        classesToBeBound.add(Workspace.class);
        // scan classpath
        scan(new Reflections(EU_BINJR));
    }

    public Collection<Class<?>> getClassesToBeBound() {
        return classesToBeBound;
    }

    public void scanClassLoader(ClassLoader cl) {
        // scan
        scan(new Reflections(EU_BINJR, cl));
    }

    private void scan(Reflections reflections) {
        // Add all Worksheet classes
        classesToBeBound.addAll(reflections.getSubTypesOf(Worksheet.class));
        // Add SourceBinding classes
        classesToBeBound.addAll(reflections.getSubTypesOf(SourceBinding.class));
        // Add TimeSeriesInfo classes
        classesToBeBound.addAll(reflections.getSubTypesOf(TimeSeriesInfo.class));
    }

}
