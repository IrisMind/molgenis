package org.molgenis.data.security.exception;

import static org.testng.Assert.assertEquals;

import org.molgenis.util.exception.CodedRuntimeException;
import org.molgenis.util.exception.ExceptionMessageTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SidPermissionExceptionTest extends ExceptionMessageTest {
  @BeforeMethod
  public void setUp() {
    messageSource.addMolgenisNamespaces("data-security");
  }

  @Test(dataProvider = "languageMessageProvider")
  @Override
  public void testGetLocalizedMessage(String lang, String message) {
    ExceptionMessageTest.assertExceptionMessageEquals(
        new SidPermissionException("user1,user2,roleA,roleB"), lang, message);
  }

  @Test
  public void testGetMessage() {
    CodedRuntimeException ex = new SidPermissionException("user1,user2,roleA,roleB");
    assertEquals(ex.getMessage(), "sids:user1,user2,roleA,roleB");
  }

  @DataProvider(name = "languageMessageProvider")
  @Override
  public Object[][] languageMessageProvider() {
    return new Object[][] {
      new Object[] {
        "en",
        "No permission to read permissions for user(s) and/or role(s) 'user1,user2,roleA,roleB'."
      }
    };
  }
}
