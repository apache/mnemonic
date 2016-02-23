package com.intel.bigdatamem;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import sun.misc.Unsafe;

import com.squareup.javapoet.*;
import com.squareup.javapoet.MethodSpec.Builder;

/**
 * this class managed to generate generic non-volatile concrete object and their corresponding factory.
 *
 */
public class AnnotatedNonVolatileEntityClass {
    protected class MethodInfo {
	public ExecutableElement elem;
	public MethodSpec.Builder specbuilder;
	public TypeName rettype;
    }
    protected class FieldInfo {
	public FieldSpec.Builder specbuilder;
	public TypeName type;
	public String name;
	public String efproxiesname;
	public String gftypesname;
	public long fieldoff;
	public long fieldsize;
    }

    protected final String FACTORYNAMESUFFIX = "Factory";
    protected final String PMEMNAMEPREFIX = "NonVolatile_";
    protected final String FIELDNAMESUFFIX = String.format("_field_%s", Utils.genRandomString());
    protected final String ALLOCATORFIELDNAME = String.format("alloc_%s", Utils.genRandomString());
    protected final String AUTORECLAIMFIELDNAME = String.format("autoreclaim_%s", Utils.genRandomString());
    protected final String UNSAFEFIELDNAME = String.format("unsafe_%s", Utils.genRandomString());
    protected final String HOLDERFIELDNAME = String.format("holder_%s", Utils.genRandomString());
    protected final String ALLOCTYPENAME = String.format("ALLOC_PMem3C93D24F59");

    private Types m_typeutils;
    private Elements m_elemutils;
    private Messager m_msgr;
    private TypeElement m_elem;

    private String m_factoryname;
    private String m_entityname;

    private long m_holdersize;
	
    private String m_packagename;

    private TypeName m_alloctypename = TypeVariableName.get(ALLOCTYPENAME);
    private TypeName m_factoryproxystypename = TypeName.get(EntityFactoryProxy[].class);
    private TypeName m_gfieldstypename = TypeName.get(GenericField.GType[].class);
    private TypeVariableName m_alloctypevarname = TypeVariableName.get(ALLOCTYPENAME,
								       ParameterizedTypeName.get(ClassName.get(CommonPersistAllocator.class), TypeVariableName.get(ALLOCTYPENAME)));

    private Map<String, MethodInfo> m_gettersinfo = new HashMap<String, MethodInfo>();
    private Map<String, MethodInfo> m_settersinfo = new HashMap<String, MethodInfo>();
    private Map<String, FieldInfo> m_dynfieldsinfo = new HashMap<String, FieldInfo>();
    private Map<String, FieldInfo> m_fieldsinfo = new HashMap<String, FieldInfo>();

    private Map<String, MethodInfo> m_durablemtdinfo = new HashMap<String, MethodInfo>();
    private Map<String, MethodInfo> m_entitymtdinfo = new HashMap<String, MethodInfo>();

    private long computeTypeSize(TypeMirror type) {
	long ret;
	switch (type.getKind()) {
	case BYTE:
	    ret = 1L;
	    break;
	case BOOLEAN:
	    ret = 1L;
	    break;
	case CHAR:
	    ret = 2L;
	    break;
	case DOUBLE:
	    ret = 8L;
	    break;
	case FLOAT:
	    ret = 4L;
	    break;
	case SHORT:
	    ret = 2L;
	    break;
	case INT:
	    ret = 4L;
	    break;
	case LONG:
	    ret = 8L;
	    break;
	default:
	    ret = 8L;
	}
	return ret;
    }

    private boolean isUnboxPrimitive(TypeName tn) {
	TypeName n = tn;
	try {
	    n = tn.unbox();
	} catch(UnsupportedOperationException ex) {}
	return n.isPrimitive();
    }
	
    private TypeName unboxTypeName(TypeName tn) {
	TypeName n = tn;
	try {
	    n = tn.unbox();
	} catch(UnsupportedOperationException ex) {}
	return n;
    }	
	
