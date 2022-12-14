package org.springframework.roo.addon.javabean.addon;

import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY;
import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY_FACTORY;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.javabean.annotations.RooEquals;
import org.springframework.roo.addon.javabean.annotations.RooJavaBean;
import org.springframework.roo.addon.javabean.annotations.RooToString;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DeclaredFieldAnnotationDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.AbstractComment;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJavaBean}.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Jose Manuel Viv??
 * @since 1.0
 */
public class JavaBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String THIS_DOT = "this.";
  private static final String RETURN_0 = "return 0;";
  private static final String PROVIDES_TYPE_STRING = JavaBeanMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }


  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  private JavaBeanAnnotationValues annotationValues;

  private Map<FieldMetadata, JavaSymbolName> declaredFields;

  private List<? extends MethodMetadata> interfaceMethods;

  private MemberDetailsScanner memberDetailsScanner;

  private Map<JavaSymbolName, MethodMetadata> accesorMethods;

  private Map<JavaSymbolName, MethodMetadata> mutatorMethods;
  private final EqualsAnnotationValues equalsAnnotationValues;
  private final List<FieldMetadata> equalsFields;
  private final FieldMetadata identifierField;
  private final ToStringAnnotationValues toStringAnnotationValues;
  private final Collection<FieldMetadata> toStringFields;
  private final JavaType target;

  /**
   * Constructor
   *
   * @param identifier the ID of the metadata to create (must be a valid ID)
   * @param aspectName the name of the ITD to be created (required)
   * @param governorPhysicalTypeMetadata the governor (required)
   * @param annotationValues the values of the {@link RooJavaBean} annotation (required)
   * @param declaredFields the fields declared in the governor (required, can be empty)
   * @param interfaceMethods
   * @param memberDetailsScanner the memberDetailsScanner used to get declared methods
   * @param equalsAnnotationValues the values of the {@link RooEquals} annotation
   * @param equalsFields the fields to use to generate equals method
   * @param identifierField the identifier field (if any)
   * @param toStringAnnotationValues the values of the {@link RooToString} annotation
   * @param toStringFields the field to use to generate toString method
   */
  public JavaBeanMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaBeanAnnotationValues annotationValues,
      final Map<FieldMetadata, JavaSymbolName> declaredFields,
      List<? extends MethodMetadata> interfaceMethods, MemberDetailsScanner memberDetailsScanner,
      EqualsAnnotationValues equalsAnnotationValues, List<FieldMetadata> equalsFields,
      FieldMetadata identifierField, ToStringAnnotationValues toStringAnnotationValues,
      List<FieldMetadata> toStringFields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate.isTrue(isValid(identifier),
        "Metadata identification string '%s' does not appear to be a valid", identifier);
    Validate.notNull(annotationValues, "Annotation values required");
    Validate.notNull(declaredFields, "Declared fields required");


    this.target = governorPhysicalTypeMetadata.getType();
    this.annotationValues = annotationValues;
    this.declaredFields = declaredFields;
    this.interfaceMethods = interfaceMethods;
    this.memberDetailsScanner = memberDetailsScanner;
    this.accesorMethods = new HashMap<JavaSymbolName, MethodMetadata>();
    this.mutatorMethods = new HashMap<JavaSymbolName, MethodMetadata>();
    this.equalsAnnotationValues = equalsAnnotationValues;
    this.equalsFields =
        equalsFields == null ? new ArrayList<FieldMetadata>() : Collections
            .unmodifiableList(equalsFields);
    this.identifierField = identifierField;
    this.toStringAnnotationValues = toStringAnnotationValues;
    this.toStringFields =
        toStringFields == null ? new ArrayList<FieldMetadata>() : Collections
            .unmodifiableCollection(toStringFields);

    if (!isValid()) {
      return;
    }



    if (!declaredFields.isEmpty()) {
      generateGettersAndSetters(declaredFields, interfaceMethods);
    }

    // Generate equals methods
    if (equalsAnnotationValues.isAnnotationFound()) {

      JavaSymbolName idField = null;
      if (identifierField != null && getAccesorMethod(identifierField) != null) {
        idField = getAccesorMethod(identifierField).getMethodName();
      }
      ensureGovernorHasMethod(EqualsMetadata.generateEqualsMethod(identifier, target,
          equalsAnnotationValues, idField, equalsFields, builder));

      ensureGovernorHasMethod(EqualsMetadata.generateHashCodeMethod(identifier,
          equalsAnnotationValues, equalsFields, builder.getImportRegistrationResolver()));

    }

    // Generate toString methods
    if (toStringAnnotationValues.isAnnotationFound()) {
      ensureGovernorHasMethod(ToStringMetadata.generateToStringMethod(identifier, target,
          toStringAnnotationValues, toStringFields));
    }

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();
  }


  protected void generateGettersAndSetters(final Map<FieldMetadata, JavaSymbolName> declaredFields,
      List<? extends MethodMetadata> interfaceMethods) {
    // Add getters and setters
    for (final Entry<FieldMetadata, JavaSymbolName> entry : declaredFields.entrySet()) {
      final FieldMetadata field = entry.getKey();
      final MethodMetadataBuilder accessorMethod = getDeclaredGetter(field);
      final MethodMetadataBuilder mutatorMethod = getDeclaredSetter(field);

      // Check to see if GAE is interested
      if (entry.getValue() != null) {
        JavaSymbolName hiddenIdFieldName;
        if (field.getFieldType().isCommonCollectionType()) {
          hiddenIdFieldName =
              governorTypeDetails.getUniqueFieldName(field.getFieldName().getSymbolName() + "Keys");
          builder.getImportRegistrationResolver().addImport(GAE_DATASTORE_KEY_FACTORY);
          builder.addField(getMultipleEntityIdField(hiddenIdFieldName));
        } else {
          hiddenIdFieldName =
              governorTypeDetails.getUniqueFieldName(field.getFieldName().getSymbolName() + "Id");
          builder.addField(getSingularEntityIdField(hiddenIdFieldName));
        }

        processGaeAnnotations(field);

        InvocableMemberBodyBuilder gaeAccessorBody = getGaeAccessorBody(field, hiddenIdFieldName);
        accessorMethod.setBodyBuilder(gaeAccessorBody);
        InvocableMemberBodyBuilder gaeMutatorBody = getGaeMutatorBody(field, hiddenIdFieldName);
        mutatorMethod.setBodyBuilder(gaeMutatorBody);
      }

      // Add to mutators and accesors list and build
      if (accessorMethod != null) {
        this.accesorMethods.put(field.getFieldName(), accessorMethod.build());
        builder.addMethod(accessorMethod);
      }
      if (mutatorMethod != null) {
        this.mutatorMethods.put(field.getFieldName(), mutatorMethod.build());
        builder.addMethod(mutatorMethod);
      }
    }

    // Implements interface methods if exists
    implementsInterfaceMethods(interfaceMethods);
  }


  protected void implementsInterfaceMethods(List<? extends MethodMetadata> interfaceMethods) {
    if (interfaceMethods != null) {
      for (MethodMetadata interfaceMethod : interfaceMethods) {
        MethodMetadataBuilder methodBuilder = getInterfaceMethod(interfaceMethod);
        // ROO-3584: JavaBean implementing Interface defining getters and setters
        // ROO-3585: If interface method already exists on type is not necessary
        // to add on ITD. Method builder will be NULL.
        if (methodBuilder != null && !checkIfInterfaceMethodWasImplemented(methodBuilder)) {
          builder.addMethod(methodBuilder);
        }
      }
    }
  }

  /**
   * Obtains the specific accessor method that is either contained within the
   * normal Java compilation unit or will be introduced by this add-on via an
   * ITD.
   *
   * @param field
   *            that already exists on the type either directly or via
   *            introduction (required; must be declared by this type to be
   *            located)
   * @return the method corresponding to an accessor, or null if not found
   */
  private MethodMetadataBuilder getDeclaredGetter(final FieldMetadata field) {
    Validate.notNull(field, "Field required");

    // Compute the mutator method name
    final JavaSymbolName methodName = BeanInfoUtils.getAccessorMethodName(field);

    // See if the type itself declared the accessor
    if (governorHasMethod(methodName)) {
      return null;
    }

    // Decide whether we need to produce the accessor method (see ROO-619
    // for reason we allow a getter for a final field)
    if (annotationValues.isGettersByDefault() && !Modifier.isTransient(field.getModifier())
        && !Modifier.isStatic(field.getModifier())) {
      final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
      String fieldName = field.getFieldName().getSymbolName();
      bodyBuilder.appendFormalLine("return this." + fieldName + ";");

      MethodMetadataBuilder methodMetadataBuilder =
          new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, field.getFieldType(),
              bodyBuilder);

      String fieldJavaDoc = getFieldJavaDocDescription(field);
      if (StringUtils.isNotBlank(fieldJavaDoc)) {
        methodMetadataBuilder.setCommentStructure(JavadocComment.create("Gets %s value\n%s",
            fieldName, fieldJavaDoc));
      } else {
        methodMetadataBuilder
            .setCommentStructure(JavadocComment.create("Gets %s value", fieldName));
      }
      return methodMetadataBuilder;
    }

    return null;
  }

  /**
   * Obtains the specific mutator method that is either contained within the
   * normal Java compilation unit or will be introduced by this add-on via an
   * ITD.
   *
   * @param field
   *            that already exists on the type either directly or via
   *            introduction (required; must be declared by this type to be
   *            located)
   * @return the method corresponding to a mutator, or null if not found
   */
  private MethodMetadataBuilder getDeclaredSetter(final FieldMetadata field) {
    Validate.notNull(field, "Field required");

    // Compute the mutator method name
    final JavaSymbolName methodName = BeanInfoUtils.getMutatorMethodName(field);

    // Compute the mutator method parameters
    final JavaType parameterType = field.getFieldType();

    // See if the type itself declared the mutator
    if (governorHasMethod(methodName, parameterType)) {
      return null;
    }

    // Compute the mutator method parameter names
    final List<JavaSymbolName> parameterNames = Arrays.asList(field.getFieldName());

    // Decide whether we need to produce the mutator method (disallowed for
    // final fields as per ROO-36)
    if (annotationValues.isSettersByDefault() && !Modifier.isTransient(field.getModifier())
        && !Modifier.isStatic(field.getModifier()) && !Modifier.isFinal(field.getModifier())) {
      final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
      String fieldName = field.getFieldName().getSymbolName();
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", fieldName, fieldName));
      bodyBuilder.appendFormalLine("return this;");

      MethodMetadataBuilder methodMetadataBuilder =
          new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
              target.withoutParameters(), AnnotatedJavaType.convertFromJavaTypes(parameterType),
              parameterNames, bodyBuilder);

      String fieldJavaDoc = getFieldJavaDocDescription(field);
      if (hasFieldJavaDoc(fieldJavaDoc)) {
        methodMetadataBuilder.setCommentStructure(JavadocComment.create(" Sets %s value\n%s",
            fieldName, fieldJavaDoc));
      } else {
        methodMetadataBuilder
            .setCommentStructure(JavadocComment.create("Sets %s value", fieldName));
      }
      return methodMetadataBuilder;
    }

    return null;
  }

  /**
   * Returns Javadoc description of a field
   *
   * @param field
   * @return
   */
  private String getFieldJavaDocDescription(FieldMetadata field) {
    if (field.getCommentStructure() == null || field.getCommentStructure().isEmpty()) {
      return "";
    }
    List<AbstractComment> comments = field.getCommentStructure().asList();
    List<String> descriptions = new ArrayList<String>();
    for (AbstractComment comment : comments) {
      if (comment instanceof JavadocComment) {
        JavadocComment javaDoc = (JavadocComment) comment;
        descriptions.add(javaDoc.getDescription());
      }
    }
    return StringUtils.join(descriptions, "\n");
  }

  private boolean hasFieldJavaDoc(String javaDoc) {
    if (StringUtils.isBlank(javaDoc)) {
      return false;
    }
    return !StringUtils.startsWith(StringUtils.replace(javaDoc, "\n", "").trim(),
        "TODO Auto-generated");
  }

  /**
   * Obtains a valid MethodMetadataBuilder with necessary configuration
   *
   * @param method
   * @return MethodMetadataBuilder
   */
  private MethodMetadataBuilder getInterfaceMethod(final MethodMetadata method) {

    // Compute the method name
    final JavaSymbolName methodName = method.getMethodName();
    // See if the type itself declared the accessor
    if (governorHasMethod(methodName)) {
      return null;
    }
    // Getting return type
    JavaType returnType = method.getReturnType();
    // Generating body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    // If return type is not primitive, return null
    if (returnType.isPrimitive()) {
      JavaType baseType = returnType.getBaseType();
      if (baseType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
        bodyBuilder.appendFormalLine("return false;");
      } else if (baseType.equals(JavaType.BYTE_PRIMITIVE)) {
        bodyBuilder.appendFormalLine(RETURN_0);
      } else if (baseType.equals(JavaType.SHORT_PRIMITIVE)) {
        bodyBuilder.appendFormalLine(RETURN_0);
      } else if (baseType.equals(JavaType.INT_PRIMITIVE)) {
        bodyBuilder.appendFormalLine(RETURN_0);
      } else if (baseType.equals(JavaType.LONG_PRIMITIVE)) {
        bodyBuilder.appendFormalLine("return 0l;");
      } else if (baseType.equals(JavaType.FLOAT_PRIMITIVE)) {
        bodyBuilder.appendFormalLine("return 0.0f;");
      } else if (baseType.equals(JavaType.DOUBLE_PRIMITIVE)) {
        bodyBuilder.appendFormalLine("return 0.00;");
      } else if (baseType.equals(JavaType.CHAR_PRIMITIVE)) {
        bodyBuilder.appendFormalLine("return '\0';");
      }
    } else {
      bodyBuilder.appendFormalLine("return null;");
    }
    return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, bodyBuilder);

  }

  private InvocableMemberBodyBuilder getEntityCollectionAccessorBody(final FieldMetadata field,
      final JavaSymbolName entityIdsFieldName) {
    final String entityCollectionName = field.getFieldName().getSymbolName();
    final String entityIdsName = entityIdsFieldName.getSymbolName();
    final String localEnitiesName = "local" + StringUtils.capitalize(entityCollectionName);

    final JavaType collectionElementType = field.getFieldType().getParameters().get(0);
    final String simpleCollectionElementTypeName = collectionElementType.getSimpleTypeName();

    JavaType collectionType = field.getFieldType();
    builder.getImportRegistrationResolver().addImport(collectionType);

    final String collectionName =
        field.getFieldType().getNameIncludingTypeParameters()
            .replace(field.getFieldType().getPackage().getFullyQualifiedPackageName() + ".", "");
    String instantiableCollection = collectionName;

    // GAE only supports java.util.List and java.util.Set collections and we
    // need a concrete implementation of either.
    if (collectionType.getFullyQualifiedTypeName().equals(LIST.getFullyQualifiedTypeName())) {
      collectionType =
          new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
              collectionType.getParameters());
      instantiableCollection =
          collectionType.getNameIncludingTypeParameters().replace(
              collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
    } else if (collectionType.getFullyQualifiedTypeName().equals(SET.getFullyQualifiedTypeName())) {
      collectionType =
          new JavaType(HASH_SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
              collectionType.getParameters());
      instantiableCollection =
          collectionType.getNameIncludingTypeParameters().replace(
              collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
    }

    builder.getImportRegistrationResolver().addImport(collectionType);

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName + " = new "
        + instantiableCollection + "();");
    bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(simpleCollectionElementTypeName + " entity = "
        + simpleCollectionElementTypeName + ".find" + simpleCollectionElementTypeName
        + "(key.getId());");
    bodyBuilder.appendFormalLine("if (entity != null) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(THIS_DOT + entityCollectionName + " = " + localEnitiesName + ";");
    bodyBuilder.appendFormalLine("return " + localEnitiesName + ";");

    return bodyBuilder;
  }

  private InvocableMemberBodyBuilder getEntityCollectionMutatorBody(final FieldMetadata field,
      final JavaSymbolName entityIdsFieldName) {
    final String entityCollectionName = field.getFieldName().getSymbolName();
    final String entityIdsName = entityIdsFieldName.getSymbolName();
    final JavaType collectionElementType = field.getFieldType().getParameters().get(0);
    final String localEnitiesName = "local" + StringUtils.capitalize(entityCollectionName);

    JavaType collectionType = field.getFieldType();
    builder.getImportRegistrationResolver().addImport(collectionType);

    final String collectionName =
        field.getFieldType().getNameIncludingTypeParameters()
            .replace(field.getFieldType().getPackage().getFullyQualifiedPackageName() + ".", "");
    String instantiableCollection = collectionName;
    if (collectionType.getFullyQualifiedTypeName().equals(LIST.getFullyQualifiedTypeName())) {
      collectionType =
          new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
              collectionType.getParameters());
      instantiableCollection =
          collectionType.getNameIncludingTypeParameters().replace(
              collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
    } else if (collectionType.getFullyQualifiedTypeName().equals(SET.getFullyQualifiedTypeName())) {
      collectionType =
          new JavaType(HASH_SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
              collectionType.getParameters());
      instantiableCollection =
          collectionType.getNameIncludingTypeParameters().replace(
              collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
    }

    builder.getImportRegistrationResolver().addImports(collectionType, LIST, ARRAY_LIST);

    final String identifierMethodName = getIdentifierMethodName(field).getSymbolName();

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName + " = new "
        + instantiableCollection + "();");
    bodyBuilder.appendFormalLine("List<Long> longIds = new ArrayList<Long>();");
    bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("if (!longIds.contains(key.getId())) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("longIds.add(key.getId());");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine("for (" + collectionElementType.getSimpleTypeName() + " entity : "
        + entityCollectionName + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("if (!longIds.contains(entity." + identifierMethodName + "())) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("longIds.add(entity." + identifierMethodName + "());");
    bodyBuilder.appendFormalLine(entityIdsName + ".add(KeyFactory.createKey("
        + collectionElementType.getSimpleTypeName() + ".class.getName(), entity."
        + identifierMethodName + "()));");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(THIS_DOT + entityCollectionName + " = " + localEnitiesName + ";");

    return bodyBuilder;
  }

  private InvocableMemberBodyBuilder getGaeAccessorBody(final FieldMetadata field,
      final JavaSymbolName hiddenIdFieldName) {
    return field.getFieldType().isCommonCollectionType() ? getEntityCollectionAccessorBody(field,
        hiddenIdFieldName) : getSingularEntityAccessor(field, hiddenIdFieldName);
  }

  private InvocableMemberBodyBuilder getGaeMutatorBody(final FieldMetadata field,
      final JavaSymbolName hiddenIdFieldName) {
    return field.getFieldType().isCommonCollectionType() ? getEntityCollectionMutatorBody(field,
        hiddenIdFieldName) : getSingularEntityMutator(field, hiddenIdFieldName);
  }

  private InvocableMemberBodyBuilder getInterfaceMethodBody(JavaType returnType) {
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("// Interface Implementation");
    bodyBuilder.appendFormalLine(RETURN_0);
    return bodyBuilder;
  }

  private JavaSymbolName getIdentifierMethodName(final FieldMetadata field) {
    final JavaSymbolName identifierAccessorMethodName = declaredFields.get(field);
    return identifierAccessorMethodName != null ? identifierAccessorMethodName
        : new JavaSymbolName("getId");
  }

  private FieldMetadataBuilder getMultipleEntityIdField(final JavaSymbolName fieldName) {
    builder.getImportRegistrationResolver().addImport(HASH_SET);
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, new JavaType(
        SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
        Collections.singletonList(GAE_DATASTORE_KEY)), "new HashSet<Key>()");
  }

  private InvocableMemberBodyBuilder getSingularEntityAccessor(final FieldMetadata field,
      final JavaSymbolName hiddenIdFieldName) {
    final String entityName = field.getFieldName().getSymbolName();
    final String entityIdName = hiddenIdFieldName.getSymbolName();
    final String simpleFieldTypeName = field.getFieldType().getSimpleTypeName();

    final String identifierMethodName = getIdentifierMethodName(field).getSymbolName();

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("if (this." + entityIdName + " != null) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("if (this." + entityName + " == null || this." + entityName + "."
        + identifierMethodName + "() != this." + entityIdName + ") {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(THIS_DOT + entityName + " = " + simpleFieldTypeName + ".find"
        + simpleFieldTypeName + "(this." + entityIdName + ");");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("} else {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(THIS_DOT + entityName + " = null;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine("return this." + entityName + ";");

    return bodyBuilder;
  }

  private FieldMetadataBuilder getSingularEntityIdField(final JavaSymbolName fieldName) {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, LONG_OBJECT, null);
  }

  private InvocableMemberBodyBuilder getSingularEntityMutator(final FieldMetadata field,
      final JavaSymbolName hiddenIdFieldName) {
    final String entityName = field.getFieldName().getSymbolName();
    final String entityIdName = hiddenIdFieldName.getSymbolName();
    final String identifierMethodName = getIdentifierMethodName(field).getSymbolName();

    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("if (" + entityName + " != null) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("if (" + entityName + "." + identifierMethodName
        + " () == null) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(entityName + ".persist();");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(THIS_DOT + entityIdName + " = " + entityName + "."
        + identifierMethodName + "();");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("} else {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(THIS_DOT + entityIdName + " = null;");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.appendFormalLine(THIS_DOT + entityName + " = " + entityName + ";");

    return bodyBuilder;
  }

  private void processGaeAnnotations(final FieldMetadata field) {
    for (final AnnotationMetadata annotation : field.getAnnotations()) {
      if (annotation.getAnnotationType().equals(ONE_TO_ONE)
          || annotation.getAnnotationType().equals(MANY_TO_ONE)
          || annotation.getAnnotationType().equals(ONE_TO_MANY)
          || annotation.getAnnotationType().equals(MANY_TO_MANY)) {
        builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(field,
            new AnnotationMetadataBuilder(annotation.getAnnotationType()).build(), true));
        builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(field,
            new AnnotationMetadataBuilder(TRANSIENT).build()));
        break;
      }
    }
  }

  /**
   * To check if current method was implemented on all Java classes or ITds
   * associated to this entity class.
   * If method was implemented, is not necessary to add again.
   *
   * @param methodBuilder
   * @return
   */
  private boolean checkIfInterfaceMethodWasImplemented(MethodMetadataBuilder methodBuilder) {

    // ROO-3584: Obtain current declared methods
    List<MethodMetadataBuilder> declaredMethods = builder.getDeclaredMethods();
    for (MethodMetadataBuilder method : declaredMethods) {
      // If current method equals to interface method, return false
      if (method.getMethodName().equals(methodBuilder.getMethodName())) {
        return true;
      }
    }

    // ROO-3587: Obtain ALL declared methods from Java classes and ITDs.
    MemberDetails memberDetails =
        memberDetailsScanner.getMemberDetails(getClass().getName(), governorTypeDetails);
    List<MethodMetadata> allMethods = memberDetails.getMethods();

    for (MethodMetadata method : allMethods) {
      // If current method equals to interface method, return false
      if (method.getMethodName().equals(methodBuilder.getMethodName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a list with the mutator methods of the class requesting the metadata
   *
   * @return List<MethodMetadata> with mutator methods of the class
   */
  public List<MethodMetadata> getMutatorMethods() {
    return (List<MethodMetadata>) this.mutatorMethods.values();
  }

  /**
   * Returns the mutator method related with the provided field
   *
   * @return MethodMetadata with the mutator method of the field
   */
  @Override
  public MethodMetadata getMutatorMethod(FieldMetadata field) {
    return mutatorMethods.get(field.getFieldName());
  }

  /**
   * Returns a list with the accesor methods of the class requesting the metadata
   *
   * @return List<MethodMetadata> with accesor methods of the class
   */
  public List<MethodMetadata> getAccesorMethods() {
    return (List<MethodMetadata>) this.accesorMethods.values();
  }

  /**
   * Returns the accesor method related with the provided field
   *
   * @return MethodMetadata with the accesor method of the field
   */
  public MethodMetadata getAccesorMethod(FieldMetadata field) {
    return accesorMethods.get(field.getFieldName());
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }
}
