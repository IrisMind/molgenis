package org.molgenis.data.importer.emx.exception;

import static org.testng.Assert.*;

import org.molgenis.util.exception.ExceptionMessageTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AttributeNameCaseMismatchExceptionTest extends ExceptionMessageTest {
  @BeforeMethod
  public void setUp() {
    messageSource.addMolgenisNamespaces("data-import");
  }

  @Test(dataProvider = "languageMessageProvider")
  @Override
  public void testGetLocalizedMessage(String lang, String message) {
    assertExceptionMessageEquals(
        new AttributeNameCaseMismatchException("Test", "test", "attributes"), lang, message);
  }

  @DataProvider(name = "languageMessageProvider")
  @Override
  public Object[][] languageMessageProvider() {
    return new Object[][] {
      new Object[] {
        "en", "Unsupported attribute metadata: 'Test', did you mean 'test'? (sheet: 'attributes')"
      },
      {
        "nl",
        "Niet ondersteunde attribuut metadata: 'Test', bedoelde u misschien 'test'? (werkblad: 'attributes')"
      }
    };
  }
}
