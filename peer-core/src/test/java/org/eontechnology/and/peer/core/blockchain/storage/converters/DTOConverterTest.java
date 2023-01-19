package org.eontechnology.and.peer.core.blockchain.storage.converters;

import com.dampcake.bencode.Bencode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.junit.Assert;
import org.junit.Test;

public class DTOConverterTest {

  private Bencode bencode = new Bencode();
  private ISigner signer =
      new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
  private BlockID networkID = new BlockID(1L);
  private ISigner signer1 =
      new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
  private ISigner signer2 =
      new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

  private int timestamp = 12345;
  private TransactionID referencedTx = new TransactionID(100L);
  private Map<String, Object> attachment =
      new HashMap<String, Object>() {
        {
          put("param1", "value1");
          put("param2", "value2");
        }
      };

  @Test
  public void transaction_base() throws Exception {

    Transaction a = Builder.newTransaction(timestamp).attach(null).build(networkID, signer);
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));

    assetEquals(a, b);
  }

  @Test
  public void transaction_contains_reference() throws Exception {

    Transaction a =
        Builder.newTransaction(timestamp).attach(null).refBy(referencedTx).build(networkID, signer);
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));

    assetEquals(a, b);
  }

  @Test
  public void transaction_contains_nested_transaction() throws Exception {

    Transaction a =
        Builder.newTransaction(timestamp)
            .attach(null)
            .addNested(Builder.newTransaction(timestamp).build(networkID, signer1))
            .addNested(Builder.newTransaction(timestamp).build(networkID, signer2))
            .build(networkID, signer);
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));
    assetEquals(a, b);
  }

  @Test
  public void transaction_contains_confirmations() throws Exception {
    Transaction a =
        Builder.newTransaction(timestamp)
            .attach(null)
            .build(networkID, signer, new ISigner[] {signer1, signer2});
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));
    assetEquals(a, b);
  }

  @Test
  public void transaction_contains_payer() throws Exception {
    Transaction a =
        Builder.newTransaction(timestamp)
            .payedBy(new AccountID(signer2.getPublicKey()))
            .build(networkID, signer, new ISigner[] {signer1, signer2});
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));
    assetEquals(a, b);
  }

  @Test
  public void transaction_contains_nested_transaction_payed() throws Exception {

    Transaction a =
        Builder.newTransaction(timestamp)
            .attach(null)
            .addNested(
                Builder.newTransaction(timestamp)
                    .payedBy(new AccountID(signer2.getPublicKey()))
                    .build(networkID, signer1))
            .addNested(
                Builder.newTransaction(timestamp)
                    .payedBy(new AccountID(signer1.getPublicKey()))
                    .build(networkID, signer2))
            .build(networkID, signer);
    Transaction b = DTOConverter.convert(DTOConverter.convert(a));
    assetEquals(a, b);
  }

  private void assetEquals(Transaction a, Transaction b) {

    Assert.assertEquals(a.getType(), b.getType());
    Assert.assertEquals(a.getTimestamp(), b.getTimestamp());
    Assert.assertEquals(a.getDeadline(), b.getDeadline());
    Assert.assertEquals(a.getReference(), b.getReference());
    Assert.assertEquals(a.getSenderID(), b.getSenderID());
    Assert.assertEquals(a.getFee(), b.getFee());
    if (a.getData() == null) {
      Assert.assertNull(a.getData());
      Assert.assertNull(b.getData());
    } else {
      Assert.assertEquals(
          Format.convert(bencode.encode(a.getData())), Format.convert(bencode.encode(b.getData())));
    }
    Assert.assertEquals(Format.convert(a.getSignature()), Format.convert(b.getSignature()));
    Assert.assertEquals(a.getID(), b.getID());
    Assert.assertEquals(a.getVersion(), b.getVersion());
    Assert.assertEquals(a.getConfirmations(), b.getConfirmations());
    Assert.assertEquals(a.hasNestedTransactions(), b.hasNestedTransactions());
    Assert.assertEquals(a.getPayer(), b.getPayer());
    if (a.hasNestedTransactions() || b.hasNestedTransactions()) {
      Set<String> aSet = a.getNestedTransactions().keySet();
      Set<String> bSet = b.getNestedTransactions().keySet();
      Assert.assertEquals(aSet.size(), bSet.size());

      for (Transaction tx1 : a.getNestedTransactions().values()) {
        Transaction tx2 = b.getNestedTransactions().get(tx1.getID().toString());
        Assert.assertNotNull(tx2);
        assetEquals(tx1, tx2);
      }
    }
  }

  @Test
  public void db_transaction_format() throws Exception {

    Transaction tx1 = Builder.newTransaction(timestamp).build(networkID, signer1);
    Transaction tx2 = Builder.newTransaction(timestamp).build(networkID, signer2);

    Transaction tx =
        Builder.newTransaction(timestamp)
            .refBy(referencedTx)
            .attach(attachment)
            .note("note")
            .addNested(tx1)
            .addNested(tx2)
            .payedBy(new AccountID(signer2.getPublicKey()))
            .build(networkID, signer, new ISigner[] {signer1, signer2});

    DbTransaction dbTx = DTOConverter.convert(tx);
    Assert.assertEquals(dbTx.getVersion(), tx.getVersion());
    Assert.assertEquals(dbTx.getType(), tx.getType());
    Assert.assertEquals(dbTx.getTimestamp(), timestamp);
    Assert.assertEquals(dbTx.getDeadline(), 3600);
    Assert.assertEquals(dbTx.getFee(), 10L);
    Assert.assertEquals(dbTx.getNote(), "note");
    Assert.assertEquals(dbTx.getReference(), referencedTx.getValue());
    Assert.assertEquals(dbTx.getSenderID(), new AccountID(signer.getPublicKey()).getValue());
    Assert.assertEquals(dbTx.getSignature(), Format.convert(tx.getSignature()));
    Assert.assertEquals(dbTx.getAttachment(), new String(bencode.encode(attachment)));
    Assert.assertEquals(dbTx.getConfirmations(), new String(bencode.encode(tx.getConfirmations())));
    Assert.assertEquals(dbTx.getPayerID(), tx.getPayer().getValue());

    Map<String, Object> map = new HashMap<>();
    map.put(tx1.getID().toString(), StorageTransactionMapper.convert(tx1));
    map.put(tx2.getID().toString(), StorageTransactionMapper.convert(tx2));
    Assert.assertEquals(dbTx.getNestedTransactions(), new String(bencode.encode(map)));
  }
}
