package com.intel.bigdatamem;

/**
 * this class defines an annotation for setter methods of non-volatile entity
 *
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) @Retention(RetentionPolicy.CLASS)
public @interface NonVolatileSetter {

}
