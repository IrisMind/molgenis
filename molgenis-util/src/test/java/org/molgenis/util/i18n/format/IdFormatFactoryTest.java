package org.molgenis.util.i18n.format;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.text.Format;
import org.mockito.Mock;
import org.molgenis.test.AbstractMockitoTest;
import org.molgenis.util.i18n.Identifiable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdFormatFactoryTest extends AbstractMockitoTest {
  private Format idFormat;
  @Mock private Identifiable identifiable;

  @BeforeMethod
  public void beforeMethod() {
    idFormat = new IdFormatFactory().getFormat(null, null, null);
  }

  @Test
  public void testFormatIdentifiable() {
    when(identifiable.getIdValue()).thenReturn("abcde");
    assertEquals(idFormat.format(identifiable), "abcde");
  }

  @Test
  public void testFormatString() {
    assertEquals(idFormat.format("abcde"), "abcde");
  }

  @Test
  public void testFormatNull() {
    assertEquals(idFormat.format(null), "null");
  }
}
