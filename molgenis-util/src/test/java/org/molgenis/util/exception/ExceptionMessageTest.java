package org.molgenis.util.exception;

import static org.testng.Assert.assertEquals;

import java.util.Locale;
import org.molgenis.test.AbstractMockitoTest;
import org.molgenis.util.i18n.AllPropertiesMessageSource;
import org.molgenis.util.i18n.MessageSourceHolder;
import org.molgenis.util.i18n.TestAllPropertiesMessageSource;
import org.molgenis.util.i18n.format.MessageFormatFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class ExceptionMessageTest extends AbstractMockitoTest {
  private MessageFormatFactory messageFormatFactory = new MessageFormatFactory();
  protected AllPropertiesMessageSource messageSource;

  @BeforeMethod
  public void exceptionMessageTestBeforeMethod() {
    messageSource = new TestAllPropertiesMessageSource(messageFormatFactory);
    MessageSourceHolder.setMessageSource(messageSource);
  }

  /**
   * Parameterized test case. Overrides must be annotated with {@link org.testng.annotations.Test}
   * and set the name of the {@link org.testng.annotations.DataProvider}.
   *
   * @param language message language
   * @param message expected exception message
   */
  @SuppressWarnings("unused")
  public abstract void testGetLocalizedMessage(String language, String message);

  /**
   * {@link org.testng.annotations.DataProvider} for @link{{@link #testGetLocalizedMessage(String,
   * String)}}. Overrides must annotate this method with {@link
   * org.testng.annotations.DataProvider}.
   */
  public abstract Object[][] languageMessageProvider();

  /** Asserts that localized exception messages match. */
  protected static void assertExceptionMessageEquals(Exception e, String language, String message) {
    LocaleContextHolder.setLocale(new Locale(language));
    try {
      assertEquals(e.getLocalizedMessage(), message);
    } finally {
      LocaleContextHolder.setLocale(null);
    }
  }

  @AfterMethod
  public void exceptionMessageTestAfterMethod() {
    MessageSourceHolder.setMessageSource(null);
  }
}
