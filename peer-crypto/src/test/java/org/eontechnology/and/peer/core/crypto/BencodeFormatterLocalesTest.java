package org.eontechnology.and.peer.core.crypto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import org.eontechnology.and.peer.core.crypto.mapper.ObjectMapper;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BencodeFormatterLocalesTest {

  private static Locale startLocale = Locale.getDefault();

  @AfterClass
  public static void restore() throws Exception {
    Locale.setDefault(startLocale);
  }

  @Parameterized.Parameter public Locale testLocale;

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    LinkedList<Object[]> res = new LinkedList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      res.add(new Object[] {locale});
    }
    return res;
  }

  @Test
  public void test_bencode_locale() {
    Locale.setDefault(testLocale);

    Transaction tx = new Transaction();
    tx.setVersion(1);
    tx.setFee(10);
    tx.setTimestamp(1);
    tx.setDeadline(3600);
    tx.setType(100);
    tx.setSenderID(new AccountID(100L));

    ObjectMapper mapper = new ObjectMapper(new BlockID(0L));
    BencodeFormatter formatter = new BencodeFormatter();

    byte[] bytes = formatter.getBytes(mapper.convert(tx));
    Assert.assertEquals(
        testLocale.getDisplayName(Locale.ENGLISH) + " - " + testLocale.toLanguageTag(),
        "D10:ATTACHMENTDE8:DEADLINEI3600E3:FEEI10E7:NETWORK23:EON-B-22222-22222-2222J6:SENDER21:EON-65222-22222-222LK9:TIMESTAMPI1E4:TYPEI100E7:VERSIONI1EE",
        new String(bytes));
  }
}
