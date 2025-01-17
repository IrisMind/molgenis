package org.molgenis.data.security.exception;

import static org.testng.Assert.assertEquals;

import org.molgenis.util.exception.CodedRuntimeException;
import org.molgenis.util.exception.ExceptionMessageTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AclNotFoundExceptionTest extends ExceptionMessageTest {
  @BeforeMethod
  public void setUp() {
    messageSource.addMolgenisNamespaces("data-security");
  }

  @Test(dataProvider = "languageMessageProvider")
  @Override
  public void testGetLocalizedMessage(String lang, String message) {
    ExceptionMessageTest.assertExceptionMessageEquals(
        new AclNotFoundException("type"), lang, message);
  }

  @Test
  public void testGetMessage() {
    CodedRuntimeException ex = new AclNotFoundException("type");
    assertEquals(ex.getMessage(), "typeId:type");
  }

  @DataProvider(name = "languageMessageProvider")
  @Override
  public Object[][] languageMessageProvider() {
    return new Object[][] {
      new Object[] {"en", "No ACL information could be found for type 'type'."}
    };
  }
}
