package org.molgenis.i18n;

import static org.testng.Assert.assertEquals;

import org.molgenis.util.i18n.PropertiesMessageSource;
import org.testng.annotations.Test;

public class PropertiesMessageSourceTest {
  PropertiesMessageSource propertiesMessageSource = new PropertiesMessageSource(" naMespace\t");

  @Test
  public void testGetNamespace() {
    assertEquals(propertiesMessageSource.getNamespace(), "namespace");
  }
}