    public AnnotatedNonVolatileEntityClass(TypeElement classElement, Types typeUtils, Elements elementUtils,
					   Messager messager) {
	m_elem = classElement;
	m_typeutils = typeUtils;
	m_elemutils = elementUtils;
	m_msgr = messager;
		
	m_packagename = m_elemutils.getPackageOf(m_elem).getQualifiedName().toString();

	m_factoryname = String.format("%s%s", m_elem.getSimpleName(), FACTORYNAMESUFFIX);
	m_entityname = String.format("%s%s_%s", PMEMNAMEPREFIX, m_elem.getSimpleName(), Utils.genRandomString());

	m_durablemtdinfo.put("cancelAutoReclaim", new MethodInfo());
	m_durablemtdinfo.put("registerAutoReclaim", new MethodInfo());
	m_durablemtdinfo.put("getNonVolatileHandler", new MethodInfo());
	m_durablemtdinfo.put("autoReclaim", new MethodInfo());
	m_durablemtdinfo.put("destroy", new MethodInfo());

	m_entitymtdinfo.put("initializeNonVolatileEntity", new MethodInfo());
	m_entitymtdinfo.put("createNonVolatileEntity", new MethodInfo());
	m_entitymtdinfo.put("restoreNonVolatileEntity", new MethodInfo());

    }

    public void prepareProcessing() throws AnnotationProcessingException {
	MethodInfo methodinfo = null;
	FieldInfo fieldinfo;
	String methodname;
	long fieldoff = 0;
	TypeElement intf_durable = m_elemutils.getTypeElement(Durable.class.getCanonicalName());
	TypeElement intf_entity = m_elemutils.getTypeElement(MemoryNonVolatileEntity.class.getCanonicalName());
	//		System.err.printf("<><><><><> %s ======\n", intf_entity.toString());

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
	fieldinfo.name = String.format("m_unsafe_%s",
				       Utils.genRandomString());
	fieldinfo.type = TypeName.get(m_elemutils.getTypeElement(Unsafe.class.getCanonicalName()).asType());
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE);
	m_fieldsinfo.put("unsafe", fieldinfo);
		
