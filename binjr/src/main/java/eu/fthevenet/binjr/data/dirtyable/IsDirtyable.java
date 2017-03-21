package eu.fthevenet.binjr.data.dirtyable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicated members of classes implementing {@link Dirtyable} are tracked for changes.
 *
 * @author Frederic Thevenet
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsDirtyable {
}
