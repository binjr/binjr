/*
 *    Copyright 2017-2020 Frederic Thevenet
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

package eu.binjr.sources.jrds.adapters;

import jakarta.xml.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A annotated POJO class used to deserialize JRDS graph descriptor XML messages return by {@code /graphdesc?id=""} service.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "graphdec")
class Graphdesc {
    @XmlElement(name = "name")
    String name;
    @XmlElement(name = "graphName")
    String graphName;
    @XmlElement(name = "graphClass")
    String graphClass;
    @XmlElement(name = "graphTitle")
    String graphTitle;
    @XmlElementWrapper(name = "unit")
    @XmlElements({
            @XmlElement(name = "SI", type = JrdsMetricUnitType.class),
            @XmlElement(name = "binary", type = JrdsBinaryUnitType.class)
    })
    List<JrdsUnitType> unit;
    @XmlElement(name = "verticalLabel")
    String verticalLabel;
    @XmlElement(name = "upperLimit")
    String upperLimit;
    @XmlElement(name = "lowerLimit")
    String lowerLimit;
    @XmlElement(name = "logarithmic")
    String logarithmic;
    @XmlElement(name = "add")
    List<SeriesDesc> seriesDescList;
    @XmlElement(name = "tree")
    List<Tree> trees;

    @Override
    public String toString() {
        return "Graphdesc{" + "name='" + name + '\'' +
                ", graphName='" + graphName + '\'' +
                ", graphClass='" + graphClass + '\'' +
                ", graphTitle='" + graphTitle + '\'' +
                ", unit='" + unit + '\'' +
                ", verticalLabel='" + verticalLabel + '\'' +
                ", upperLimit='" + upperLimit + '\'' +
                ", lowerLimit='" + lowerLimit + '\'' +
                ", logarithmic='" + logarithmic + '\'' +
                ", seriesDescList=" + seriesDescList.stream().map(SeriesDesc::toString).collect(Collectors.joining(";")) +
                ", trees=" + trees.stream().map(Tree::toString).collect(Collectors.joining(";")) +
                '}';
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "add")
    static class SeriesDesc {
        @XmlElement(name = "name")
        String name = "";
        @XmlElement(name = "dsName")
        String dsName = "";
        @XmlElement(name = "graphType")
        String graphType = "";
        @XmlElement(name = "color")
        String color = "";
        @XmlElement(name = "legend")
        String legend = "";
        @XmlElement(name = "rpn")
        String rpn = "";

        @Override
        public String toString() {
            return "SeriesDesc{" + "name='" + name + '\'' +
                    ", dsName='" + dsName + '\'' +
                    ", graphType='" + graphType + '\'' +
                    ", color='" + color + '\'' +
                    ", legend='" + legend + '\'' +
                    ", rpn='" + rpn + "'}";
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "tree")
    static class Tree {
        @XmlElement(name = "pathstring")
        List<String> pathstring;

        @Override
        public String toString() {
            return "Tree{" + "pathstring=" + pathstring + '}';
        }
    }

    @XmlSeeAlso({JrdsMetricUnitType.class, JrdsBinaryUnitType.class})
    public static abstract class JrdsUnitType {
    }

    @XmlType(name = "SI")
    public static class JrdsMetricUnitType extends JrdsUnitType {
        @Override
        public String toString() {
            return "SI";
        }
    }

    @XmlType(name = "binary")
    public static class JrdsBinaryUnitType extends JrdsUnitType {
        @Override
        public String toString() {
            return "binary";
        }

    }
}
