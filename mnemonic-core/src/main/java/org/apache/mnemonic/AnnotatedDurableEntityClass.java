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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import org.apache.mnemonic.resgc.ReclaimContext;

/**
 * this class managed to generate generic durable concrete object and their
 * corresponding factory.
 *
 */
@SuppressWarnings("restriction")
public class AnnotatedDurableEntityClass {
  protected class MethodInfo {
    public ExecutableElement elem;
    public MethodSpec.Builder specbuilder;
    public TypeName rettype;
  }

  protected class FieldInfo {
    public FieldSpec.Builder specbuilder;
    public TypeName type;
    public long id;
    public String name;
    public String efproxiesname;
    public String gftypesname;
    public long fieldoff;
    public long fieldsize;
    public boolean refbreak;
  }

  protected final String cFACTORYNAMESUFFIX = "Factory";
  protected final String cPMEMNAMEPREFIX = "Durable_";
  protected final String cFIELDNAMESUFFIX = String.format("_field_%s", Utils.genRandomString());
  protected final String cALLOCATORFIELDNAME = String.format("alloc_%s", Utils.genRandomString());
  protected final String cAUTORECLAIMFIELDNAME = String.format("autoreclaim_%s", Utils.genRandomString());
  protected final String cUNSAFEFIELDNAME = String.format("unsafe_%s", Utils.genRandomString());
  protected final String cHOLDERFIELDNAME = String.format("holder_%s", Utils.genRandomString());
  protected final String cALLOCTYPENAME = String.format("ALLOC_PMem3C93D24F59");

  private Types m_typeutils;
  private Elements m_elemutils;
  private TypeElement m_elem;

  private String m_factoryname;
  private String m_entityname;

  private long m_holdersize;

  private String m_packagename;

  private TypeName m_alloctypename = TypeVariableName.get(cALLOCTYPENAME);
  private TypeName m_factoryproxystypename = TypeName.get(EntityFactoryProxy[].class);
  private TypeName m_gfieldstypename = TypeName.get(DurableType[].class);
  private TypeVariableName m_alloctypevarname = TypeVariableName.get(cALLOCTYPENAME,
      ParameterizedTypeName.get(ClassName.get(RestorableAllocator.class), m_alloctypename));
  private TypeName m_parameterholder = ParameterizedTypeName.get(ClassName.get(ParameterHolder.class), m_alloctypename);
  private TypeName m_reclaimctxtypename = TypeName.get(ReclaimContext.class);

  private Map<String, MethodInfo> m_gettersinfo = new HashMap<String, MethodInfo>();
  private Map<String, MethodInfo> m_settersinfo = new HashMap<String, MethodInfo>();
  private Map<String, FieldInfo> m_dynfieldsinfo = new HashMap<String, FieldInfo>();
  private Map<String, FieldInfo> m_fieldsinfo = new HashMap<String, FieldInfo>();

  private Map<String, List<MethodInfo>> m_durablemtdinfo = new HashMap<String, List<MethodInfo>>();
  private Map<String, MethodInfo> m_entitymtdinfo = new HashMap<String, MethodInfo>();
  private Map<String, MethodInfo> m_extramtdinfo = new HashMap<String, MethodInfo>();

  private long computeTypeSize(TypeName tname) throws AnnotationProcessingException {
    long ret = 0L;
    if (isUnboxPrimitive(tname)) {
      TypeName tn = unboxTypeName(tname);
      if (tn.equals(TypeName.BOOLEAN)) {
        ret = 1L;
      }
      if (tn.equals(TypeName.BYTE)) {
        ret = 1L;
      }
      if (tn.equals(TypeName.CHAR)) {
        ret = 2L;
      }
      if (tn.equals(TypeName.DOUBLE)) {
        ret = 8L;
      }
      if (tn.equals(TypeName.FLOAT)) {
        ret = 4L;
      }
      if (tn.equals(TypeName.INT)) {
        ret = 4L;
      }
      if (tn.equals(TypeName.LONG)) {
        ret = 8L;
      }
      if (tn.equals(TypeName.SHORT)) {
        ret = 2L;
      }
    } else {
      ret = 8L;
    }
    if (0L == ret) {
      throw new AnnotationProcessingException(null, "%s is not supported for type names", tname.toString());
    }
    return ret;
  }

  private boolean isUnboxPrimitive(TypeName tn) {
    TypeName n = tn;
    try {
      n = tn.unbox();
    } catch (UnsupportedOperationException ex) {
    }
    return n.isPrimitive();
  }

  private TypeName unboxTypeName(TypeName tn) {
    TypeName n = tn;
    try {
      n = tn.unbox();
    } catch (UnsupportedOperationException ex) {
    }
    return n;
  }

