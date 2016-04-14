/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mnemonic;

/**
 * a non-volatile annotation processor
 *
 */

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.Diagnostic;

public class DurableEntityProcessor extends AbstractProcessor {
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
    annotataions.add(DurableEntity.class.getCanonicalName());
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
   *          the element in question
   *
   * @param msg
   *          the message issued
   */
  public void error(Element e, String msg) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
  }

  /**
   * triggered if a note issued during processing
   *
   * @param e
   *          the element in question
   *
   * @param msg
   *          the message issued
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

      for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(DurableEntity.class)) {

        String outputstr = String.format("++++++++++%s+++++++++++", annotatedElement.getSimpleName());
        note(annotatedElement, outputstr);
        // System.err.println(outputstr);

        if (annotatedElement.getKind() != ElementKind.CLASS) {
          throw new AnnotationProcessingException(annotatedElement, "Only classes can be annotated with @%s",
              DurableEntity.class.getSimpleName());
        }

        // We can cast it, because we know that it of ElementKind.CLASS
        TypeElement typeelem = (TypeElement) annotatedElement;

        AnnotatedDurableEntityClass annotatedClass = new AnnotatedDurableEntityClass(typeelem, typeUtils,
            elementUtils, messager);

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
