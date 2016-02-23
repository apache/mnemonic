package com.intel.bigdatamem;

import javax.lang.model.element.Element;

/**
 * this exception is used for non-volatile annotation processing 
 *
 */
public class AnnotationProcessingException extends Exception {

    private static final long serialVersionUID = 6911141027622831646L;

    private Element element;

    public AnnotationProcessingException(Element element, String msg, Object... args) {
	super(String.format(msg, args));
	this.element = element;
    }

    public Element getElement() {
	return element;
    }
}
