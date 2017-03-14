package eu.fthevenet.binjr.data.dirtyable;

import javafx.beans.property.BooleanProperty;

/**
 * @author Frederic Thevenet
 */
public interface Dirtyable {
    Boolean isDirty();

    BooleanProperty dirtyProperty();

    void cleanUp();
}
