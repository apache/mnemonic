package com.intel.bigdatamem;

/**
 * this class defines a annotation for non-volatile entity
 *
 */

import java.lang.annotation.*;

@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS)
public @interface NonVolatileEntity {

}
