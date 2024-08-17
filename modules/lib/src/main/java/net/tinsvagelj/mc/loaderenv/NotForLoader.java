package net.tinsvagelj.mc.loaderenv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated element will not be compiled into the resulting binary if any of specified {@link Loader}s are detected at
 * compile time.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface NotForLoader {
    Loader[] value();
}
