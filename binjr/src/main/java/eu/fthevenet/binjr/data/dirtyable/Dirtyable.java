package eu.fthevenet.binjr.data.dirtyable;

import javafx.beans.property.BooleanProperty;

/**
 * Classes implementing this interface can have the changes made on member decorated with the
 * {@link IsDirtyable} annotation tracked by an instance of {@link ChangeWatcher}.
 *
 * @author Frederic Thevenet
 */
public interface Dirtyable {
    Boolean isDirty();

    BooleanProperty dirtyProperty();

    void cleanUp();
}
