package eu.fthevenet.binjr.data.dirtyable;

import javafx.beans.property.BooleanProperty;

/**
 * Classes implementing this interface can have the changes made on member decorated with the
 * {@link IsDirtyable} annotation tracked by an instance of {@link ChangeWatcher}.
 *
 * @author Frederic Thevenet
 */
public interface Dirtyable {
    /**
     * Returns true if the {@link Dirtyable} instance needs to be persisted, false otherwise
     *
     * @return true if the {@link Dirtyable} instance needs to be persisted, false otherwise
     */
    Boolean isDirty();

    /**
     * A {@link BooleanProperty} that observes the changes made to the {@link Dirtyable} instance
     *
     * @return a {@link BooleanProperty} that observes the changes made to the {@link Dirtyable} instance
     */
    BooleanProperty dirtyProperty();

    /**
     * Clear the dirty status of the {@link Dirtyable} instance
     */
    void cleanUp();
}