  public AnnotatedDurableEntityClass(TypeElement classElement, Types typeUtils, Elements elementUtils,
      Messager messager) {
    m_elem = classElement;
    m_typeutils = typeUtils;
    m_elemutils = elementUtils;

    m_packagename = m_elemutils.getPackageOf(m_elem).getQualifiedName().toString();

    m_factoryname = String.format("%s%s", m_elem.getSimpleName(), cFACTORYNAMESUFFIX);
    m_entityname = String.format("%s%s_%s", cPMEMNAMEPREFIX, m_elem.getSimpleName(), Utils.genRandomString());

    m_durablemtdinfo.put("cancelAutoReclaim", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("registerAutoReclaim", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("getHandler", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("autoReclaim", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("destroy", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("syncToVolatileMemory", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("syncToNonVolatileMemory", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("syncToLocal", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("getNativeFieldInfo", new ArrayList<MethodInfo>());
    m_durablemtdinfo.put("refbreak", new ArrayList<MethodInfo>());

    m_entitymtdinfo.put("initializeDurableEntity", new MethodInfo());
    m_entitymtdinfo.put("createDurableEntity", new MethodInfo());
    m_entitymtdinfo.put("restoreDurableEntity", new MethodInfo());

    m_extramtdinfo.put("getNativeFieldInfo_static", new MethodInfo());

  }

  public void prepareProcessing() throws AnnotationProcessingException {
    MethodInfo methodinfo = null;
    FieldInfo fieldinfo;
    String methodname;
    long fieldoff = 0;
    AnnotationSpec unsafeannotation = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "{$S,$S}", "restriction", "UseOfSunClasses").build();
    TypeElement intf_durable = m_elemutils.getTypeElement(Durable.class.getCanonicalName());
    TypeElement intf_entity = m_elemutils.getTypeElement(MemoryDurableEntity.class.getCanonicalName());
    // System.err.printf("<><><><><> %s ======\n", intf_entity.toString());

    boolean valid = false;
    for (TypeMirror tm : m_elem.getInterfaces()) {
      if (tm.toString().equals(Durable.class.getCanonicalName())) {
        valid = true;
        break;
      }
    }
    if (!valid) {
      throw new AnnotationProcessingException(m_elem, "Not implemented Durable Interface by %s.",
          m_elem.getSimpleName().toString());
    }

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_unsafe_%s", Utils.genRandomString());
    // fieldinfo.type = TypeName.get(m_elemutils.getTypeElement(sun.misc.Unsafe.class.getCanonicalName()).asType());
    fieldinfo.type = ClassName.get("sun.misc", "Unsafe");
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE)
                                     .addAnnotation(unsafeannotation);
    m_fieldsinfo.put("unsafe", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_holder_%s", Utils.genRandomString());
    fieldinfo.type = ParameterizedTypeName.get(ClassName.get(DurableChunk.class), m_alloctypename);
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE);
    m_fieldsinfo.put("holder", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_autoreclaim_%s", Utils.genRandomString());
    fieldinfo.type = TypeName.get(m_typeutils.getPrimitiveType(TypeKind.BOOLEAN));
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE, Modifier.VOLATILE);
    m_fieldsinfo.put("autoreclaim", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_reclaimcontext_%s", Utils.genRandomString());
    fieldinfo.type = m_reclaimctxtypename;
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE, Modifier.VOLATILE);
    m_fieldsinfo.put("reclaimcontext", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_allocator_%s", Utils.genRandomString());
    fieldinfo.type = m_alloctypename;
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE);
    m_fieldsinfo.put("allocator", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_factoryproxy_%s", Utils.genRandomString());
    fieldinfo.type = m_factoryproxystypename;
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE);
    m_fieldsinfo.put("factoryproxy", fieldinfo);

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_genericfield_%s", Utils.genRandomString());
    fieldinfo.type = m_gfieldstypename;
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE);
    m_fieldsinfo.put("genericfield", fieldinfo);

    for (Element elem : m_elem.getEnclosedElements()) {
      if (elem.getKind() == ElementKind.METHOD) {
        methodname = elem.getSimpleName().toString();
        // System.err.printf("=========== %s ======\n", methodname);
        DurableGetter pgetter = elem.getAnnotation(DurableGetter.class);
        if (pgetter != null) {
          if (!elem.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessingException(elem, "%s annotated with DurableGetter is not abstract.",
                methodname);
          }
          if (null != elem.getAnnotation(DurableSetter.class)) {
            throw new AnnotationProcessingException(elem, "%s is annotated with DurableSetter as well.",
                methodname);
          }
          if (!methodname.startsWith("get")) {
            throw new AnnotationProcessingException(elem, "%s does not comply name convention of getter.", methodname);
          }
          methodinfo = new MethodInfo();
          methodinfo.elem = (ExecutableElement) elem;
          methodinfo.specbuilder = MethodSpec.overriding(methodinfo.elem);
          methodinfo.rettype = TypeName.get(methodinfo.elem.getReturnType());
          m_gettersinfo.put(methodname.substring(3), methodinfo);
          fieldinfo = new FieldInfo();
          fieldinfo.type = methodinfo.rettype;
          if (fieldinfo.type.toString().equals(String.class.getCanonicalName())) {
            fieldinfo.type = ParameterizedTypeName.get(ClassName.get(MemBufferHolder.class), m_alloctypename);
          }
          if (fieldinfo.type instanceof TypeVariableName) {
            fieldinfo.type = ParameterizedTypeName.get(ClassName.get(GenericField.class), m_alloctypename,
                fieldinfo.type);
          }
          fieldinfo.name = String.format("m_%s_%s", methodname.substring(3).toLowerCase(), Utils.genRandomString());
          fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE);
          fieldinfo.fieldsize = computeTypeSize(TypeName.get(methodinfo.elem.getReturnType()));
          fieldinfo.fieldoff = fieldoff;
          fieldoff += fieldinfo.fieldsize;
          fieldinfo.efproxiesname = pgetter.EntityFactoryProxies();
          fieldinfo.gftypesname = pgetter.GenericFieldTypes();
          fieldinfo.id = pgetter.Id();
          fieldinfo.refbreak = elem.getAnnotation(RefBreak.class) != null;
          m_dynfieldsinfo.put(methodname.substring(3), fieldinfo);

        }
        if (null != elem.getAnnotation(DurableSetter.class)) {
          if (!elem.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessingException(elem, "%s annotated with DurableSetter is not abstract.",
                methodname);
          }
          if (!methodname.startsWith("set")) {
            throw new AnnotationProcessingException(elem, "%s does not comply name convention of setter.", methodname);
          }
          methodinfo = new MethodInfo();
          methodinfo.elem = (ExecutableElement) elem;
          methodinfo.specbuilder = MethodSpec.overriding(methodinfo.elem);
          m_settersinfo.put(methodname.substring(3), methodinfo);
        }
      }
    }

    m_holdersize = fieldoff;

    // MethodInfo minfo = null;
    for (String name : m_settersinfo.keySet()) {
      if (!m_gettersinfo.containsKey(name)) {
        throw new AnnotationProcessingException(null, "%s has no getter.", name);
      }
    }

    for (Element elem : intf_durable.getEnclosedElements()) {
      if (elem.getKind() == ElementKind.METHOD) {
        methodname = elem.getSimpleName().toString();
        if (m_durablemtdinfo.containsKey(methodname)) {
          // System.err.printf("**++++++++++ %s ======\n", methodname);
          methodinfo = new MethodInfo();
          m_durablemtdinfo.get(methodname).add(methodinfo);
          methodinfo.elem = (ExecutableElement) elem;
          methodinfo.specbuilder = MethodSpec.overriding(methodinfo.elem);
        }
      }
    }

    for (Element elem : intf_entity.getEnclosedElements()) {
      if (elem.getKind() == ElementKind.METHOD) {
        methodname = elem.getSimpleName().toString();
        if (m_entitymtdinfo.containsKey(methodname)) {
          // System.err.printf("**------- %s ======\n", elem.toString());
          methodinfo = m_entitymtdinfo.get(methodname);
          methodinfo.elem = (ExecutableElement) elem;
          methodinfo.specbuilder = overriding(methodinfo.elem, cALLOCTYPENAME);

        }
      }
    }

    if (m_extramtdinfo.containsKey("getNativeFieldInfo_static")) {
      methodinfo = m_extramtdinfo.get("getNativeFieldInfo_static");
      assert null != methodinfo;
      assert m_durablemtdinfo.containsKey("getNativeFieldInfo");
      MethodInfo mi = m_durablemtdinfo.get("getNativeFieldInfo").get(0);
      assert null != mi;
      methodinfo.elem = mi.elem;
      methodinfo.rettype = TypeName.get(mi.elem.getReturnType());
      methodinfo.specbuilder = createFrom(mi.elem, "getNativeFieldInfo_static")
              .addModifiers(Modifier.STATIC);
    }

    genNFieldInfo();
  }

  public static Builder createFrom(ExecutableElement method, String methodName) {

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)
            || modifiers.contains(Modifier.FINAL)
            || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
      methodBuilder.addAnnotation(annotationSpec);
    }

    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    methodBuilder.returns(TypeName.get(method.getReturnType()));

    List<? extends VariableElement> parameters = method.getParameters();
    for (VariableElement parameter : parameters) {
      TypeName type = TypeName.get(parameter.asType());
      String name = parameter.getSimpleName().toString();
      Set<Modifier> parameterModifiers = parameter.getModifiers();
      ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
              .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
      for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
        parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
      }
      methodBuilder.addParameter(parameterBuilder.build());
    }
    methodBuilder.varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    return methodBuilder;
  }

  protected String transTypeToUnsafeMethod(TypeName tname, boolean isget) throws AnnotationProcessingException {
    String ret = null;
    if (isUnboxPrimitive(tname)) {
      TypeName tn = unboxTypeName(tname);
      if (tn.equals(TypeName.BOOLEAN)) {
        ret = isget ? "getByte" : "putByte";
      }
      if (tn.equals(TypeName.BYTE)) {
        ret = isget ? "getByte" : "putByte";
      }
      if (tn.equals(TypeName.CHAR)) {
        ret = isget ? "getChar" : "putChar";
      }
      if (tn.equals(TypeName.DOUBLE)) {
        ret = isget ? "getDouble" : "putDouble";
      }
      if (tn.equals(TypeName.FLOAT)) {
        ret = isget ? "getFloat" : "putFloat";
      }
      if (tn.equals(TypeName.INT)) {
        ret = isget ? "getInt" : "putInt";
      }
      if (tn.equals(TypeName.LONG)) {
        ret = isget ? "getLong" : "putLong";
      }
      if (tn.equals(TypeName.SHORT)) {
        ret = isget ? "getShort" : "putShort";
      }
    } else {
      ret = isget ? "getAddress" : "putAddress";
    }
    if (null == ret) {
      throw new AnnotationProcessingException(null, "%s is not supported by getters or setters.", tname.toString());
    }
    return ret;
  }

  protected String getIntialValueLiteral(TypeName tname) throws AnnotationProcessingException {
    String ret = null;
    if (isUnboxPrimitive(tname)) {
      TypeName tn = unboxTypeName(tname);
      if (tn.equals(TypeName.BOOLEAN)) {
        ret = "false";
      }
      if (tn.equals(TypeName.BYTE)) {
        ret = "(byte)0";
      }
      if (tn.equals(TypeName.CHAR)) {
        ret = "(char)0";
      }
      if (tn.equals(TypeName.DOUBLE)) {
        ret = "(double)0.0";
      }
      if (tn.equals(TypeName.FLOAT)) {
        ret = "(float)0.0";
      }
      if (tn.equals(TypeName.INT)) {
        ret = "(int)0";
      }
      if (tn.equals(TypeName.LONG)) {
        ret = "(long)0";
      }
      if (tn.equals(TypeName.SHORT)) {
        ret = "(short)0";
      }
    } else {
      ret = null;
    }
    if (null == ret) {
      throw new AnnotationProcessingException(null, "%s is not supported to determine the inital value.",
          tname.toString());
    }
    return ret;
  }

  private int getFactoryProxyIndex(TypeName gtname) throws AnnotationProcessingException {
    int ret = -1;
    boolean found = false;
    if (gtname instanceof TypeVariableName) {
      for (TypeParameterElement tpe : m_elem.getTypeParameters()) {
        ++ret;
        if (tpe.toString().equals(gtname.toString())) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new AnnotationProcessingException(null, "%s type is not found during factory proxy query.",
            gtname.toString());
      }
    } else {
      throw new AnnotationProcessingException(null, "%s type is not generic type for factory proxy query.",
          gtname.toString());
    }
    return ret;
  }

  protected void genNFieldInfo() {
    FieldInfo dynfieldinfo, fieldinfo;
    List<long[]> finfo = new ArrayList<long[]>();
    for (String name : m_gettersinfo.keySet()) {
      dynfieldinfo = m_dynfieldsinfo.get(name);
      if (dynfieldinfo.id > 0) {
        finfo.add(new long[]{dynfieldinfo.id, dynfieldinfo.fieldoff, dynfieldinfo.fieldsize});
      }
    }

    fieldinfo = new FieldInfo();
    fieldinfo.name = String.format("m_nfieldinfo_%s", Utils.genRandomString());
    fieldinfo.type = ArrayTypeName.of(ArrayTypeName.of(TypeName.LONG));
    String initlstr = Utils.toInitLiteral(finfo);
    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type, fieldinfo.name, Modifier.PRIVATE, Modifier.STATIC)
        .initializer("$1L", initlstr);
    m_fieldsinfo.put("nfieldinfo", fieldinfo);
  }

