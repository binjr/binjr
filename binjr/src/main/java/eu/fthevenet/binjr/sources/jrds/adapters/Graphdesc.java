package eu.fthevenet.binjr.sources.jrds.adapters;

import javax.xml.bind.annotation.*;
import java.util.List;

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
    @XmlElement(name = "unit")
    String unit;
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
        String name;
        @XmlElement(name = "dsName")
        String dsName;
        @XmlElement(name = "graphType")
        String graphType;
        @XmlElement(name = "color")
        String color;
        @XmlElement(name = "legend")
        String legend;
        @XmlElement(name = "rpn")
        String rpn;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "tree")
    static class Tree {
        @XmlElement(name = "pathstring")
        List<String> pathstring;
    }
}
