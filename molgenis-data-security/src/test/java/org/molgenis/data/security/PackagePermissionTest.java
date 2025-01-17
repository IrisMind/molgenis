package org.molgenis.data.security;

import static org.testng.Assert.assertEquals;

import java.util.Locale;
import org.molgenis.util.i18n.AllPropertiesMessageSource;
import org.molgenis.util.i18n.MessageSourceHolder;
import org.molgenis.util.i18n.TestAllPropertiesMessageSource;
import org.molgenis.util.i18n.format.MessageFormatFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class PackagePermissionTest {
  private PackagePermission addPackagePermission = PackagePermission.ADD_PACKAGE;
  private MessageFormatFactory messageFormatFactory = new MessageFormatFactory();
  private AllPropertiesMessageSource messageSource;

  @BeforeMethod
  public void exceptionMessageTestBeforeMethod() {
    messageSource = new TestAllPropertiesMessageSource(messageFormatFactory);
    messageSource.addMolgenisNamespaces("data-security");
    MessageSourceHolder.setMessageSource(messageSource);
  }

  @Test
  public void testNameEnglish() {
    assertEquals(
        messageSource.getMessage(addPackagePermission.getName(), Locale.ENGLISH), "Add package");
  }

  @Test
  public void testDescription() {
    assertEquals(
        messageSource.getMessage(addPackagePermission.getDescription(), Locale.ENGLISH),
        "Permission to add a child package to this package");
  }
}
