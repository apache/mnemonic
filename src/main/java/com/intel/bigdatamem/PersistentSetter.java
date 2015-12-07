package com.intel.bigdatamem;

/**
 *
 * @author Wang, Gang {@literal <gang1.wang@intel.com>}
 *
 */


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) @Retention(RetentionPolicy.CLASS)
public @interface PersistentSetter {

}
