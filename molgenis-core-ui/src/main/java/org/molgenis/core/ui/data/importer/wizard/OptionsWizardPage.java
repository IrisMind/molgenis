package org.molgenis.core.ui.data.importer.wizard;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;
import static org.molgenis.util.stream.MapCollectors.toLinkedMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.core.ui.wizard.AbstractWizardPage;
import org.molgenis.core.ui.wizard.Wizard;
import org.molgenis.data.DataService;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.file.FileRepositoryCollectionFactory;
import org.molgenis.data.file.util.FileExtensionUtils;
import org.molgenis.data.importer.EntitiesValidationReport;
import org.molgenis.data.importer.ImportService;
import org.molgenis.data.importer.ImportServiceFactory;
import org.molgenis.data.meta.model.Package;
import org.molgenis.data.security.NoWritablePackageException;
import org.molgenis.data.security.PackagePermissionUtils;
import org.molgenis.data.validation.meta.NameValidator;
import org.molgenis.security.core.UserPermissionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

@Component
public class OptionsWizardPage extends AbstractWizardPage {
  private static final long serialVersionUID = -2931051095557369343L;
  private static final Logger LOG = LoggerFactory.getLogger(OptionsWizardPage.class);

  private final transient FileRepositoryCollectionFactory fileRepositoryCollectionFactory;
  private final transient ImportServiceFactory importServiceFactory;
  private transient DataService dataService;
  private final transient UserPermissionEvaluator userPermissionEvaluator;

  OptionsWizardPage(
      FileRepositoryCollectionFactory fileRepositoryCollectionFactory,
      ImportServiceFactory importServiceFactory,
      DataService dataService,
      UserPermissionEvaluator userPermissionEvaluator) {
    this.fileRepositoryCollectionFactory = fileRepositoryCollectionFactory;
    this.importServiceFactory = importServiceFactory;
    this.dataService = dataService;
    this.userPermissionEvaluator = requireNonNull(userPermissionEvaluator);
  }

  @Override
  public String getTitle() {
    return "Options";
  }

  @SuppressWarnings("squid:S2083")
  @Override
  public String handleRequest(HttpServletRequest request, BindingResult result, Wizard wizard) {
    ImportWizardUtil.validateImportWizard(wizard);
    ImportWizard importWizard = (ImportWizard) wizard;

    String dataImportOption = setImportOption(request, importWizard);

    setMetadataImportOption(request, importWizard);

    if (importWizard.getMustChangeEntityName()) {
      // userGivenName will be validated by the NameValidator and can't contain any
      // characters that have special meaning for the file system
      @SuppressWarnings("squid:S2083")
      String userGivenName = request.getParameter("name");
      if (StringUtils.isEmpty(userGivenName)) {
        result.addError(new ObjectError("wizard", "Please enter an entity name"));
        return null;
      }

      try {
        NameValidator.validateEntityName(userGivenName);
        if (dataService.hasRepository(userGivenName)) {
          result.addError(new ObjectError("wizard", "An entity with this name already exists."));
          return null;
        }
      } catch (MolgenisDataException e) {
        ImportWizardUtil.handleException(e, importWizard, result, LOG, dataImportOption);
        return null;
      }

      File tmpFile = importWizard.getFile();
      String tmpFilename = tmpFile.getName();

      String extension =
          FileExtensionUtils.findExtensionFromPossibilities(
              tmpFilename,
              fileRepositoryCollectionFactory
                  .createFileRepositoryCollection(tmpFile)
                  .getFileNameExtensions());

      File file = new File(tmpFile.getParent(), userGivenName + "." + extension);
      if (!tmpFile.renameTo(file)) {
        // Replace pattern-breaking characters, to fix sonac vulnerability S5145
        String filename =
            file.getName() == null ? file.getName().replaceAll("[\n|\r|\t]", "_") : null;
        LOG.error("Failed to rename '{}' to '{}'", tmpFilename, filename);
      }
      importWizard.setFile(file);
    }

    try {
      return validateInput(importWizard.getFile(), importWizard);
    } catch (Exception e) {
      ImportWizardUtil.handleException(e, importWizard, result, LOG, dataImportOption);
    }

    return null;
  }