  protected void buildGettersSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
    MethodInfo methodinfo;
    TypeName ftname;
    String unsafename = m_fieldsinfo.get("unsafe").name;
    String holdername = m_fieldsinfo.get("holder").name;
    String allocname = m_fieldsinfo.get("allocator").name;
    String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
    String factoryproxyname = m_fieldsinfo.get("factoryproxy").name;
    String genericfieldname = m_fieldsinfo.get("genericfield").name;
    String reclaimctxname = m_fieldsinfo.get("reclaimcontext").name;
    FieldInfo dynfieldinfo;
    CodeBlock.Builder code;
    String codefmt;
    for (String name : m_gettersinfo.keySet()) {
      code = CodeBlock.builder();
      methodinfo = m_gettersinfo.get(name);
      dynfieldinfo = m_dynfieldsinfo.get(name);
      ftname = m_dynfieldsinfo.get(name).type;
      if (isUnboxPrimitive(ftname)) {
        if (unboxTypeName(ftname).equals(TypeName.BOOLEAN)) {
          codefmt = "return 1 == $1N.$4L($2N.get() + $3L)";
        } else {
          codefmt = "return $1N.$4L($2N.get() + $3L)";
        }
        code.addStatement(codefmt, unsafename, holdername, dynfieldinfo.fieldoff,
            transTypeToUnsafeMethod(ftname, true));
      } else {
        if (methodinfo.rettype.toString().startsWith(DurableChunk.class.getCanonicalName())) {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)", unsafename, holdername,
              dynfieldinfo.fieldoff);
          code.beginControlFlow("if (0L != phandler)");
          code.addStatement("$1N = $2N.retrieveChunk(phandler, $3N, $4N)",
              dynfieldinfo.name, allocname, autoreclaimname, reclaimctxname);
          code.endControlFlow();
          code.endControlFlow();
          code.addStatement("return $1N", dynfieldinfo.name);
        }  else if (methodinfo.rettype.toString().startsWith(DurableBuffer.class.getCanonicalName())) {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)", unsafename, holdername,
              dynfieldinfo.fieldoff);
          code.beginControlFlow("if (0L != phandler)");
          code.addStatement("$1N = $2N.retrieveBuffer(phandler, $3N, $4N)",
              dynfieldinfo.name, allocname, autoreclaimname, reclaimctxname);
          code.endControlFlow();
          code.endControlFlow();
          code.addStatement("return $1N", dynfieldinfo.name);
        } else if (methodinfo.rettype.toString().equals(String.class.getCanonicalName())) {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)", unsafename, holdername,
              dynfieldinfo.fieldoff);
          code.beginControlFlow("if (0L != phandler)");
          code.addStatement("$1N = $2N.retrieveBuffer(phandler, $3N, $4N)",
              dynfieldinfo.name, allocname, autoreclaimname, reclaimctxname);
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("throw new RetrieveDurableEntityError(\"Retrieve String Buffer Failure.\")");
          code.endControlFlow();
          code.endControlFlow();
          code.endControlFlow();
          code.addStatement("return null == $1N ? null : $1N.get().asCharBuffer().toString()", dynfieldinfo.name);
        } else if (dynfieldinfo.type.toString().startsWith(GenericField.class.getCanonicalName())) {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("$1T proxy = null", TypeName.get(EntityFactoryProxy.class));
          code.addStatement("$1T gftype = null", TypeName.get(DurableType.class));
          code.addStatement("int gfpidx = $1L", getFactoryProxyIndex(methodinfo.rettype));
          code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", factoryproxyname);
          code.addStatement("proxy = $1L[gfpidx]", factoryproxyname);
          code.endControlFlow();
          code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", genericfieldname);
          code.addStatement("gftype = $1L[gfpidx]", genericfieldname);
          code.nextControlFlow("else");
          code.addStatement("throw new RetrieveDurableEntityError(\"No Generic Field Type Info.\")");
          code.endControlFlow();
          code.addStatement("$1N = new $2T(proxy, gftype, $9L, $10L, $3N, $4N, $5N, $6N, $7N.get() + $8L)",
                  dynfieldinfo.name, dynfieldinfo.type, allocname, unsafename, autoreclaimname, reclaimctxname,
                  holdername, dynfieldinfo.fieldoff, dynfieldinfo.efproxiesname, dynfieldinfo.gftypesname);
          code.endControlFlow();
          code.addStatement("return $1N.get()", dynfieldinfo.name);
        } else {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)", unsafename, holdername,
              dynfieldinfo.fieldoff);
          code.beginControlFlow("if (0L != phandler)");
          code.addStatement("$1N = $5N.restore($2N, $6L, $7L, phandler, $3N, $4N)", dynfieldinfo.name, allocname,
              autoreclaimname, reclaimctxname, String.format("%s%s",
                  m_typeutils.asElement(methodinfo.elem.getReturnType()).toString(), cFACTORYNAMESUFFIX),
              dynfieldinfo.efproxiesname, dynfieldinfo.gftypesname);
          code.endControlFlow();
          code.endControlFlow();
          code.addStatement("return $1N", dynfieldinfo.name);
        }
      }
      typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
    }
  }

  protected String gsetterName(String name, boolean isget) {
    return String.format("%s%s", isget ? "get" : "set", name); // Character.toUpperCase(name.charAt(0))
                                                               // +
                                                               // name.substring(1));
  }

  protected void buildSettersSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
    MethodInfo methodinfo;
    TypeName ftname, valtname;
    String unsafename = m_fieldsinfo.get("unsafe").name;
    String holdername = m_fieldsinfo.get("holder").name;
    String allocname = m_fieldsinfo.get("allocator").name;
    String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
    String factoryproxyname = m_fieldsinfo.get("factoryproxy").name;
    String genericfieldname = m_fieldsinfo.get("genericfield").name;
    String reclaimctxname = m_fieldsinfo.get("reclaimcontext").name;
    FieldInfo dynfieldinfo;
    CodeBlock.Builder code;
    VariableElement arg0;
    VariableElement arg1;
    String codefmt;
    for (String name : m_settersinfo.keySet()) {
      code = CodeBlock.builder();
      methodinfo = m_settersinfo.get(name);
      dynfieldinfo = m_dynfieldsinfo.get(name);
      ftname = m_dynfieldsinfo.get(name).type;
      valtname = m_gettersinfo.get(name).rettype;
      arg0 = methodinfo.elem.getParameters().get(0);
      if (!TypeName.get(arg0.asType()).equals(valtname)) {
        throw new AnnotationProcessingException(null, "%s has inconsistent value type with its getter/setter.", name);
      }
      if (isUnboxPrimitive(ftname)) {
        if (unboxTypeName(ftname).equals(TypeName.BOOLEAN)) {
          codefmt = "$1N.$4L($2N.get() + $3L, (byte) ($5L? 1 : 0))";
        } else {
          codefmt = "$1N.$4L($2N.get() + $3L, $5L)";
        }
        code.addStatement(codefmt, unsafename, holdername, dynfieldinfo.fieldoff,
            transTypeToUnsafeMethod(ftname, false), arg0);
      } else {
        try {
          arg1 = methodinfo.elem.getParameters().get(1);
          if (!TypeName.BOOLEAN.equals(TypeName.get(arg1.asType()))) {
            throw new AnnotationProcessingException(null, "the second parameter of %s's setter is not boolean type.",
                name);
          }
        } catch (IndexOutOfBoundsException ex) {
          throw new AnnotationProcessingException(null, "%s's setter has no second parameters for non primitive type.",
              name);
        }
        if (valtname.toString().startsWith(DurableChunk.class.getCanonicalName())) {
          code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
          code.addStatement("$1N.destroy()", dynfieldinfo.name);
          code.addStatement("$1N = null", dynfieldinfo.name);
          code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", unsafename, holdername, dynfieldinfo.fieldoff);
          code.endControlFlow();
          code.beginControlFlow("if ((null != $1L) && ($1L.getAllocator() != $2N))", arg0, allocname);
          code.addStatement("throw new IllegalAllocatorError(\"This chunk is allocated by another allocator!\")");
          code.endControlFlow();
          code.addStatement("$1N = $2L", dynfieldinfo.name, arg0);
          code.addStatement("$1N.putLong($2N.get() + $3L, null == $4N ? 0L : $4N.getHandler())",
              unsafename, holdername, dynfieldinfo.fieldoff, dynfieldinfo.name);
          code.beginControlFlow("if (null != $1L)", dynfieldinfo.name);
          code.beginControlFlow("if ($1N)", autoreclaimname);
          code.addStatement("$1N.registerAutoReclaim()", dynfieldinfo.name);
          code.nextControlFlow("else");
          code.addStatement("$1N.cancelAutoReclaim()", dynfieldinfo.name);
          code.endControlFlow();
          code.endControlFlow();
        } else if (valtname.toString().startsWith(DurableBuffer.class.getCanonicalName())) {
          code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
          code.addStatement("$1N.destroy()", dynfieldinfo.name);
          code.addStatement("$1N = null", dynfieldinfo.name);
          code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", unsafename, holdername, dynfieldinfo.fieldoff);
          code.endControlFlow();
          code.beginControlFlow("if ((null != $1L) && ($1L.getAllocator() != $2N))", arg0, allocname);
          code.addStatement("throw new IllegalAllocatorError(\"This buffer is allocated by another allocator!\")");
          code.endControlFlow();
          code.addStatement("$1N = $2L", dynfieldinfo.name, arg0);
          code.addStatement("$1N.putLong($2N.get() + $3L, null == $4N ? 0L : $4N.getHandler())",
              unsafename, holdername, dynfieldinfo.fieldoff, dynfieldinfo.name);
          code.beginControlFlow("if (null != $1L)", dynfieldinfo.name);
          code.beginControlFlow("if ($1N)", autoreclaimname);
          code.addStatement("$1N.registerAutoReclaim()", dynfieldinfo.name);
          code.nextControlFlow("else");
          code.addStatement("$1N.cancelAutoReclaim()", dynfieldinfo.name);
          code.endControlFlow();
          code.endControlFlow();
        } else if (valtname.toString().equals(String.class.getCanonicalName())) {
          code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
          code.addStatement("$1N.destroy()", dynfieldinfo.name);
          code.addStatement("$1N = null", dynfieldinfo.name);
          code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", unsafename, holdername, dynfieldinfo.fieldoff);
          code.endControlFlow();
          code.beginControlFlow("if (null == $1L)", arg0);
          code.addStatement("$1N.putLong($2N.get() + $3L, 0L)", unsafename, holdername, dynfieldinfo.fieldoff);
          code.nextControlFlow("else");
          code.addStatement("$1N = $2N.createBuffer($3L.length() * 2, $4N)", dynfieldinfo.name, allocname, arg0,
              autoreclaimname);
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("throw new OutOfHybridMemory(\"Create Durable String Error!\")");
          code.endControlFlow();
          code.addStatement("$1N.get().asCharBuffer().put($2L)", dynfieldinfo.name, arg0);
          code.addStatement("$1N.putLong($2N.get() + $3L, $4N.getBufferHandler($5N))", unsafename, holdername,
              dynfieldinfo.fieldoff, allocname, dynfieldinfo.name);
          code.endControlFlow();
        } else if (dynfieldinfo.type.toString().startsWith(GenericField.class.getCanonicalName())) {
          code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
          code.addStatement("$1T proxy = null", TypeName.get(EntityFactoryProxy.class));
          code.addStatement("$1T gftype = null", TypeName.get(DurableType.class));
          code.addStatement("int gfpidx = $1L", getFactoryProxyIndex(valtname));
          code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", factoryproxyname);
          code.addStatement("proxy = $1L[gfpidx]", factoryproxyname);
          code.endControlFlow();
          code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", genericfieldname);
          code.addStatement("gftype = $1L[gfpidx]", genericfieldname);
          code.nextControlFlow("else");
          code.addStatement("throw new RetrieveDurableEntityError(\"No Generic Field Type Info.\")");
          code.endControlFlow();
          code.addStatement("$1N = new $2T(proxy, gftype, $9L, $10L, $3N, $4N, $5N, $6N, $7N.get() + $8L)",
                  dynfieldinfo.name, dynfieldinfo.type, allocname, unsafename, autoreclaimname, reclaimctxname,
                  holdername, dynfieldinfo.fieldoff, dynfieldinfo.efproxiesname, dynfieldinfo.gftypesname);
          code.endControlFlow();
          code.beginControlFlow("if (null != $1L)", dynfieldinfo.name);
          code.addStatement("$1N.set($2L, $3L)", dynfieldinfo.name, arg0, arg1);
          code.nextControlFlow("else");
          code.addStatement("throw new RetrieveDurableEntityError(\"GenericField is null!\")");
          code.endControlFlow();
        } else {
          code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
          code.addStatement("$1N.destroy()", dynfieldinfo.name);
          code.addStatement("$1N = null", dynfieldinfo.name);
          code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", unsafename, holdername, dynfieldinfo.fieldoff);
          code.endControlFlow();
          code.addStatement("$1N = $2L", dynfieldinfo.name, arg0);
          code.addStatement("$1N.putLong($2N.get() + $3L, null == $4N ? 0L : $4N.getHandler())", unsafename,
              holdername, dynfieldinfo.fieldoff, dynfieldinfo.name);
          code.beginControlFlow("if (null != $1L)", dynfieldinfo.name);
          code.beginControlFlow("if ($1N)", autoreclaimname);
          code.beginControlFlow("if (!$1N.autoReclaim())", dynfieldinfo.name);
          code.addStatement("$1N.registerAutoReclaim();", dynfieldinfo.name);
          code.endControlFlow();
          code.nextControlFlow("else");
          code.beginControlFlow("if ($1N.autoReclaim())", dynfieldinfo.name);
          code.addStatement("$1N.cancelAutoReclaim();", dynfieldinfo.name);
          code.endControlFlow();
          code.endControlFlow();
          code.endControlFlow();
        }
      }
      typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
    }
  }

  protected void buildDurableMethodSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
    CodeBlock.Builder code;
    FieldInfo dynfieldinfo;
    String holdername = m_fieldsinfo.get("holder").name;
    String allocname = m_fieldsinfo.get("allocator").name;
    String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
    String reclaimctxname = m_fieldsinfo.get("reclaimcontext").name;
    for (String name : m_durablemtdinfo.keySet()) {
      for (MethodInfo methodinfo : m_durablemtdinfo.get(name)) {
        code = CodeBlock.builder();
        switch (name) {
          case "cancelAutoReclaim":
            code.addStatement("$1N.cancelAutoReclaim()", holdername);
            for (String fname : m_dynfieldsinfo.keySet()) {
              dynfieldinfo = m_dynfieldsinfo.get(fname);
              if (!isUnboxPrimitive(dynfieldinfo.type)) {
                code.beginControlFlow("if (null != $1N)", dynfieldinfo.name);
                code.addStatement("$1N.cancelAutoReclaim()", dynfieldinfo.name);
                code.endControlFlow();
              }
            }
            code.addStatement("$1N = false", autoreclaimname);
            break;
          case "registerAutoReclaim":
            if (methodinfo.elem.asType().toString().contains("ReclaimContext")) {
              VariableElement arg0 = methodinfo.elem.getParameters().get(0);
              code.addStatement("$1N.registerAutoReclaim($2L)", holdername, arg0);
              for (String fname : m_dynfieldsinfo.keySet()) {
                dynfieldinfo = m_dynfieldsinfo.get(fname);
                if (!isUnboxPrimitive(dynfieldinfo.type)) {
                  code.beginControlFlow("if (null != $1N)", dynfieldinfo.name);
                  code.addStatement("$1N.registerAutoReclaim($2L)", dynfieldinfo.name, arg0);
                  code.endControlFlow();
                }
              }
              code.addStatement("$1N = true", autoreclaimname);
              code.addStatement("$1N = $2L", reclaimctxname, arg0);
            } else {
              code.addStatement("this.registerAutoReclaim($1N)", reclaimctxname);
            }
            break;
          case "getHandler":
            code.addStatement("return $1N.getChunkHandler($2N)", allocname, holdername);
            break;
          case "autoReclaim":
            code.addStatement("return $1N", autoreclaimname);
            break;
          case "syncToVolatileMemory":
            code.addStatement("$1N.syncToVolatileMemory()", holdername);
            break;
          case "syncToNonVolatileMemory":
            code.addStatement("$1N.syncToNonVolatileMemory()", holdername);
            break;
          case "syncToLocal":
            code.addStatement("$1N.syncToLocal()", holdername);
            break;
          case "destroy":
            for (String fname : m_dynfieldsinfo.keySet()) {
              dynfieldinfo = m_dynfieldsinfo.get(fname);
              if (!isUnboxPrimitive(dynfieldinfo.type)) {
                code.beginControlFlow("if (null != $1N)", dynfieldinfo.name);
                code.addStatement("$1N.destroy()", dynfieldinfo.name);
                code.addStatement("$1N = null", dynfieldinfo.name);
                code.endControlFlow();
              }
            }
            code.addStatement("$1N.destroy()", holdername);
            break;
          case "getNativeFieldInfo":
            code.addStatement("return $1N", m_fieldsinfo.get("nfieldinfo").name);
            break;
          case "refbreak":
            for (FieldInfo fldinfo : m_dynfieldsinfo.values()) {
              if (fldinfo.refbreak && !isUnboxPrimitive(fldinfo.type)) {
                code.beginControlFlow("if ($1N != null && $1N.autoReclaim())", fldinfo.name);
                code.addStatement(
                    "throw new ReferenceBreakingException(\"Not be able to break reference if it is reclaimable.\")");
                code.endControlFlow();
                code.addStatement("$1N = null", fldinfo.name);
              }
            }
            break;
          default:
            throw new AnnotationProcessingException(null, "Method %s is not supported.", name);
        }
        typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
      }
    }
  }

  protected void buildEntityMethodSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
    MethodInfo methodinfo;
    CodeBlock.Builder code;
    VariableElement arg0, arg1, arg2, arg3, arg4, arg5;
    String unsafename = m_fieldsinfo.get("unsafe").name;
    String holdername = m_fieldsinfo.get("holder").name;
    String allocname = m_fieldsinfo.get("allocator").name;
    String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
    String factoryproxyname = m_fieldsinfo.get("factoryproxy").name;
    String genericfieldname = m_fieldsinfo.get("genericfield").name;
    String reclaimctxname = m_fieldsinfo.get("reclaimcontext").name;
    for (String name : m_entitymtdinfo.keySet()) {
      methodinfo = m_entitymtdinfo.get(name);
      code = CodeBlock.builder();
      arg0 = methodinfo.elem.getParameters().get(0);
      arg1 = methodinfo.elem.getParameters().get(1);
      arg2 = methodinfo.elem.getParameters().get(2);
      switch (name) {
      case "initializeDurableEntity":
        arg3 = methodinfo.elem.getParameters().get(3);
        arg4 = methodinfo.elem.getParameters().get(4);
        code.addStatement("$1N = $2L", allocname, arg0);
        code.addStatement("$1N = $2L", factoryproxyname, arg1);
        code.addStatement("$1N = $2L", genericfieldname, arg2);
        code.addStatement("$1N = $2L", autoreclaimname, arg3);
        code.addStatement("$1N = $2L", reclaimctxname, arg4);
        code.beginControlFlow("try");
        code.addStatement("$1N = $2T.getUnsafe()", unsafename, Utils.class);
        code.nextControlFlow("catch (Exception e)");
        code.addStatement("e.printStackTrace()");
        code.endControlFlow();
        break;
      case "createDurableEntity":
        arg3 = methodinfo.elem.getParameters().get(3);
        arg4 = methodinfo.elem.getParameters().get(4);
        code.addStatement("initializeDurableEntity($1L, $2L, $3L, $4L, $5L)", arg0, arg1, arg2, arg3, arg4);
        code.addStatement("$1N = $2N.createChunk($3L, $4N, $5N)",
                holdername, allocname, m_holdersize, autoreclaimname, reclaimctxname);
        code.beginControlFlow("if (null == $1N)", holdername);
        code.addStatement("throw new OutOfHybridMemory(\"Create Durable Entity Error!\")");
        code.endControlFlow();
        // code.beginControlFlow("try");
        // for (String fname : m_dynfieldsinfo.keySet()) {
        // dynfieldinfo = m_dynfieldsinfo.get(fname);
        // if (isUnboxPrimitive(dynfieldinfo.type)) {
        // code.addStatement("$1N($2L)", gsetterName(fname, false),
        // getIntialValueLiteral(dynfieldinfo.type));
        // } else {
        // code.addStatement("$1N(null, false)", gsetterName(fname, false));
        // }
        // }
        // code.nextControlFlow("catch(RetrieveDurableEntityError ex)");
        // code.endControlFlow();
        code.addStatement("initializeAfterCreate()");
        break;
      case "restoreDurableEntity":
        arg3 = methodinfo.elem.getParameters().get(3);
        arg4 = methodinfo.elem.getParameters().get(4);
        arg5 = methodinfo.elem.getParameters().get(5);
//        code.beginControlFlow("if ($1L instanceof RestorableAllocator)", arg0);
//        code.addStatement(
//               "throw new RestoreDurableEntityError(\"Allocator does not support restore operation in $1N.\")",
//               name);
//        code.endControlFlow();
        code.addStatement("initializeDurableEntity($1L, $2L, $3L, $4L, $5L)", arg0, arg1, arg2, arg4, arg5);
        code.beginControlFlow("if (0L == $1L)", arg3);
        code.addStatement("throw new RestoreDurableEntityError(\"Input handler is null on $1N.\")", name);
        code.endControlFlow();
        code.addStatement("$1N = $2N.retrieveChunk($3L, $4N, $5N)",
                holdername, allocname, arg3, autoreclaimname, reclaimctxname);
        code.beginControlFlow("if (null == $1N)", holdername);
        code.addStatement("throw new RestoreDurableEntityError(\"Retrieve Entity Failure!\")");
        code.endControlFlow();
        code.addStatement("initializeAfterRestore()");
        break;
      default:
        throw new AnnotationProcessingException(null, "Method %s is not supported.", name);
      }
      typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
    }
  }

  protected void buildExtraMethodSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
    MethodInfo methodinfo;
    CodeBlock.Builder code;
    for (String name : m_extramtdinfo.keySet()) {
      methodinfo = m_extramtdinfo.get(name);
      code = CodeBlock.builder();
      switch (name) {
        case "getNativeFieldInfo_static":
          code.addStatement("return $1N", m_fieldsinfo.get("nfieldinfo").name);
          break;
        default:
          throw new AnnotationProcessingException(null, "Method %s is not supported.", name);
      }
      typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
    }
  }

  protected void buildFieldSpecs(TypeSpec.Builder typespecbuilder, Map<String, FieldInfo> fieldinfos) {
    FieldInfo fieldinfo;
    for (String name : fieldinfos.keySet()) {
      fieldinfo = fieldinfos.get(name);
      if (null != fieldinfo.specbuilder) {
        typespecbuilder.addField(fieldinfo.specbuilder.build());
        if (name.equals("unsafe")) {
            typespecbuilder.alwaysQualify("Unsafe");
        }
      }
    }
  }

  protected void buildFactoryMethodSpecs(TypeSpec entityspec, TypeSpec.Builder typespecbuilder)
      throws AnnotationProcessingException {
    MethodSpec methodspec;
    CodeBlock code;

    TypeName entitytn = ParameterizedTypeName.get(ClassName.get(m_packagename, m_entityname),
        entityspec.typeVariables.toArray(new TypeVariableName[0]));

    ParameterSpec parameterhold = ParameterSpec.builder(m_parameterholder, "parameterholder").build();
    code = CodeBlock.builder().addStatement("$1T entity = new $1T()", entitytn)
        .addStatement("entity.setupGenericInfo($1N.getEntityFactoryProxies(), $1N.getGenericTypes())",
                parameterhold.name)
        .addStatement("entity.createDurableEntity($1L.getAllocator(), $1L.getEntityFactoryProxies(), "
                    + "$1L.getGenericTypes(), $1L.getAutoReclaim(), null)", parameterhold.name)
        .addStatement("return entity").build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
        .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(parameterhold).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    ParameterSpec allocparam = ParameterSpec.builder(m_alloctypename, "allocator").build();
    code = CodeBlock.builder().addStatement("return create($1L, false)", allocparam.name).build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
        .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    ParameterSpec autoreclaimparam = ParameterSpec.builder(TypeName.BOOLEAN, "autoreclaim").build();
    code = CodeBlock.builder()
        .addStatement("return create($1L, null, null, $2L, null)", allocparam.name, autoreclaimparam.name).build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
        .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(autoreclaimparam).addCode(code)
        .build();
    typespecbuilder.addMethod(methodspec);

    ParameterSpec reclaimctxparam = ParameterSpec.builder(m_reclaimctxtypename, "reclaimcontext").build();
    code = CodeBlock.builder()
            .addStatement("return create($1L, null, null, $2L, $3L)",
                    allocparam.name, autoreclaimparam.name, reclaimctxparam.name).build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
            .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(m_elem.asType())).addParameter(allocparam)
            .addParameter(autoreclaimparam).addParameter(reclaimctxparam).addCode(code)
            .build();
    typespecbuilder.addMethod(methodspec);

    ParameterSpec factoryproxysparam = ParameterSpec.builder(m_factoryproxystypename, "factoryproxys").build();
    ParameterSpec gfieldsparam = ParameterSpec.builder(m_gfieldstypename, "gfields").build();
    code = CodeBlock.builder()
            .addStatement("return create($1L, $2L, $3L, $4L, null)",
                    allocparam.name, factoryproxysparam.name, gfieldsparam.name, autoreclaimparam.name).build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
            .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(factoryproxysparam)
            .addParameter(gfieldsparam).addParameter(autoreclaimparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("$1T entity = new $1T()", entitytn)
        .addStatement("entity.setupGenericInfo($1N, $2N)", factoryproxysparam.name, gfieldsparam.name)
        .addStatement("entity.createDurableEntity($1L, $2L, $3L, $4L, $5L)",
                allocparam.name, factoryproxysparam.name, gfieldsparam.name,
                autoreclaimparam.name, reclaimctxparam.name)
        .addStatement("return entity").build();
    methodspec = MethodSpec.methodBuilder("create").addTypeVariables(entityspec.typeVariables)
        .addException(OutOfHybridMemory.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(factoryproxysparam)
        .addParameter(gfieldsparam).addParameter(autoreclaimparam).addParameter(reclaimctxparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    ParameterSpec phandlerparam = ParameterSpec.builder(TypeName.LONG, "phandler").build();
    code = CodeBlock.builder().addStatement("return restore($1L, $2L, false)", allocparam.name, phandlerparam.name)
        .build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
        .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(phandlerparam).addCode(code)
        .build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("$1T entity = new $1T()", entitytn)
        .addStatement("entity.setupGenericInfo($1N.getEntityFactoryProxies(), $1N.getGenericTypes())",
                parameterhold.name)
        .addStatement("entity.restoreDurableEntity($1L.getAllocator(), $1L.getEntityFactoryProxies(),"
                    + "$1L.getGenericTypes(), $1L.getHandler(), $1L.getAutoReclaim(), null)", parameterhold.name)
        .addStatement("return entity").build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
            .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(m_elem.asType())).addParameter(parameterhold).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("return restore($1L, null, null, $2L, $3L, null)", allocparam.name,
        phandlerparam.name, autoreclaimparam.name).build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
        .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(phandlerparam)
        .addParameter(autoreclaimparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("return restore($1L, null, null, $2L, $3L, $4L)", allocparam.name,
            phandlerparam.name, autoreclaimparam.name, reclaimctxparam.name).build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
            .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(phandlerparam)
            .addParameter(autoreclaimparam).addParameter(reclaimctxparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("return restore($1L, $2L, $3L, $4L, $5L, null)", allocparam.name,
                    factoryproxysparam.name, gfieldsparam.name, phandlerparam.name, autoreclaimparam.name).build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
            .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(factoryproxysparam)
            .addParameter(gfieldsparam).addParameter(phandlerparam)
            .addParameter(autoreclaimparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    code = CodeBlock.builder().addStatement("$1T entity = new $1T()", entitytn)
        .addStatement("entity.setupGenericInfo($1N, $2N)", factoryproxysparam.name, gfieldsparam.name)
        .addStatement("entity.restoreDurableEntity($1L, $2L, $3L, $4L, $5L, $6L)", allocparam.name,
                factoryproxysparam.name, gfieldsparam.name, phandlerparam.name,
                autoreclaimparam.name, reclaimctxparam.name)
        .addStatement("return entity").build();
    methodspec = MethodSpec.methodBuilder("restore").addTypeVariables(entityspec.typeVariables)
        .addException(RestoreDurableEntityError.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.get(m_elem.asType())).addParameter(allocparam).addParameter(factoryproxysparam)
        .addParameter(gfieldsparam).addParameter(phandlerparam)
        .addParameter(autoreclaimparam).addParameter(reclaimctxparam).addCode(code).build();
    typespecbuilder.addMethod(methodspec);

    MethodInfo mi = m_extramtdinfo.get("getNativeFieldInfo_static");
    assert null != mi;
    code = CodeBlock.builder().addStatement("return $1T.getNativeFieldInfo_static()",
            ClassName.get(m_packagename, m_entityname))
            .build();
    methodspec = MethodSpec.methodBuilder("getNativeFieldInfo")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mi.rettype).addCode(code)
            .build();
    typespecbuilder.addMethod(methodspec);

  }

  public void generateCode(Filer filer) throws IOException, AnnotationProcessingException {
    AnnotationSpec classannotation = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "restriction").build();

    TypeSpec.Builder entitybuilder = TypeSpec.classBuilder(m_entityname).superclass(TypeName.get(m_elem.asType()))
        .addModifiers(Modifier.PUBLIC).addAnnotation(classannotation)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(MemoryDurableEntity.class), m_alloctypevarname))
        .addTypeVariable(m_alloctypevarname);

    for (TypeParameterElement tpe : m_elem.getTypeParameters()) {
      entitybuilder.addTypeVariable(TypeVariableName.get(tpe));
    }

    buildFieldSpecs(entitybuilder, m_dynfieldsinfo);
    buildFieldSpecs(entitybuilder, m_fieldsinfo);

    buildGettersSpecs(entitybuilder);
    buildSettersSpecs(entitybuilder);

    buildDurableMethodSpecs(entitybuilder);
    buildEntityMethodSpecs(entitybuilder);
    buildExtraMethodSpecs(entitybuilder);

    TypeSpec entityspec = entitybuilder.build();

    JavaFile entityFile = JavaFile.builder(m_packagename, entityspec).build();

    entityFile.writeTo(filer);

    TypeSpec.Builder factorybuilder = TypeSpec.classBuilder(m_factoryname).addModifiers(Modifier.PUBLIC);

    buildFactoryMethodSpecs(entityspec, factorybuilder);

    JavaFile factoryFile = JavaFile.builder(m_packagename, factorybuilder.build()).build();

    factoryFile.writeTo(filer);

  }

  public static Builder overriding(ExecutableElement method, String varname) {

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    methodBuilder.addAnnotation(Override.class);
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
      if (annotationSpec.type.equals(Override.class)) {
        continue;
      }
      methodBuilder.addAnnotation(annotationSpec);
    }

    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    methodBuilder.returns(TypeName.get(method.getReturnType()));

    List<? extends VariableElement> parameters = method.getParameters();
    TypeName type;
    for (VariableElement parameter : parameters) {
      if (parameter.asType().getKind() == TypeKind.TYPEVAR && parameter.asType().toString().equals(varname)) {
        type = TypeVariableName.get(varname);
      } else {
        type = TypeName.get(parameter.asType());
      }

      String name = parameter.getSimpleName().toString();
      Set<Modifier> parameterModifiers = parameter.getModifiers();
      ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
          .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
      for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
        parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
      }
      methodBuilder.addParameter(parameterBuilder.build());
    }
    methodBuilder.varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    return methodBuilder;
  }

}
