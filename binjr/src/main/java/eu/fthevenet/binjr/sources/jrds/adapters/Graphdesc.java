package eu.fthevenet.binjr.sources.jrds.adapters;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A POJO class used to deserialize JRDS graph descriptor XML messages return by {@code /graphdesc?id=""} service.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="graphdec")
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
            @XmlElement(name="SI", type=JrdsMetricUnitType.class),
            @XmlElement(name="manager", type=JrdsBinaryUnitType.class)
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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "add")
    static class SeriesDesc {
        @XmlElement(name = "name")
        String name ="";
        @XmlElement(name = "dsName")
        String dsName ="";
        @XmlElement(name = "graphType")
        String graphType ="";
        @XmlElement(name = "color")
        String color ="";
        @XmlElement(name = "legend")
        String legend ="";
        @XmlElement(name = "rpn")
        String rpn ="";

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SeriesDesc{");
            sb.append("name='").append(name).append('\'');
            sb.append(", dsName='").append(dsName).append('\'');
            sb.append(", graphType='").append(graphType).append('\'');
            sb.append(", color='").append(color).append('\'');
            sb.append(", legend='").append(legend).append('\'');
            sb.append(", rpn='").append(rpn).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "tree")
    static class Tree {
        @XmlElement(name = "pathstring")
        List<String> pathstring;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Tree{");
            sb.append("pathstring=").append(pathstring);
            sb.append('}');
            return sb.toString();
        }
    }


   @XmlSeeAlso({JrdsMetricUnitType.class, JrdsBinaryUnitType.class})
    public static abstract class JrdsUnitType{

    }

    @XmlType(name = "SI")
    public static class JrdsMetricUnitType extends JrdsUnitType{
        @Override
        public String toString() {
            return "SI";
        }
    }

    @XmlType(name = "binary")
    public  static class JrdsBinaryUnitType extends JrdsUnitType{
        @Override
        public String toString() {
            return "binary";
        }

    }

//    @XmlType(name = "unit")
//    @XmlEnum
//    public enum JrdsUnitType {
//        @XmlEnumValue("SI")
//        METRIC("SI"),
//        @XmlEnumValue("binary")
//        BINARY("binary");
//
//        private final String value;
//
//        JrdsUnitType(String value){
//            this.value = value;
//        }
//
//        public String value() {
//            return name();
//        }
//
//        public static JrdsUnitType fromValue(String v) {
//            for (JrdsUnitType c: JrdsUnitType.values()) {
//                if (c.value.equals(v)) {
//                    return c;
//                }
//            }
//            throw new IllegalArgumentException(v);
//        }
//    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Graphdesc{");
        sb.append("name='").append(name).append('\'');
        sb.append(", graphName='").append(graphName).append('\'');
        sb.append(", graphClass='").append(graphClass).append('\'');
        sb.append(", graphTitle='").append(graphTitle).append('\'');
        sb.append(", unit='").append(unit).append('\'');
        sb.append(", verticalLabel='").append(verticalLabel).append('\'');
        sb.append(", upperLimit='").append(upperLimit).append('\'');
        sb.append(", lowerLimit='").append(lowerLimit).append('\'');
        sb.append(", logarithmic='").append(logarithmic).append('\'');
        sb.append(", seriesDescList=").append(seriesDescList.stream().map(SeriesDesc::toString).collect(Collectors.joining(";")));
        sb.append(", trees=").append(trees.stream().map(Tree::toString).collect(Collectors.joining(";")));
        sb.append('}');
        return sb.toString();
    }
}
