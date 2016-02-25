package com.intel.bigdatamem;

/**
 * this class defines an annotation for getter methods of non-volatile entity
 *
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) @Retention(RetentionPolicy.CLASS)
public @interface NonVolatileGetter {
    String EntityFactoryProxies() default "null";
    String GenericFieldTypes() default "null";
}
