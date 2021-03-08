package org.javawebstack.orm.annotation;

import org.javawebstack.orm.Model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface BelongsTo {
    Class<? extends Model> value();
    String name() default "";
}
