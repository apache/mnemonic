package com.intel.bigdatamem;

/**
 * a non-volatile annotation processor
 *
 */

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.Diagnostic;

import com.squareup.javapoet.MethodSpec;

public class NonVolatileEntityProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    // private Map<String, FactoryGroupedClasses> factoryClasses = new
    // LinkedHashMap<String, FactoryGroupedClasses>();

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
	super.init(processingEnv);
	typeUtils = processingEnv.getTypeUtils();
	elementUtils = processingEnv.getElementUtils();
	filer = processingEnv.getFiler();
	messager = processingEnv.getMessager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
	Set<String> annotataions = new LinkedHashSet<String>();
	annotataions.add(NonVolatileEntity.class.getCanonicalName());
	return annotataions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
	return SourceVersion.latestSupported();
    }

    /**
     * triggered if an error issued during processing
     *
     * @param e
     *        the element in question
     *
     * @param msg
     *        the message issued
     */
    public void error(Element e, String msg) {
	messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
	
    /**
     * triggered if a note issued during processing
     *
     * @param e
     *        the element in question
     *
     * @param msg
     *        the message issued
     */
    public void note(Element e, String msg) {
	messager.printMessage(Diagnostic.Kind.NOTE, msg, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
	try {

	    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(NonVolatileEntity.class)) {

		String outputstr = String.format("++++++++++%s+++++++++++", annotatedElement.getSimpleName());
		note(annotatedElement, outputstr);
		//				System.err.println(outputstr);
				
		if (annotatedElement.getKind() != ElementKind.CLASS) {
		    throw new AnnotationProcessingException(
							    annotatedElement, "Only classes can be annotated with @%s",
							    NonVolatileEntity.class.getSimpleName());
		}

		// We can cast it, because we know that it of ElementKind.CLASS
		TypeElement typeelem = (TypeElement) annotatedElement;

		AnnotatedNonVolatileEntityClass annotatedClass = 
		    new AnnotatedNonVolatileEntityClass(typeelem, typeUtils, elementUtils, messager);

		annotatedClass.prepareProcessing();
				
		annotatedClass.generateCode(filer);
				
	    }

	} catch (AnnotationProcessingException e) {
	    error(e.getElement(), e.getMessage());
	} catch (IOException e) {
	    error(null, e.getMessage());
	}

	return true;
    }

}
