package org.anonymous.specslice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** This class unit tests static methods in UnsolvedSymbolVisitor. */
public class UnsovledSymbolVisitorStaticTest {

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
    assertTrue(UnsolvedSymbolVisitor.isAClassPath("org.checkerframework.javacutil.ElementUtils"));

    assertFalse(UnsolvedSymbolVisitor.isAClassPath("org"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("ElementUtils"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("ElementUtils.foo()"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("org.ElementUtils.foo()"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("MapElements.into(TypeDescriptors.strings())"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath(LONG_CHAIN));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("TextIO.write()"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath("TextIO.<BillingEvent>writeCustomType()"));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath(ENDS_WITH_DOT_CLASS));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath(ANOTHER_LONG_CHAIN));
    assertFalse(UnsolvedSymbolVisitor.isAClassPath(ANOTHER_LONG_CHAIN_2));
    // This is the one that caused https://github.com/kelloggm/specSlice/issues/94
    assertFalse(UnsolvedSymbolVisitor.isAClassPath(ANOTHER_LONG_CHAIN_3));
  }
}
