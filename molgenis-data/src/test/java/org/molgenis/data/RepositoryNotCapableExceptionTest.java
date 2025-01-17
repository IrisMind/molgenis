package org.molgenis.data;

import static org.molgenis.data.RepositoryCapability.WRITABLE;

import org.molgenis.util.exception.ExceptionMessageTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RepositoryNotCapableExceptionTest extends ExceptionMessageTest {
  @BeforeMethod
  public void setUp() {
    messageSource.addMolgenisNamespaces("data");
  }

  @Test(dataProvider = "languageMessageProvider")
  @Override
  public void testGetLocalizedMessage(String lang, String message) {
    assertExceptionMessageEquals(
        new RepositoryNotCapableException("MyRepository", WRITABLE), lang, message);
  }

  @DataProvider(name = "languageMessageProvider")
  @Override
  public Object[][] languageMessageProvider() {
    Object[] enParams = {"en", "Repository 'MyRepository' does not support 'writing'."};
    Object[] nlParams = {"nl", "Opslagplaats 'MyRepository' ondersteunt geen 'schrijven'."};
    return new Object[][] {enParams, nlParams};
  }
}
