package com.exscudo.peer.eon.transactions;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.transport.TransactionMapper;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.crypto.ISigner;

public class TransactionConverterTest {

	private Bencode bencode = new Bencode();

	@Before
	public void setUp() {
		CryptoProvider cryptoProvider = Mockito.mock(CryptoProvider.class);
		CryptoProvider.init(cryptoProvider);
	}

	@Test
	public void transaction_payment() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Payment.newPayment(100L, 12345L).forFee(1L).validity(12345, 60, 1).build(signer);

		checkTransaction(tran,
				"d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei60e3:feei1e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni1ee");
	}

	@Test
	public void transaction_payment_v2() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Payment.newPayment(100L, 12345L).forFee(1L).validity(12345, 3600, 2).build(signer);

		checkTransaction(tran,
				"d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei3600e3:feei1e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni2ee");
	}

	@Test
	public void transaction_payment_referenced() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Payment.newPayment(100L, 12345L).forFee(1L).validity(12345, 60, 1).build(signer);
		tran.setReference(-1);

		checkTransaction(tran,
				"d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei60e3:feei1e21:referencedTransaction23:EON-T-ZZZZZ-ZZZZZ-ZZZ9J6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei200e7:versioni1ee");
	}

	@Test
	public void transaction_register() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Registration.newAccount(signer.getPublicKey()).validity(12345 + 60, 60, 1).build(signer);

		checkTransaction(tran,
				"d10:attachmentd21:EON-RMNF4-KLGQ7-9Y65X64:ddf121b99504bc3cd18cabfdaaf0334d16d1d7403226fa924557da9b3a0f4642e8:deadlinei60e3:feei1e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12405e4:typei100e7:versioni1ee");
	}

	@Test
	public void transaction_deposit() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Deposit.refill(999L).validity(12345, 60, 1).build(signer);

		checkTransaction(tran,
				"d10:attachmentd6:amounti999ee8:deadlinei60e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei310e7:versioni1ee");
	}

	@Test
	public void transaction_deposit_issue() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Deposit.withdraw(999L).validity(12345, 60, 1).build(signer);

		checkTransaction(tran,
				"d10:attachmentd6:amounti999ee8:deadlinei60e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei320e7:versioni1ee");
	}

	@Test
	public void transaction_confirmations() throws Exception {
		MockSigner signer = new MockSigner(123L);

		Transaction tran = Deposit.withdraw(999L).validity(12345, 60, 1).build(signer, new ISigner[]{signer});

		checkTransaction(tran,
				"d10:attachmentd6:amounti999ee13:confirmationsd21:EON-RMNF4-KLGQ7-9Y65X128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c57437e8:deadlinei60e3:feei10e6:sender21:EON-RMNF4-KLGQ7-9Y65X9:signature128:c3764dce8a9c539f5f0d1be0fcd49e8c90f44eb7daf786297b276912eb478bae58c2d7cb22f1844404cb269400d11a3eb4eb7ae82bca2e9a29441f26d4c574379:timestampi12345e4:typei320e7:versioni1ee");
	}

	private void checkTransaction(Transaction tran, String data) throws IllegalArgumentException {
		Map<String, Object> map = TransactionMapper.convert(tran);
		byte[] bytes = bencode.encode(map);
		String s = new String(bytes);

		assertEquals(data, s);

		Map<String, Object> decoded = bencode.decode(data.getBytes(), Type.DICTIONARY);

		Transaction tx = TransactionMapper.convert(decoded);

		Assert.assertEquals(tran.getType(), tx.getType());
		Assert.assertEquals(tran.getTimestamp(), tx.getTimestamp());
		Assert.assertEquals(tran.getDeadline(), tx.getDeadline());
		Assert.assertEquals(tran.getReference(), tx.getReference());
		Assert.assertEquals(tran.getSenderID(), tx.getSenderID());
		Assert.assertEquals(tran.getFee(), tx.getFee());
		Assert.assertEquals(Format.convert(bencode.encode(tran.getData())),
				Format.convert(bencode.encode(tx.getData())));
		Assert.assertEquals(Format.convert(tran.getSignature()), Format.convert(tx.getSignature()));
		Assert.assertEquals(tran.getBlock(), tx.getBlock());
		Assert.assertEquals(tran.getHeight(), tx.getHeight());
		Assert.assertEquals(tran.getID(), tx.getID());
		Assert.assertEquals(tran.getLength(), tx.getLength());
		Assert.assertEquals(tran.getVersion(), tx.getVersion());
		Assert.assertEquals(tran.getConfirmations(), tx.getConfirmations());
	}
}