	fieldinfo = new FieldInfo();
	fieldinfo.name = String.format("m_holder_%s",
				       Utils.genRandomString());
	fieldinfo.type = ParameterizedTypeName.get(ClassName.get(MemChunkHolder.class), m_alloctypename);
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE);
	m_fieldsinfo.put("holder", fieldinfo);
		
	fieldinfo = new FieldInfo();
	fieldinfo.name = String.format("m_autoreclaim_%s",
				       Utils.genRandomString());
	fieldinfo.type = TypeName.get(m_typeutils.getPrimitiveType(TypeKind.BOOLEAN));
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE, Modifier.VOLATILE);
	m_fieldsinfo.put("autoreclaim", fieldinfo);
		
	fieldinfo = new FieldInfo();
	fieldinfo.name = String.format("m_allocator_%s",
				       Utils.genRandomString());
	fieldinfo.type = m_alloctypename;
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE);
	m_fieldsinfo.put("allocator", fieldinfo);
		
	fieldinfo = new FieldInfo();
	fieldinfo.name = String.format("m_factoryproxy_%s",
				       Utils.genRandomString());
	fieldinfo.type = m_factoryproxystypename;
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE);
	m_fieldsinfo.put("factoryproxy", fieldinfo);

	fieldinfo = new FieldInfo();
	fieldinfo.name = String.format("m_genericfield_%s",
				       Utils.genRandomString());
	fieldinfo.type = m_gfieldstypename;
	fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
						  fieldinfo.name, Modifier.PRIVATE);
	m_fieldsinfo.put("genericfield", fieldinfo);

		
	for (Element elem : m_elem.getEnclosedElements()) {
	    if (elem.getKind() == ElementKind.METHOD) {
		methodname = elem.getSimpleName().toString();
		//				System.err.printf("=========== %s ======\n", methodname);
		NonVolatileGetter pgetter = elem.getAnnotation(NonVolatileGetter.class); 
		if (pgetter != null) {
		    if (!elem.getModifiers().contains(Modifier.ABSTRACT)) {
			throw new AnnotationProcessingException(elem,
								"%s annotated with NonVolatileGetter is not abstract.", methodname);
		    }
		    if (null != elem.getAnnotation(NonVolatileSetter.class)) {
			throw new AnnotationProcessingException(elem, "%s is annotated with NonVolatileSetter as well.",
								methodname);
		    }
		    if (!methodname.startsWith("get")) {
			throw new AnnotationProcessingException(elem, "%s does not comply name convention of getter.",
								methodname);
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
			fieldinfo.type = ParameterizedTypeName.get(ClassName.get(GenericField.class), m_alloctypename, fieldinfo.type);
		    }
		    fieldinfo.name = String.format("m_%s_%s",
						   methodname.substring(3).toLowerCase(),
						   Utils.genRandomString());
		    fieldinfo.specbuilder = FieldSpec.builder(fieldinfo.type,
							      fieldinfo.name, Modifier.PRIVATE);
		    fieldinfo.fieldsize = computeTypeSize(methodinfo.elem.getReturnType());
		    fieldinfo.fieldoff = fieldoff;
		    fieldoff += fieldinfo.fieldsize;
		    fieldinfo.efproxiesname = pgetter.EntityFactoryProxies();
		    fieldinfo.gftypesname = pgetter.GenericFieldTypes();
		    m_dynfieldsinfo.put(methodname.substring(3), fieldinfo);
					
		}
		if (null != elem.getAnnotation(NonVolatileSetter.class)) {
		    if (!elem.getModifiers().contains(Modifier.ABSTRACT)) {
			throw new AnnotationProcessingException(elem,
								"%s annotated with NonVolatileSetter is not abstract.", methodname);
		    }
		    if (!methodname.startsWith("set")) {
			throw new AnnotationProcessingException(elem, "%s does not comply name convention of setter.",
								methodname);
		    }
		    methodinfo = new MethodInfo();
		    methodinfo.elem = (ExecutableElement) elem;
		    methodinfo.specbuilder = MethodSpec.overriding(methodinfo.elem);
		    m_settersinfo.put(methodname.substring(3), methodinfo);
		}
		//				if (!methodinfo.elem.getThrownTypes().contains(m_elemutils.getTypeElement(RetrieveNonVolatileEntityError.class.getCanonicalName()).asType())) {
		//					throw new AnnotationProcessingException(methodinfo.elem, "%s must throw out %s.",
		//							methodname, RetrieveNonVolatileEntityError.class.getName());
		//				}
	    }
	}

	m_holdersize = fieldoff;
		
	//		MethodInfo minfo = null;
	for (String name : m_settersinfo.keySet()) {
	    if (!m_gettersinfo.containsKey(name)) {
		throw new AnnotationProcessingException(null, "%s has no getter.", name);
	    }
	    //			if (m_dynfieldsinfo.get(name).type.toString().equals(String.class.getCanonicalName())) {
	    //				minfo = m_settersinfo.get(name);
	    //				if (!minfo.elem.getThrownTypes().contains(m_elemutils.getTypeElement(OutOfPersistentMemory.class.getCanonicalName()).asType())) {
	    //					throw new AnnotationProcessingException(minfo.elem, "%s must throw out %s.",
	    //							gsetterName(name, false), OutOfPersistentMemory.class.getName());
	    //				}
	    //			}
	}
		
	//		for (String name : m_dynfieldsinfo.keySet()) {
	//			if (!isUnboxPrimitive(m_dynfieldsinfo.get(name).type)) {
	//				if (m_gettersinfo.containsKey(name)) {
	//					minfo = m_gettersinfo.get(name);
	//					if (!minfo.elem.getThrownTypes().contains(m_elemutils.getTypeElement(RetrieveNonVolatileEntityError.class.getCanonicalName()).asType())) {
	//						throw new AnnotationProcessingException(minfo.elem, "%s must throw out %s.",
	//								gsetterName(name, true), RetrieveNonVolatileEntityError.class.getName());
	//					}
	//				}
	//				if (m_settersinfo.containsKey(name)) {
	//					minfo = m_settersinfo.get(name);
	//					if (!minfo.elem.getThrownTypes().contains(m_elemutils.getTypeElement(RetrieveNonVolatileEntityError.class.getCanonicalName()).asType())) {
	//						throw new AnnotationProcessingException(minfo.elem, "%s must throw out %s.",
	//								gsetterName(name, false), RetrieveNonVolatileEntityError.class.getName());
	//					}
	//				}
	//			}
	//		}

	for (Element elem : intf_durable.getEnclosedElements()) {
	    if (elem.getKind() == ElementKind.METHOD) {
		methodname = elem.getSimpleName().toString();
		if (m_durablemtdinfo.containsKey(methodname)) {
		    //					System.err.printf("**++++++++++ %s ======\n", methodname);
		    methodinfo = m_durablemtdinfo.get(methodname);
		    methodinfo.elem = (ExecutableElement) elem;
		    methodinfo.specbuilder = MethodSpec.overriding(methodinfo.elem);
		}
	    }
	}

	for (Element elem : intf_entity.getEnclosedElements()) {
	    if (elem.getKind() == ElementKind.METHOD) {
		methodname = elem.getSimpleName().toString();
		if (m_entitymtdinfo.containsKey(methodname)) {
		    //					System.err.printf("**------- %s ======\n", elem.toString());
		    methodinfo = m_entitymtdinfo.get(methodname);
		    methodinfo.elem = (ExecutableElement) elem;
		    methodinfo.specbuilder = overriding(methodinfo.elem, ALLOCTYPENAME);

		}
	    }
	}
    }

    protected String transTypeToUnsafeMethod(TypeName tname, boolean isget) throws AnnotationProcessingException {
	String ret = null;
	if (isUnboxPrimitive(tname)) {
	    TypeName tn = unboxTypeName(tname);
	    if (tn.equals(TypeName.BOOLEAN)) ret = isget ? "getByte" : "putByte";
	    if (tn.equals(TypeName.BYTE)) ret = isget ? "getByte" : "putByte";
	    if (tn.equals(TypeName.CHAR)) ret = isget ? "getChar" : "putChar";
	    if (tn.equals(TypeName.DOUBLE)) ret = isget ? "getDouble" : "putDouble";
	    if (tn.equals(TypeName.FLOAT)) ret = isget ? "getFloat" : "putFloat";
	    if (tn.equals(TypeName.INT)) ret = isget ? "getInt" : "putInt";
	    if (tn.equals(TypeName.LONG)) ret = isget ? "getLong" : "putLong";
	    if (tn.equals(TypeName.SHORT)) ret = isget ? "getShort" : "putShort";
	} else {
	    ret = isget ? "getAddress" : "putAddress";
	}
	if (null == ret) {
	    throw new AnnotationProcessingException(null, "%s is not supported by getters or setters.",
						    tname.toString());
	}
	return ret;
    }
	
    protected String getIntialValueLiteral(TypeName tname) throws AnnotationProcessingException {
	String ret = null;
	if (isUnboxPrimitive(tname)) {
	    TypeName tn = unboxTypeName(tname);
	    if (tn.equals(TypeName.BOOLEAN)) ret = "false";
	    if (tn.equals(TypeName.BYTE)) ret = "(byte)0";
	    if (tn.equals(TypeName.CHAR)) ret = "(char)0";
	    if (tn.equals(TypeName.DOUBLE)) ret = "(double)0.0";
	    if (tn.equals(TypeName.FLOAT)) ret = "(float)0.0";
	    if (tn.equals(TypeName.INT)) ret = "(int)0";
	    if (tn.equals(TypeName.LONG)) ret = "(long)0";
	    if (tn.equals(TypeName.SHORT)) ret = "(short)0";
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
	
    protected void buildGettersSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
	MethodInfo methodinfo;
	TypeName ftname;
	String unsafename = m_fieldsinfo.get("unsafe").name;
	String holdername = m_fieldsinfo.get("holder").name;
	String allocname = m_fieldsinfo.get("allocator").name;
	String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
	String factoryproxyname = m_fieldsinfo.get("factoryproxy").name;
	String genericfieldname = m_fieldsinfo.get("genericfield").name;
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
		code.addStatement(codefmt, unsafename, holdername, dynfieldinfo.fieldoff, transTypeToUnsafeMethod(ftname, true));
	    } else {
		if(methodinfo.rettype.toString().equals(String.class.getCanonicalName())) {
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)",
				      unsafename, holdername, dynfieldinfo.fieldoff);
		    code.beginControlFlow("if (0L != phandler)");
		    code.addStatement("$1N = $2N.retrieveBuffer(phandler, $3N)",
				      dynfieldinfo.name, allocname, autoreclaimname);
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("throw new RetrieveNonVolatileEntityError(\"Retrieve String Buffer Failure.\")");
		    code.endControlFlow();
		    code.endControlFlow();
		    code.endControlFlow();
		    code.addStatement("return null == $1N ? null : $1N.get().asCharBuffer().toString()", 
				      dynfieldinfo.name);
		} else if (dynfieldinfo.type.toString().startsWith(GenericField.class.getCanonicalName())) {
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("$1T proxy = null", TypeName.get(EntityFactoryProxy.class));
		    code.addStatement("$1T gftype = null", TypeName.get(GenericField.GType.class));
		    code.addStatement("int gfpidx = $1L", getFactoryProxyIndex(methodinfo.rettype));
		    code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", factoryproxyname);
		    code.addStatement("proxy = $1L[gfpidx]", factoryproxyname);
		    code.endControlFlow();
		    code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", genericfieldname);
		    code.addStatement("gftype = $1L[gfpidx]", genericfieldname);
		    code.nextControlFlow("else");
		    code.addStatement("throw new RetrieveNonVolatileEntityError(\"No Generic Field Type Info.\")");
		    code.endControlFlow();
		    code.addStatement("$1N = new $2T(proxy, gftype, $8L, $9L, $3N, $4N, $5N, $6N.get() + $7L)", 
				      dynfieldinfo.name, dynfieldinfo.type, 
				      allocname, unsafename, autoreclaimname,	holdername, dynfieldinfo.fieldoff,
				      dynfieldinfo.efproxiesname, dynfieldinfo.gftypesname);
		    code.endControlFlow();
		    code.addStatement("return $1N.get()", dynfieldinfo.name);
		} else {
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("long phandler = $1N.getAddress($2N.get() + $3L)", 
				      unsafename, holdername, dynfieldinfo.fieldoff);
		    code.beginControlFlow("if (0L != phandler)");
		    code.addStatement("$1N = $4N.restore($2N, $5L, $6L, phandler, $3N)", 
				      dynfieldinfo.name, allocname, autoreclaimname, 
				      String.format("%s%s", m_typeutils.asElement(methodinfo.elem.getReturnType()).getSimpleName(), FACTORYNAMESUFFIX),
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
	return String.format("%s%s", isget ? "get" : "set", name); //Character.toUpperCase(name.charAt(0)) + name.substring(1));
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
		throw new AnnotationProcessingException(null, "%s has inconsistent value type with its getter/setter.",
							name);
	    }
	    if (isUnboxPrimitive(ftname)) {
		if (unboxTypeName(ftname).equals(TypeName.BOOLEAN)) {
		    codefmt = "$1N.$4L($2N.get() + $3L, $5L?1:0)";
		} else {
		    codefmt = "$1N.$4L($2N.get() + $3L, $5L)";
		}
		code.addStatement(codefmt, unsafename, holdername, dynfieldinfo.fieldoff, transTypeToUnsafeMethod(ftname, false), arg0);
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
		if(valtname.toString().equals(String.class.getCanonicalName())) {
		    code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
		    code.addStatement("$1N.destroy()", dynfieldinfo.name);
		    code.addStatement("$1N = null", dynfieldinfo.name);
		    code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", 
				      unsafename, holdername, dynfieldinfo.fieldoff);
		    code.endControlFlow();
		    code.beginControlFlow("if (null == $1L)", arg0);
		    code.addStatement("$1N.putLong($2N.get() + $3L, 0L)", 
				      unsafename, holdername, dynfieldinfo.fieldoff);
		    code.nextControlFlow("else");
		    code.addStatement("$1N = $2N.createBuffer($3L.length() * 2, $4N)", 
				      dynfieldinfo.name, allocname, arg0, autoreclaimname);
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("throw new OutOfPersistentMemory(\"Create Non-Volatile String Error!\")");
		    code.endControlFlow();
		    code.addStatement("$1N.get().asCharBuffer().put($2L)", dynfieldinfo.name, arg0);
		    code.addStatement("$1N.putLong($2N.get() + $3L, $4N.getBufferHandler($5N))", 
				      unsafename, holdername, dynfieldinfo.fieldoff, allocname, dynfieldinfo.name);
		    code.endControlFlow();
		} else if (dynfieldinfo.type.toString().startsWith(GenericField.class.getCanonicalName())) {
		    code.beginControlFlow("if (null == $1N)", dynfieldinfo.name);
		    code.addStatement("$1T proxy = null", TypeName.get(EntityFactoryProxy.class));
		    code.addStatement("$1T gftype = null", TypeName.get(GenericField.GType.class));
		    code.addStatement("int gfpidx = $1L", getFactoryProxyIndex(valtname));
		    code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", factoryproxyname);
		    code.addStatement("proxy = $1L[gfpidx]", factoryproxyname);
		    code.endControlFlow();
		    code.beginControlFlow("if (null != $1N && $1N.length > gfpidx)", genericfieldname);
		    code.addStatement("gftype = $1L[gfpidx]", genericfieldname);
		    code.nextControlFlow("else");
		    code.addStatement("throw new RetrieveNonVolatileEntityError(\"No Generic Field Type Info.\")");
		    code.endControlFlow();
		    code.addStatement("$1N = new $2T(proxy, gftype, $8L, $9L, $3N, $4N, $5N, $6N.get() + $7L)", 
				      dynfieldinfo.name, dynfieldinfo.type, 
				      allocname, unsafename, autoreclaimname,	holdername, dynfieldinfo.fieldoff,
				      dynfieldinfo.efproxiesname, dynfieldinfo.gftypesname);
		    code.endControlFlow();
		    code.beginControlFlow("if (null != $1L)", dynfieldinfo.name);
		    code.addStatement("$1N.set($2L, $3L)", dynfieldinfo.name, arg0, arg1);
		    code.nextControlFlow("else");
		    code.addStatement("throw new RetrieveNonVolatileEntityError(\"GenericField is null!\")");
		    code.endControlFlow();
		} else {
		    code.beginControlFlow("if ($1L && null != $2L())", arg1, gsetterName(name, true));
		    code.addStatement("$1N.destroy()", dynfieldinfo.name);
		    code.addStatement("$1N = null", dynfieldinfo.name);
		    code.addStatement("$1N.putAddress($2N.get() + $3L, 0L)", 
				      unsafename, holdername, dynfieldinfo.fieldoff);
		    code.endControlFlow();
		    code.addStatement("$1N = $2L", dynfieldinfo.name, arg0);
		    code.addStatement("$1N.putLong($2N.get() + $3L, null == $4N ? 0L : $4N.getNonVolatileHandler())", 
				      unsafename, holdername, dynfieldinfo.fieldoff, dynfieldinfo.name);
		}
	    }
	    typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
	}
    }
	
    protected void buildDurableMethodSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
	MethodInfo methodinfo;
	CodeBlock.Builder code;
	FieldInfo dynfieldinfo;
	String holdername = m_fieldsinfo.get("holder").name;
	String allocname = m_fieldsinfo.get("allocator").name;
	String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
	for (String name : m_durablemtdinfo.keySet()) {
	    methodinfo = m_durablemtdinfo.get(name);
	    code = CodeBlock.builder();
	    switch(name) {
	    case "cancelAutoReclaim" :
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
	    case "registerAutoReclaim" :
		code.addStatement("$1N.registerAutoReclaim()", holdername);
		for (String fname : m_dynfieldsinfo.keySet()) {
		    dynfieldinfo = m_dynfieldsinfo.get(fname);
		    if (!isUnboxPrimitive(dynfieldinfo.type)) {
			code.beginControlFlow("if (null != $1N)", dynfieldinfo.name);
			code.addStatement("$1N.registerAutoReclaim()", dynfieldinfo.name);
			code.endControlFlow();
		    }
		}
		code.addStatement("$1N = true", autoreclaimname);
		break;
	    case "getNonVolatileHandler" :
		code.addStatement("return $1N.getChunkHandler($2N)", allocname, holdername);
		break;
	    case "autoReclaim" :
		code.addStatement("return $1N", autoreclaimname);
		break;
	    case "destroy" :
		code.addStatement("$1N.destroy()", holdername);
		for (String fname : m_dynfieldsinfo.keySet()) {
		    dynfieldinfo = m_dynfieldsinfo.get(fname);
		    if (!isUnboxPrimitive(dynfieldinfo.type)) {
			code.beginControlFlow("if (null != $1N)", dynfieldinfo.name);
			code.addStatement("$1N.destroy()", dynfieldinfo.name);
			code.addStatement("$1N = null", dynfieldinfo.name);
			code.endControlFlow();
		    }
		}				
		break;
	    default:
		throw new AnnotationProcessingException(null, "Method %s is not supported.",
							name);
	    }
	    typespecbuilder.addMethod(methodinfo.specbuilder.addCode(code.build()).build());
	}
    }
	
    protected void buildEntityMethodSpecs(TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
	MethodInfo methodinfo;
	CodeBlock.Builder code;
	FieldInfo dynfieldinfo;
	VariableElement arg0, arg1, arg2, arg3, arg4;
	String unsafename = m_fieldsinfo.get("unsafe").name;
	String holdername = m_fieldsinfo.get("holder").name;
	String allocname = m_fieldsinfo.get("allocator").name;
	String autoreclaimname = m_fieldsinfo.get("autoreclaim").name;
	String factoryproxyname = m_fieldsinfo.get("factoryproxy").name;
	String genericfieldname = m_fieldsinfo.get("genericfield").name;
	for (String name : m_entitymtdinfo.keySet()) {
	    methodinfo = m_entitymtdinfo.get(name);
	    code = CodeBlock.builder();
	    arg0 = methodinfo.elem.getParameters().get(0);
	    arg1 = methodinfo.elem.getParameters().get(1);
	    arg2 = methodinfo.elem.getParameters().get(2);
	    switch(name) {
	    case "initializeNonVolatileEntity" :
		arg3 = methodinfo.elem.getParameters().get(3);
		code.addStatement("$1N = $2L", allocname, arg0);
		code.addStatement("$1N = $2L", factoryproxyname, arg1);
		code.addStatement("$1N = $2L", genericfieldname, arg2);
		code.addStatement("$1N = $2L", autoreclaimname, arg3);
		code.beginControlFlow("try");
		code.addStatement("$1N = $2T.getUnsafe()", unsafename, Utils.class);
		code.nextControlFlow("catch (Exception e)");
		code.addStatement("e.printStackTrace()");
		code.endControlFlow();
		break;
	    case "createNonVolatileEntity" :
		arg3 = methodinfo.elem.getParameters().get(3);
		code.addStatement("initializeNonVolatileEntity($1L, $2L, $3L, $4L)", arg0, arg1, arg2, arg3);
		code.addStatement("$1N = $2N.createChunk($3L, $4N)", 
				  holdername, allocname, m_holdersize, autoreclaimname);
		code.beginControlFlow("if (null == $1N)", holdername);
		code.addStatement("throw new OutOfPersistentMemory(\"Create Non-Volatile Entity Error!\")");
		code.endControlFlow();
		//				code.beginControlFlow("try");
		//				for (String fname : m_dynfieldsinfo.keySet()) {
		//					dynfieldinfo = m_dynfieldsinfo.get(fname);
		//					if (isUnboxPrimitive(dynfieldinfo.type)) {
		//						code.addStatement("$1N($2L)", gsetterName(fname, false), getIntialValueLiteral(dynfieldinfo.type));
		//					} else {
		//						code.addStatement("$1N(null, false)", gsetterName(fname, false));
		//					}
		//				}
		//				code.nextControlFlow("catch(RetrieveNonVolatileEntityError ex)");
		//				code.endControlFlow();
		code.addStatement("initializeAfterCreate()");
		break;
	    case "restoreNonVolatileEntity" :
		arg3 = methodinfo.elem.getParameters().get(3);
		arg4 = methodinfo.elem.getParameters().get(4);
		code.addStatement("initializeNonVolatileEntity($1L, $2L, $3L, $4L)", arg0, arg1, arg2, arg4);
		code.beginControlFlow("if (0L == $1L)", arg3);
		code.addStatement("throw new RetrieveNonVolatileEntityError(\"Input handler is null on $1N.\")", name);
		code.endControlFlow();
		code.addStatement("$1N = $2N.retrieveChunk($3L, $4N)", 
				  holdername, allocname, arg3, autoreclaimname);
		code.beginControlFlow("if (null == $1N)", holdername);
		code.addStatement("throw new RetrieveNonVolatileEntityError(\"Retrieve Entity Failure!\")");
		code.endControlFlow();
		code.addStatement("initializeAfterRestore()");
		break;
	    default:
		throw new AnnotationProcessingException(null, "Method %s is not supported.",
							name);
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
	    }
	}
    }
	
    protected void buildFactoryMethodSpecs(TypeSpec entityspec, TypeSpec.Builder typespecbuilder) throws AnnotationProcessingException {
	MethodSpec methodspec;
	CodeBlock code;
		
	TypeName entitytn= ParameterizedTypeName.get(ClassName.get(m_packagename, m_entityname), entityspec.typeVariables.toArray(new TypeVariableName[0]));
		
	ParameterSpec allocparam = ParameterSpec.builder(m_alloctypename, "allocator").build();		
	code = CodeBlock.builder()
	    .addStatement("return create($1L, false)", allocparam.name)
	    .build();
	methodspec = MethodSpec.methodBuilder("create")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(OutOfPersistentMemory.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);
		
	ParameterSpec autoreclaimparam = ParameterSpec.builder(TypeName.BOOLEAN, "autoreclaim").build();
	code = CodeBlock.builder()
	    .addStatement("return create($1L, null, null, $2L)", allocparam.name, autoreclaimparam.name)
	    .build();
	methodspec = MethodSpec.methodBuilder("create")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(OutOfPersistentMemory.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addParameter(autoreclaimparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);
		
	ParameterSpec factoryproxysparam = ParameterSpec.builder(m_factoryproxystypename, "factoryproxys").build();
	ParameterSpec gfieldsparam = ParameterSpec.builder(m_gfieldstypename, "gfields").build();
	code = CodeBlock.builder()
	    .addStatement("$1T entity = new $1T()", entitytn)
	    .addStatement("entity.setupGenericInfo($1N, $2N)", factoryproxysparam.name, gfieldsparam.name)
	    .addStatement("entity.createNonVolatileEntity($1L, $2L, $3L, $4L)", 
			  allocparam.name, factoryproxysparam.name, gfieldsparam.name, autoreclaimparam.name)
	    .addStatement("return entity")
	    .build();
	methodspec = MethodSpec.methodBuilder("create")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(OutOfPersistentMemory.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addParameter(factoryproxysparam)
	    .addParameter(gfieldsparam)
	    .addParameter(autoreclaimparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);

	ParameterSpec phandlerparam = ParameterSpec.builder(TypeName.LONG, "phandler").build();
	code = CodeBlock.builder()
	    .addStatement("return restore($1L, $2L, false)", allocparam.name, phandlerparam.name)
	    .build();
	methodspec = MethodSpec.methodBuilder("restore")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(RetrieveNonVolatileEntityError.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addParameter(phandlerparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);
		
	code = CodeBlock.builder()
	    .addStatement("return restore($1L, null, null, $2L, $3L)", allocparam.name, phandlerparam.name, autoreclaimparam.name)
	    .build();
	methodspec = MethodSpec.methodBuilder("restore")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(RetrieveNonVolatileEntityError.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addParameter(phandlerparam)
	    .addParameter(autoreclaimparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);
		
	code = CodeBlock.builder()
	    .addStatement("$1T entity = new $1T()", entitytn)
	    .addStatement("entity.setupGenericInfo($1N, $2N)", factoryproxysparam.name, gfieldsparam.name)
	    .addStatement("entity.restoreNonVolatileEntity($1L, $2L, $3L, $4L, $5L)", 
			  allocparam.name, factoryproxysparam.name, gfieldsparam.name, phandlerparam.name, autoreclaimparam.name)
	    .addStatement("return entity")
	    .build();
	methodspec = MethodSpec.methodBuilder("restore")
	    .addTypeVariables(entityspec.typeVariables)
	    .addException(RetrieveNonVolatileEntityError.class)
	    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
	    .returns(TypeName.get(m_elem.asType()))
	    .addParameter(allocparam)
	    .addParameter(factoryproxysparam)
	    .addParameter(gfieldsparam)
	    .addParameter(phandlerparam)
	    .addParameter(autoreclaimparam)
	    .addCode(code).build();
	typespecbuilder.addMethod(methodspec);
    }

    public void generateCode(Filer filer) throws IOException, AnnotationProcessingException {
	AnnotationSpec classannotation = AnnotationSpec.builder(SuppressWarnings.class)
	    .addMember("value", "$S", "restriction").build();

	TypeSpec.Builder entitybuilder = TypeSpec.classBuilder(m_entityname)
	    .superclass(TypeName.get(m_elem.asType()))
	    .addModifiers(Modifier.PUBLIC).addAnnotation(classannotation)
	    .addSuperinterface(
			       ParameterizedTypeName.get(ClassName.get(MemoryNonVolatileEntity.class), m_alloctypevarname))
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
		
	TypeSpec entityspec = entitybuilder.build();

	JavaFile entityFile = JavaFile
	    .builder(m_packagename, entityspec).build();

	entityFile.writeTo(filer);
		
	TypeSpec.Builder factorybuilder = TypeSpec.classBuilder(m_factoryname)
	    .addModifiers(Modifier.PUBLIC);
		
	buildFactoryMethodSpecs(entityspec, factorybuilder);
		
	JavaFile factoryFile = JavaFile
	    .builder(m_packagename, factorybuilder.build()).build();

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
	    if (annotationSpec.type.equals(Override.class))
		continue;
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
