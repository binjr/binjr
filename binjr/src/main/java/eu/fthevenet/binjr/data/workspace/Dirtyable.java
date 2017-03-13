package eu.fthevenet.binjr.data.workspace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Collection;

/**
 * Created by FTT2 on 13/03/2017.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Dirtyable {

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    private ChangeListener<Object> setDirtyListener = (observable, oldValue, newValue) -> dirty.setValue(true);

    private ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
        if (newValue) {
            forceDirty();
        }
    };

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public  boolean isDirty() {
        return dirty.getValue();
    }

    public void cleanUp() {
        dirty.setValue(false);
    }

    protected void evaluateDirty(Boolean isDirty) {
        this.dirty.setValue(dirty.getValue() | isDirty);
    }

    protected void forceDirty(){
        dirty.setValue(true);
    }

    protected void registerProperties(Property<?>... properties){
        for (Property<?> p :properties){
            p.addListener(setDirtyListener);
        }
    }

    protected void unRegisterProperties(Property<?>... properties){
        for (Property<?> p :properties){
            p.removeListener(setDirtyListener);
        }
    }

    public void addDirtyable(Collection<? extends Dirtyable> dirtyables) {
        if (dirtyables.size() == 0) {
            return;
        }
        for (Dirtyable dirtyable : dirtyables) {
            evaluateDirty(dirtyable.isDirty());
            dirtyable.dirtyProperty().addListener(changeListener);
        }
        forceDirty();
    }

    public void removeDirtyable(Collection<? extends Dirtyable> dirtyables) {
        if (dirtyables.size() == 0) {
            return;
        }
        for (Dirtyable dirtyable : dirtyables) {
            dirtyable.dirtyProperty().removeListener(changeListener);
        }
        forceDirty();
    }


}
