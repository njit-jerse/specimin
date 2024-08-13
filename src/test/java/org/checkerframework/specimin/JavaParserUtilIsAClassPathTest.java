package org.checkerframework.specimin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** This class unit tests the isAClassPath method in JavaParserUtil. */
public class JavaParserUtilIsAClassPathTest {

  private static final String LONG_CHAIN =
      "BigQueryIO.read(BillingEvent::parseFromRecord)\n"
          + "                .fromQuery(InvoicingUtils.makeQueryProvider(options.getYearMonth(),"
          + " projectId))\n"
          + "                .withCoder(SerializableCoder.of(BillingEvent.class))\n"
          + "                .usingStandardSql()\n"
          + "                .withoutValidation()\n"
          + "                .withTemplateCompatibility())";

  private static final String ENDS_WITH_DOT_CLASS =
      "BigQueryIO.read(BillingEvent::parseFromRecord)\n"
          + "                .fromQuery(InvoicingUtils.makeQueryProvider(options.getYearMonth(),"
          + " projectId))\n"
          + "                .withCoder(SerializableCoder.of(BillingEvent.class))";

  private static final String ANOTHER_LONG_CHAIN =
      "TextIO.write().to(NestedValueProvider.of(yearMonthProvider, yearMonth ->"
          + " String.format(\"%s/%s/%s/%s-%s\", billingBucketUrl, BillingModule.INVOICES_DIRECTORY,"
          + " yearMonth, invoiceFilePrefix,"
          + " yearMonth))).withHeader(InvoiceGroupingKey.invoiceHeader())";

  private static final String ANOTHER_LONG_CHAIN_2 =
      "TextIO.write().to(NestedValueProvider.of(yearMonthProvider, yearMonth ->"
          + " String.format(\"%s/%s/%s/%s-%s\", billingBucketUrl, BillingModule.INVOICES_DIRECTORY,"
          + " yearMonth, invoiceFilePrefix,"
          + " yearMonth))).withHeader(InvoiceGroupingKey.invoiceHeader()).withoutSharding()";

  private static final String ANOTHER_LONG_CHAIN_3 =
      "TextIO.write().to(NestedValueProvider.of(yearMonthProvider, yearMonth ->"
          + " String.format(\"%s/%s/%s/%s-%s\", billingBucketUrl, BillingModule.INVOICES_DIRECTORY,"
          + " yearMonth, invoiceFilePrefix, yearMonth)))";

  @Test
  public void testIsAClassPath() {
    assertTrue(JavaParserUtil.isAClassPath("org.checkerframework.javacutil.ElementUtils"));

    assertFalse(JavaParserUtil.isAClassPath("org"));
    assertFalse(JavaParserUtil.isAClassPath("ElementUtils"));
    assertFalse(JavaParserUtil.isAClassPath("ElementUtils.foo()"));
    assertFalse(JavaParserUtil.isAClassPath("org.ElementUtils.foo()"));
    assertFalse(JavaParserUtil.isAClassPath("MapElements.into(TypeDescriptors.strings())"));
    assertFalse(JavaParserUtil.isAClassPath(LONG_CHAIN));
    assertFalse(JavaParserUtil.isAClassPath("TextIO.write()"));
    assertFalse(JavaParserUtil.isAClassPath("TextIO.<BillingEvent>writeCustomType()"));
    assertFalse(JavaParserUtil.isAClassPath(ENDS_WITH_DOT_CLASS));
    assertFalse(JavaParserUtil.isAClassPath(ANOTHER_LONG_CHAIN));
    assertFalse(JavaParserUtil.isAClassPath(ANOTHER_LONG_CHAIN_2));
    // This is the one that caused https://github.com/kelloggm/specimin/issues/94
    assertFalse(JavaParserUtil.isAClassPath(ANOTHER_LONG_CHAIN_3));
  }
}
