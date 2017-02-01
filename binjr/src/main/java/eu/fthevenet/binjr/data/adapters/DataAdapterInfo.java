package eu.fthevenet.binjr.data.adapters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//FIXME not needed for the moment
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataAdapterInfo {
    String name();
    String description() default "";
}
