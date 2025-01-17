package org.molgenis.data.importer.emx.exception;

import static org.testng.Assert.*;

import org.molgenis.util.exception.ExceptionMessageTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MissingRootPackageExceptionTest extends ExceptionMessageTest {
  @BeforeMethod
  public void setUp() {
    messageSource.addMolgenisNamespaces("data-import");
  }

  @Test(dataProvider = "languageMessageProvider")
  @Override
  public void testGetLocalizedMessage(String lang, String message) {
    assertExceptionMessageEquals(new MissingRootPackageException(), lang, message);
  }

  @DataProvider(name = "languageMessageProvider")
  @Override
  public Object[][] languageMessageProvider() {
    return new Object[][] {
      new Object[] {
        "en",
        "Missing root package. There must be at least one package without a parent. (sheet: 'packages')"
      },
      {
        "nl",
        "Onbrekende hoofdmap. Minstens 1 map zonder bovenliggende map nodig. (werkblad: 'packages')"
      }
    };
  }
}
