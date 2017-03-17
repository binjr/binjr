package eu.fthevenet.binjr.data.adapters;

import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents a binding between a time series and the {@link DataAdapter} used to produce it.
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name="TimeSeriesBinding")
public interface TimeSeriesBinding<T extends Number> {
    /**
     * Returns the label of the binding
     *
     * @return the label of the binding
     */
    String getLabel();

    /**
     * Returns the path of the binding
     *
     * @return the path of the binding
     */
    String getPath();

    /**
     * Returns the {@link DataAdapter} of the binding
     *
     * @return the {@link DataAdapter} of the binding
     */
    @XmlTransient
    DataAdapter<T> getAdapter();

    /**
     * Returns the color of the bound series as defined in the source.
     *
     * @return the color of the bound series as defined in the source.
     */
    Color getColor();

    /**
     * Returns the legend of the bound series as defined in the source.
     *
     * @return the legend of the bound series as defined in the source.
     */
    String getLegend();

    /**
     * Returns the type of graph of the bound series as defined in the source.
     *
     * @return the type of graph of the bound series as defined in the source.
     */
    ChartType getGraphType();

    /**
     * Returns the unit name of the bound series as defined in the source.
     *
     * @return the  unit name of the bound series as defined in the source.
     */
    String getUnitName();

    /**
     * Returns the unit numerical base of the bound series as defined in the source.
     *
     * @return the  unit numerical base of the bound series as defined in the source.
     */
    UnitPrefixes getUnitPrefix();


}
