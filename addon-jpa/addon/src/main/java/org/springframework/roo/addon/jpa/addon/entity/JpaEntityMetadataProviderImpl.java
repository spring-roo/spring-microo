package org.springframework.roo.addon.jpa.addon.entity;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COLUMN_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ENUMERATED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.LOB_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_MUTATOR_METHOD;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED_ID;
import static org.springframework.roo.model.JpaJavaType.ENUMERATED;
import static org.springframework.roo.model.JpaJavaType.ID;
import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.JpaJavaType.VERSION;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.jpa.addon.identifier.Identifier;
import org.springframework.roo.addon.jpa.addon.identifier.IdentifierMetadata;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.Matcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.MidTypeMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The {@link JpaEntityMetadataProvider} implementation.
 *
 * @author Andrew Swan
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @author Juan Carlos Garc??a
 * @since 1.2.0
 */
@Component
@Service
public class JpaEntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider
    implements JpaEntityMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JpaEntityMetadataProviderImpl.class);

  // JPA-related field matchers
  private static final FieldMatcher JPA_COLUMN_FIELD_MATCHER = new FieldMatcher(COLUMN_FIELD,
      AnnotationMetadataBuilder.getInstance(COLUMN));
  private static final FieldMatcher JPA_EMBEDDED_FIELD_MATCHER = new FieldMatcher(EMBEDDED_FIELD,
      AnnotationMetadataBuilder.getInstance(EMBEDDED));
  private static final FieldMatcher JPA_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(
      EMBEDDED_ID_FIELD, AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
  private static final FieldMatcher JPA_ENUMERATED_FIELD_MATCHER = new FieldMatcher(
      ENUMERATED_FIELD, AnnotationMetadataBuilder.getInstance(ENUMERATED));
  private static final FieldMatcher JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(
      IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(ID),
      AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
  private static final FieldMatcher JPA_ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD,
      AnnotationMetadataBuilder.getInstance(ID));
  private static final FieldMatcher JPA_LOB_FIELD_MATCHER = new FieldMatcher(LOB_FIELD,
      AnnotationMetadataBuilder.getInstance(LOB));
  private static final FieldMatcher JPA_MANY_TO_MANY_FIELD_MATCHER = new FieldMatcher(
      MANY_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance(MANY_TO_MANY));
  private static final FieldMatcher JPA_MANY_TO_ONE_FIELD_MATCHER = new FieldMatcher(
      MANY_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance(MANY_TO_ONE));
  private static final FieldMatcher JPA_ONE_TO_MANY_FIELD_MATCHER = new FieldMatcher(
      ONE_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance(ONE_TO_MANY));
  private static final FieldMatcher JPA_ONE_TO_ONE_FIELD_MATCHER = new FieldMatcher(
      ONE_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance(ONE_TO_ONE));
  private static final FieldMatcher JPA_TRANSIENT_FIELD_MATCHER = new FieldMatcher(TRANSIENT_FIELD,
      AnnotationMetadataBuilder.getInstance(TRANSIENT));
  private static final FieldMatcher JPA_VERSION_FIELD_MATCHER = new FieldMatcher(VERSION_FIELD,
      AnnotationMetadataBuilder.getInstance(VERSION));
  private static final String PROVIDES_TYPE_STRING = JpaEntityMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  // The order of this array is the order in which we look for annotations. We
  // use the values of the first one found.
  private static final JavaType[] TRIGGER_ANNOTATIONS = {
  // We trigger off RooJpaEntity
  ROO_JPA_ENTITY};

  private CustomDataKeyDecorator customDataKeyDecorator;
  private ProjectOperations projectOperations;

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#TRIGGER_ANNOTATIONS} as additional
   * JavaType that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, null,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), PROVIDES_TYPE);
    this.registryTracker.open();
    addMetadataTriggers(TRIGGER_ANNOTATIONS);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), getMatchers());
    this.keyDecoratorTracker.open();
  }

  /**
   * This service is being deactivated so unregister upstream-downstream
   * dependencies, triggers, matchers and listeners.
   *
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), PROVIDES_TYPE);
    this.registryTracker.close();
    removeMetadataTriggers(TRIGGER_ANNOTATIONS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = getType(metadataIdentificationString);
    final LogicalPath path =
        PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
            metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  /**
   * Returns the {@link Identifier} for the entity identified by the given
   * metadata ID.
   *
   * @param metadataIdentificationString
   * @return <code>null</code> if there isn't one
   */
  private Identifier getIdentifier(final String metadataIdentificationString) {
    final JavaType entity = getType(metadataIdentificationString);
    final List<Identifier> identifiers = getIdentifiersForType(entity);
    if (CollectionUtils.isEmpty(identifiers)) {
      return null;
    }
    // We have potential identifier information from an IdentifierService.
    // We only use this identifier information if the user did NOT provide
    // ANY identifier-related attributes on @RooJpaEntity....
    Validate
        .isTrue(
            identifiers.size() == 1,
            "Identifier service indicates %d fields illegally for the entity '%s' (should only be one identifier field given this is an entity, not an Identifier class)",
            identifiers.size(), entity.getSimpleTypeName());
    return identifiers.iterator().next();
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Entity";
  }

  /**
   * Returns the {@link JpaEntityAnnotationValues} for the given domain type
   *
   * @param governorPhysicalType (required)
   * @return a non-<code>null</code> instance
   */
  private JpaEntityAnnotationValues getJpaEntityAnnotationValues(
      final PhysicalTypeMetadata governorPhysicalType) {
    for (final JavaType triggerAnnotation : TRIGGER_ANNOTATIONS) {
      final JpaEntityAnnotationValues annotationValues =
          new JpaEntityAnnotationValues(governorPhysicalType, triggerAnnotation);
      if (annotationValues.isAnnotationFound()) {
        return annotationValues;
      }
    }
    throw new IllegalStateException(getClass().getSimpleName()
        + " was triggered but not by any of " + Arrays.toString(TRIGGER_ANNOTATIONS));
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {

    if (projectOperations == null) {
      projectOperations = getProjectOperations();
    }
    Validate.notNull(projectOperations, "ProjectOperations is required");

    // Find out the entity-level JPA details from the trigger annotation
    final JpaEntityAnnotationValues jpaEntityAnnotationValues =
        getJpaEntityAnnotationValues(governorPhysicalType);

    /*
     * Walk the inheritance hierarchy for any existing JpaEntityMetadata. We
     * don't need to monitor any such parent, as any changes to its Java
     * type will trickle down to the governing java type.
     */
    final JpaEntityMetadata parent =
        getParentMetadata(governorPhysicalType.getMemberHoldingTypeDetails());

    // Get the governor's members
    final MemberDetails governorMemberDetails = getMemberDetails(governorPhysicalType);

    final String moduleName =
        PhysicalTypeIdentifierNamingUtils.getPath(metadataIdentificationString).getModule();
    if (projectOperations.isProjectAvailable(moduleName)) {
      // If the project itself changes, we want a chance to refresh this
      // item
      getMetadataDependencyRegistry().registerDependency(
          ProjectMetadata.getProjectIdentifier(moduleName), metadataIdentificationString);
    }

    // Getting entity details
    JavaType entity = JpaEntityMetadata.getJavaType(metadataIdentificationString);
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);

    // Getting JavaBeanMetadata
    String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);
    JavaBeanMetadata entityJavaBeanMetadata = getMetadataService().get(javaBeanMetadataKey);

    // This metadata is not available yet
    if (entityJavaBeanMetadata == null) {
      return null;
    }

    // Locate relation fields to process
    List<FieldMetadata> fieldsParent = new ArrayList<FieldMetadata>();
    Map<String, FieldMetadata> relationsAsChild = new HashMap<String, FieldMetadata>();

    for (FieldMetadata field : entityDetails.getDeclaredFields()) {
      if (field.getAnnotation(RooJavaType.ROO_JPA_RELATION) != null) {
        fieldsParent.add(field);
      } else if (field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null
          || field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
          || field.getAnnotation(JpaJavaType.MANY_TO_MANY) != null) {
        relationsAsChild.put(field.getFieldName().getSymbolName(), field);
      }
    }

    // Check if it's a child part of a composition
    FieldMetadata compositionRelationField;
    try {
      compositionRelationField =
          getCompositionRelationField(entity, entityDetails, relationsAsChild);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "Problems found when trying to identify composition relationship", e);
    }

    // Getting identifier field and version field and its accessors
    FieldMetadata identifierField = null;
    MethodMetadata identifierAccessor = null;
    FieldMetadata versionField = null;
    MethodMetadata versionAccessor = null;
    if (parent == null) {

      // Obtain identifier field from entity details
      List<FieldMetadata> identifierFields = entityDetails.getFieldsWithAnnotation(ID);
      List<FieldMetadata> embeddedIdentifierFields =
          entityDetails.getFieldsWithAnnotation(EMBEDDED_ID);

      Validate.isTrue(!(identifierFields.isEmpty() && embeddedIdentifierFields.isEmpty()), String
          .format("ERROR: The annotated entity '%s' doesn't contain any identifier field.",
              entityDetails.getType().getFullyQualifiedTypeName()));

      if (!identifierFields.isEmpty()) {
        identifierField = identifierFields.get(0);
      } else if (!embeddedIdentifierFields.isEmpty()) {
        identifierField = embeddedIdentifierFields.get(0);
      }

      identifierAccessor = entityJavaBeanMetadata.getAccesorMethod(identifierField);

      // Obtain version field from entity details
      List<FieldMetadata> versionFields = entityDetails.getFieldsWithAnnotation(VERSION);

      // Check and add version field
      if (!versionFields.isEmpty()) {
        versionField = versionFields.get(0);
        versionAccessor = entityJavaBeanMetadata.getAccesorMethod(versionField);
      }

    } else {
      identifierField = parent.getCurrentIndentifierField();
      versionField = parent.getCurrentVersionField();
    }

    return new JpaEntityMetadata(metadataIdentificationString, aspectName, governorPhysicalType,
        parent, governorMemberDetails, identifierField, identifierAccessor, versionField,
        versionAccessor, jpaEntityAnnotationValues, entityDetails, fieldsParent, relationsAsChild,
        compositionRelationField);
  }

  /**
   * Gets {@link FieldMetadata} of entity field which declares a composition relationship
   *
   * @param entity
   * @param entityDetails
   * @param relationsAsChild
   * @return
   * @throws ClassNotFoundException
   */
  private FieldMetadata getCompositionRelationField(JavaType entity,
      ClassOrInterfaceTypeDetails entityDetails, Map<String, FieldMetadata> relationsAsChild)
      throws ClassNotFoundException {
    // Try to identify if it's is a child part of a composition.
    // It uses details and annotation values instead metadata to
    // avoid problems of circular dependencies
    ClassOrInterfaceTypeDetails parentDatils;
    FieldMetadata compositionRelationField = null;
    AnnotationMetadata parentFieldRelationAnnotation;
    JpaRelationType type;
    String parentMappedBy;
    for (FieldMetadata field : relationsAsChild.values()) {
      parentDatils = getTypeLocationService().getTypeDetails(field.getFieldType().getBaseType());
      for (FieldMetadata parentField : parentDatils
          .getFieldsWithAnnotation(RooJavaType.ROO_JPA_RELATION)) {
        parentFieldRelationAnnotation = parentField.getAnnotation(RooJavaType.ROO_JPA_RELATION);
        if (parentFieldRelationAnnotation != null
            && entity.equals(parentField.getFieldType().getBaseType())) {
          parentMappedBy = getFieldMappedByAnnotationValue(parentField);
          if (field.getFieldName().getSymbolName().equals(parentMappedBy)) {
            // Found parent relation field
            // Check composition
            EnumDetails value =
                ((EnumAttributeValue) parentFieldRelationAnnotation
                    .getAttribute(new JavaSymbolName("type"))).getValue();
            if (JpaRelationType.COMPOSITION.name().equals(value.getField().getSymbolName())) {
              // Found composition
              if (compositionRelationField != null) {
                throw new IllegalArgumentException(
                    String
                        .format(
                            "Found to relations which '%s' is child part of composition relation field: '%s' and '%s'",
                            entity.getFullyQualifiedTypeName(),
                            compositionRelationField.getFieldName(), field.getFieldName()));
              }
              compositionRelationField = field;
            }
          }
        }
      }
    }
    return compositionRelationField;
  }

  /**
   * Return _mappedBy_ annotation attribute value of Jpa-relation-definition annotation
   *
   * @param parentField
   * @return
   */
  private String getFieldMappedByAnnotationValue(FieldMetadata parentField) {
    AnnotationMetadata annotation = null;
    for (JavaType jpaAnnotation : Arrays.asList(JpaJavaType.ONE_TO_MANY, JpaJavaType.ONE_TO_ONE,
        JpaJavaType.MANY_TO_MANY)) {
      annotation = parentField.getAnnotation(jpaAnnotation);
      if (annotation != null) {
        break;
      }
    }
    if (annotation != null) {
      return (String) annotation.getAttribute("mappedBy").getValue();
    }
    return null;
  }

  public String getProvidesType() {
    return PROVIDES_TYPE;
  }

  private JavaType getType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  @SuppressWarnings("unchecked")
  private Matcher<? extends CustomDataAccessor>[] getMatchers() {
    Matcher<? extends CustomDataAccessor>[] matchers =
        new Matcher[] {
            // Type matchers
            new MidTypeMatcher(IDENTIFIER_TYPE, IdentifierMetadata.class.getName()),
            new AnnotatedTypeMatcher(PERSISTENT_TYPE, ROO_JPA_ENTITY),
            // Field matchers
            JPA_COLUMN_FIELD_MATCHER,
            JPA_EMBEDDED_FIELD_MATCHER,
            JPA_EMBEDDED_ID_FIELD_MATCHER,
            JPA_ENUMERATED_FIELD_MATCHER,
            JPA_ID_FIELD_MATCHER,
            JPA_LOB_FIELD_MATCHER,
            JPA_MANY_TO_MANY_FIELD_MATCHER,
            JPA_MANY_TO_ONE_FIELD_MATCHER,
            JPA_ONE_TO_MANY_FIELD_MATCHER,
            JPA_ONE_TO_ONE_FIELD_MATCHER,
            JPA_TRANSIENT_FIELD_MATCHER,
            JPA_VERSION_FIELD_MATCHER,
            // Method matchers
            new MethodMatcher(Arrays.asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER),
                IDENTIFIER_ACCESSOR_METHOD, true),
            new MethodMatcher(Arrays.asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER),
                IDENTIFIER_MUTATOR_METHOD, false),
            new MethodMatcher(Arrays.asList(JPA_VERSION_FIELD_MATCHER), VERSION_ACCESSOR_METHOD,
                true),
            new MethodMatcher(Arrays.asList(JPA_VERSION_FIELD_MATCHER), VERSION_MUTATOR_METHOD,
                false)};
    return matchers;
  }

  public CustomDataKeyDecorator getCustomDataKeyDecorator() {
    if (customDataKeyDecorator == null) {
      // Get all Services implement CustomDataKeyDecorator interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(CustomDataKeyDecorator.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (CustomDataKeyDecorator) context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load CustomDataKeyDecorator on JpaEntityMetadataProviderImpl.");
        return null;
      }
    } else {
      return customDataKeyDecorator;
    }

  }

  public ProjectOperations getProjectOperations() {
    // Get all Services implement ProjectOperations interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(ProjectOperations.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (ProjectOperations) context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ProjectOperations on JpaEntityMetadataProviderImpl.");
      return null;
    }
  }
}
