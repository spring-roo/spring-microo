package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.shell.OptionContexts.PROJECT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.converters.JavaPackageConverter;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CliOptionAutocompleteIndicator;
import org.springframework.roo.shell.CliOptionMandatoryIndicator;
import org.springframework.roo.shell.CliOptionVisibilityIndicator;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class defines available commands to manage service layer. Allows to
 * create new service related with some specific entity or generate services for
 * every entity defined on generated project. Always generates interface and its
 * implementation
 *
 * @author Stefan Schmidt
 * @author Juan Carlos Garc??a
 * @since 1.2.0
 */
@Component
@Service
public class ServiceCommands implements CommandMarker {

  private static final Logger LOGGER = HandlerUtils.getLogger(ServiceCommands.class);

  //------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  private ServiceOperations serviceOperations;
  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private RepositoryJpaLocator repositoryJpaLocator;

  private Converter<JavaType> javaTypeConverter;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }


  @CliAvailabilityIndicator({"service"})
  public boolean isServiceCommandAvailable() {
    return serviceOperations.areServiceCommandsAvailable();
  }

  // ROO-3717: Service secure methods will be updated on future commits
  /*@CliAvailabilityIndicator({ "service secure type", "service secure all" })
  public boolean isSecureServiceCommandAvailable() {
      return serviceOperations.isSecureServiceInstallationPossible();
  }*/

  @CliOptionVisibilityIndicator(
      command = "service",
      params = {"interface", "class", "repository"},
      help = "--repository, --interface and --class parameters are not available if you don't specify --entity parameter")
  public boolean areIterfaceAndClassVisible(ShellContext shellContext) {

    // Get all defined parameters
    Map<String, String> parameters = shellContext.getParameters();

    // If --entity and --repository have been defined, show --class and --interface parameters
    if (parameters.containsKey("entity") && StringUtils.isNotBlank(parameters.get("entity"))) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "service", params = "repository")
  public boolean isRepositoryParameterMandatory(ShellContext shellContext) {
    if (shellContext.getParameters().containsKey("entity")
        && projectOperations.isMultimoduleProject()) {
      return true;
    }
    return false;
  }

  @CliOptionMandatoryIndicator(command = "service", params = "interface")
  public boolean isInterfaceParameterMandatory(ShellContext shellContext) {
    if (shellContext.getParameters().containsKey("repository")
        && projectOperations.isMultimoduleProject()) {
      return true;
    }
    return false;
  }

  @CliOptionAutocompleteIndicator(
      command = "service",
      param = "repository",
      help = "--repository parameter must  be the repository associated to the entity specified in --entity parameter. Please, write a valid value using autocomplete feature (TAB or CTRL + Space)")
  public List<String> returnRepositories(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("repository");

    List<String> allPossibleValues = new ArrayList<String>();

    // Get all defined parameters
    Map<String, String> contextParameters = shellContext.getParameters();

    // Getting entity
    String entity = contextParameters.get("entity");

    if (StringUtils.isNotBlank(entity)) {

      JavaType domainEntity =
          getJavaTypeConverter().convertFromText(entity, JavaType.class, PROJECT);

      // Check if current entity has valid repository
      Collection<ClassOrInterfaceTypeDetails> repositories =
          repositoryJpaLocator.getRepositories(domainEntity);

      for (ClassOrInterfaceTypeDetails repository : repositories) {
        String replacedValue = replaceTopLevelPackageString(repository, currentText);
        allPossibleValues.add(replacedValue);
      }

      if (allPossibleValues.isEmpty()) {
        LOGGER
            .log(
                Level.INFO,
                String
                    .format(
                        "ERROR: Entity '%s' does not have any repository generated. Use 'repository' commands to generate a valid repository and then try again.",
                        entity));
        allPossibleValues.add("");
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "service", param = "entity",
      help = "--entity option should be an entity.")
  public List<String> getEntityPossibleResults(ShellContext shellContext) {

    // Get current value of class
    String currentText = shellContext.getParameters().get("entity");

    List<String> allPossibleValues = new ArrayList<String>();

    // Getting all existing entities
    Set<ClassOrInterfaceTypeDetails> entitiesInProject =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : entitiesInProject) {
      String name = replaceTopLevelPackageString(entity, currentText);
      if (!allPossibleValues.contains(name)) {
        allPossibleValues.add(name);
      }
    }

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "service", param = "interface",
      help = "--interface option should be a new interface.", validate = false,
      includeSpaceOnFinish = false)
  public List<String> getInterfacePossibleResults(ShellContext shellContext) {

    List<String> allPossibleValues = new ArrayList<String>();

    // Add all modules to completions list
    Collection<String> modules = projectOperations.getModuleNames();
    for (String module : modules) {
      if (StringUtils.isNotBlank(module)
          && !module.equals(projectOperations.getFocusedModule().getModuleName())) {
        allPossibleValues.add(module.concat(LogicalPath.MODULE_PATH_SEPARATOR)
            .concat(JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL).concat("."));
      } else if (!projectOperations.isMultimoduleProject()) {

        // Add only JavaPackage and JavaType completion
        allPossibleValues.add(String.format("%s.service.api.",
            JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL));
      }
    }

    // Always add base package
    allPossibleValues.add("~.");

    return allPossibleValues;
  }

  @CliOptionAutocompleteIndicator(command = "service", param = "class",
      help = "--class option should be a new class.", validate = false,
      includeSpaceOnFinish = false)
  public List<String> getClassPossibleResults(ShellContext shellContext) {

    List<String> allPossibleValues = new ArrayList<String>();

    // Add all modules to completions list
    Collection<String> modules = projectOperations.getModuleNames();
    for (String module : modules) {
      if (StringUtils.isNotBlank(module)
          && !module.equals(projectOperations.getFocusedModule().getModuleName())) {
        allPossibleValues.add(module.concat(LogicalPath.MODULE_PATH_SEPARATOR)
            .concat(JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL).concat("."));
      } else if (!projectOperations.isMultimoduleProject()) {

        // Add only JavaPackage and JavaType completion
        allPossibleValues.add(String.format("%s.service.impl.",
            JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL));
      }
    }

    // Always add base package
    allPossibleValues.add("~.");

    return allPossibleValues;
  }

  /**
   * This indicator says if --apiPackage and --implPackage parameters are visible.
   *
   * If --all is specified, --apiPackage and --implPackage will be visible
   *
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(
      params = {"apiPackage", "implPackage"},
      command = "service",
      help = "--apiPackage and --implPackage parameters are not visible if --all parameter hasn't been specified before.")
  public boolean arePackageParametersVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return true;
    }
    return false;
  }

  /**
   * This indicator says if --all parameter is visible.
   *
   * If --entity is specified, --all won't be visible
   *
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(params = {"all"}, command = "service",
      help = "--all parameter is not visible if --entity parameter has been specified before.")
  public boolean isAllParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("entity")) {
      return false;
    }
    return true;
  }

  /**
   * This indicator says if --entity parameter is visible.
   *
   * If --all is specified, --entity won't be visible
   *
   * @param context ShellContext
   * @return
   */
  @CliOptionVisibilityIndicator(command = "service", params = {"entity"},
      help = "--all parameter is not visible if --entity parameter has been specified before.")
  public boolean isEntityParameterVisible(ShellContext context) {
    if (context.getParameters().containsKey("all")) {
      return false;
    }
    return true;
  }

  @CliCommand(
      value = "service",
      help = "Creates new service interface and its implementation related to an entity, or for all the "
          + "entities in generated project, with some basic management methods by using Spring Data repository methods.")
  public void service(
      @CliOption(
          key = "all",
          mandatory = false,
          specifiedDefaultValue = "true",
          unspecifiedDefaultValue = "false",
          help = "Indicates if developer wants to generate service interfaces and their implementations "
              + "for every entity of current project. "
              + "This option is mandatory if `--entity` is not specified. Otherwise, using `--entity` "
              + "will cause the parameter `--all` won't be available. "
              + "Default if option present: `true`; default if option not present: `false`.") boolean all,
      @CliOption(
          key = "entity",
          optionContext = PROJECT,
          mandatory = false,
          help = "The domain entity this service should manage. When working on a single module "
              + "project, simply specify the name of the entity. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.domain.MyEntity` (where `~` is the base package). "
              + "When working with multiple modules, you should specify the name of the entity and the "
              + "module where it is. Ex.: `--class model:~.domain.MyEntity`. If the module is not specified, "
              + "it is assumed that the entity is in the module which has the focus. "
              + "Possible values are: any of the entities in the project. "
              + "This option is mandatory if `--all` is not specified. Otherwise, using `--all` "
              + "will cause the parameter `--entity` won't be available.") final JavaType domainType,
      @CliOption(
          key = "repository",
          optionContext = PROJECT,
          mandatory = true,
          help = "The repository this service should expose. When working on a single module project, "
              + "simply specify the name of the class. If you consider it necessary, you can also "
              + "specify the package. Ex.: `--class ~.repository.MyClass` (where `~` is the base "
              + "package). When working with multiple modules, you should specify the name of the class "
              + "and the module where it is. Ex.: `--class repository:~.MyClass`. If the module is not "
              + "specified, it is assumed that the class is in the module which has the focus. "
              + "Possible values are: any of the repositories annotated with `@RooJpaRepository` and "
              + "associated to the entity specified in `--entity`. "
              + "This option is mandatory if `--entity` has been already specified and the project is "
              + "multi-module. "
              + "This option is available only when `--entity` has been specified. "
              + "Default if option not present: first repository annotated with `@RooJpaRepository` and "
              + "associated to the entity specified in `--entity`.") final JavaType repositoryType,
      @CliOption(
          key = "interface",
          mandatory = true,
          help = "The service interface to be generated. When working on a single module project, simply "
              + "specify the name of the class. If you consider it necessary, you can also specify the "
              + "package. Ex.: `--class ~.service.api.MyClass` (where `~` is the base package). When "
              + "working with multiple modules, you should specify the name of the class and the module "
              + "where it is. Ex.: `--class service-api:~.MyClass`. If the module is not specified, it "
              + "is assumed that the class is in the module which has the focus. "
              + "This option is mandatory if `--entity` has been already specified and the project is "
              + "multi-module. "
              + "This option is available only when `--entity` has been specified. "
              + "Default if option not present: concatenation of entity simple name with 'Service' in "
              + "`~.service.api` package, or 'service-api:~.' if multi-module project.") final JavaType interfaceType,
      @CliOption(
          key = "class",
          mandatory = false,
          help = "The service implementation to be generated. When working on a single module "
              + "project, simply specify the name of the class. If you consider it necessary, you can "
              + "also specify the package. Ex.: `--class ~.service.impl.MyClass` (where `~` is the base "
              + "package). When working with multiple modules, you should specify the name of the class "
              + "and the module where it is. Ex.: `--class service-impl:~.MyClass`. If the module is not "
              + "specified, it is assumed that the class is in the module which has the focus. "
              + "This option is available only when `--entity` has been specified. "
              + "Default if option not present: concatenation of entity simple name with 'ServiceImpl' "
              + "in `~.service.impl` package, or 'service-impl:~.' if multi-module project.") final JavaType implType,
      @CliOption(
          key = "apiPackage",
          mandatory = false,
          help = "The java interface package. In multi-module project you should specify the module name"
              + " before the package name. Ex.: `--apiPackage service-api:org.springframework.roo` but, "
              + "if module name is not present, the Roo Shell focused module will be used. "
              + "This option is available only when `--all` parameter has been specified. "
              + "Default value if not present: `~.service.api` package, or 'service-api:~.' if "
              + "multi-module project.") JavaPackage apiPackage,
      @CliOption(
          key = "implPackage",
          mandatory = false,
          help = "The java package of the implementation classes for the interfaces. In multi-module "
              + "project you should specify the module name before the package name. Ex.: `--implPackage "
              + "service-impl:org.springframework.roo` but, if module name is not present, the Roo Shell "
              + "focused module will be used. "
              + "This option is available only when `--all` parameter has been specified. "
              + "Default value if not present: `~.service.impl` package, or 'service-impl:~.' if "
              + "multi-module project.") JavaPackage implPackage) {

    if (all) {

      // If user didn't specified some API package, use default API package
      if (apiPackage == null) {
        if (projectOperations.isMultimoduleProject()) {

          // Build default JavaPackage with module
          for (String moduleName : projectOperations.getModuleNames()) {
            if (moduleName.equals("service-api")) {
              Pom module = projectOperations.getPomFromModuleName(moduleName);
              apiPackage =
                  new JavaPackage(typeLocationService.getTopLevelPackageForModule(module),
                      moduleName);
              break;
            }
          }

          // Check if repository found
          Validate.notNull(apiPackage, "Couldn't find in project a default service.api "
              + "package. Please, use 'apiPackage' option to specify it.");
        } else {

          // Build default JavaPackage for single module
          apiPackage =
              new JavaPackage(projectOperations.getFocusedTopLevelPackage()
                  .getFullyQualifiedPackageName().concat(".service.api"),
                  projectOperations.getFocusedModuleName());
        }
      }

      // If user didn't specified some impl package, use default impl package
      if (implPackage == null) {
        if (projectOperations.isMultimoduleProject()) {

          // Build default JavaPackage with module
          for (String moduleName : projectOperations.getModuleNames()) {
            if (moduleName.equals("service-impl")) {
              Pom module = projectOperations.getPomFromModuleName(moduleName);
              implPackage =
                  new JavaPackage(typeLocationService.getTopLevelPackageForModule(module),
                      moduleName);
              break;
            }
          }

          // Check if repository found
          Validate.notNull(implPackage, "Couldn't find in project a default service.impl "
              + "package. Please, use 'implPackage' option to specify it.");
        } else {

          // Build default JavaPackage for single module
          implPackage =
              new JavaPackage(projectOperations.getFocusedTopLevelPackage()
                  .getFullyQualifiedPackageName().concat(".service.impl"),
                  projectOperations.getFocusedModuleName());
        }
      }

      serviceOperations.addAllServices(apiPackage, implPackage);
    } else {
      serviceOperations.addService(domainType, repositoryType, interfaceType, implType);
    }
  }

  /**
   * Replaces a JavaType fullyQualifiedName for a shorter name using '~' for TopLevelPackage
   *
   * @param cid ClassOrInterfaceTypeDetails of a JavaType
   * @param currentText String current text for option value
   * @return the String representing a JavaType with its name shortened
   */
  private String replaceTopLevelPackageString(ClassOrInterfaceTypeDetails cid, String currentText) {
    String javaTypeFullyQualilfiedName = cid.getType().getFullyQualifiedTypeName();
    String javaTypeString = "";
    String topLevelPackageString = "";

    // Add module value to topLevelPackage when necessary
    if (StringUtils.isNotBlank(cid.getType().getModule())
        && !cid.getType().getModule().equals(projectOperations.getFocusedModuleName())) {

      // Target module is not focused
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else if (StringUtils.isNotBlank(cid.getType().getModule())
        && cid.getType().getModule().equals(projectOperations.getFocusedModuleName())
        && (currentText.startsWith(cid.getType().getModule()) || cid.getType().getModule()
            .startsWith(currentText)) && StringUtils.isNotBlank(currentText)) {

      // Target module is focused but user wrote it
      javaTypeString = cid.getType().getModule().concat(LogicalPath.MODULE_PATH_SEPARATOR);
      topLevelPackageString =
          projectOperations.getTopLevelPackage(cid.getType().getModule())
              .getFullyQualifiedPackageName();
    } else {

      // Not multimodule project
      topLevelPackageString =
          projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

    // Autocomplete with abbreviate or full qualified mode
    String auxString =
        javaTypeString.concat(StringUtils.replace(javaTypeFullyQualilfiedName,
            topLevelPackageString, "~"));
    if ((StringUtils.isBlank(currentText) || auxString.startsWith(currentText))
        && StringUtils.contains(javaTypeFullyQualilfiedName, topLevelPackageString)) {

      // Value is for autocomplete only or user wrote abbreviate value
      javaTypeString = auxString;
    } else {

      // Value could be for autocomplete or for validation
      javaTypeString = String.format("%s%s", javaTypeString, javaTypeFullyQualilfiedName);
    }

    return javaTypeString;
  }

  @SuppressWarnings("unchecked")
  public Converter<JavaType> getJavaTypeConverter() {
    if (javaTypeConverter == null) {
      // Get all Services implement JavaTypeConverter interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(Converter.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          Converter<?> converter = (Converter<?>) this.context.getService(ref);
          if (converter.supports(JavaType.class, PROJECT)) {
            javaTypeConverter = (Converter<JavaType>) converter;
            return javaTypeConverter;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("ERROR: Cannot load JavaTypeConverter on FinderCommands.");
        return null;
      }
    } else {
      return javaTypeConverter;
    }
  }
}