  private String setImportOption(HttpServletRequest request, ImportWizard importWizard) {
    String dataImportOption = request.getParameter("data-option");
    if (dataImportOption != null && ImportWizardUtil.toDataAction(dataImportOption) == null) {
      throw new IllegalArgumentException("unknown data action: " + dataImportOption);
    }
    importWizard.setDataImportOption(dataImportOption);
    return dataImportOption;
  }

  private void setMetadataImportOption(HttpServletRequest request, ImportWizard importWizard) {
    String metadataImportOption = request.getParameter("metadata-option");
    if (metadataImportOption != null
        && ImportWizardUtil.toMetadataAction(metadataImportOption) == null) {
      throw new IllegalArgumentException("unknown metadata action: " + metadataImportOption);
    }
    importWizard.setMetadataImportOption(metadataImportOption);
  }

  private String validateInput(File file, ImportWizard wizard) {

    // decide what importer to use...
    RepositoryCollection source =
        fileRepositoryCollectionFactory.createFileRepositoryCollection(file);
    ImportService importService = importServiceFactory.getImportService(file, source);
    EntitiesValidationReport validationReport = importService.validateImport(file, source);

    wizard.setEntitiesImportable(validationReport.getSheetsImportable());
    wizard.setFieldsDetected(validationReport.getFieldsImportable());
    wizard.setFieldsRequired(validationReport.getFieldsRequired());
    wizard.setFieldsAvailable(validationReport.getFieldsAvailable());
    wizard.setFieldsUnknown(validationReport.getFieldsUnknown());

    Set<String> allPackages = new HashSet<>(validationReport.getPackages());
    List<Package> packages = dataService.getMeta().getPackages();
    for (Package p : packages) {
      allPackages.add(p.getId());
    }

    List<String> entitiesInDefaultPackage = new ArrayList<>();
    for (String entityTypeId : validationReport.getSheetsImportable().keySet()) {
      if (validationReport.getSheetsImportable().get(entityTypeId)
          && isInDefaultPackage(entityTypeId, allPackages))
        entitiesInDefaultPackage.add(entityTypeId);
    }
    wizard.setEntitiesInDefaultPackage(entitiesInDefaultPackage);

    Map<String, String> packageSelection = getPackageSelection(packages);
    if (!entitiesInDefaultPackage.isEmpty() && packageSelection.isEmpty()) {
      throw new NoWritablePackageException();
    }
    wizard.setPackages(packageSelection);

    String msg = null;
    if (validationReport.valid()) {
      wizard.setFile(file);
      msg = "File is validated and can be imported.";
    } else {
      wizard.setValidationMessage(
          "File did not pass validation see results below. Please resolve the errors and try again.");
    }

    return msg;
  }

  private boolean isInDefaultPackage(String entityTypeId, Set<String> packages) {
    for (String packageName : packages) {
      if (entityTypeId.toLowerCase().startsWith(packageName.toLowerCase() + PACKAGE_SEPARATOR)) {
        return false;
      }
    }

    return true;
  }

  /** @return sorted map of writable packages ids to package path label */
  private Map<String, String> getPackageSelection(List<Package> packages) {
    return packages.stream()
        .filter(this::isReadablePackage)
        .sorted(comparing(this::getPackagePathLabel))
        .collect(toLinkedMap(Package::getId, this::getPackagePathLabel));
  }

  private String getPackagePathLabel(Package aPackage) {
    String packageLabel = aPackage.getLabel();
    Package parentPackage = aPackage.getParent();
    return parentPackage == null
        ? packageLabel
        : getPackagePathLabel(parentPackage) + " / " + packageLabel;
  }

  private boolean isReadablePackage(Package aPackage) {
    return PackagePermissionUtils.isReadablePackage(aPackage, userPermissionEvaluator);
  }
}
