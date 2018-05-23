package com.exscudo.eon.app.mapper;

import java.util.Map;
import java.util.Set;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.TestSigner;
import com.exscudo.eon.app.utils.mapper.TransaportTransactionMapper;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.ComplexPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.DelegateBuilder;
import com.exscudo.peer.tx.midleware.builders.DepositBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PublicationBuilder;
import com.exscudo.peer.tx.midleware.builders.QuorumBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import com.exscudo.peer.tx.midleware.builders.RejectionBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransaportTransactionMapperTest {
    private Bencode bencode = new Bencode();
    private ISigner signer;
    private BlockID networkID;

    @Before
    public void setUp() {
        signer = new TestSigner("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        networkID = new BlockID(0L);
    }

    //
    // Optional fields
    //

    @Test
    public void transaction_referenced() throws Exception {

        Transaction tx = (new TransactionBuilder(1)).forFee(1L)
                                                    .validity(12345, 3600)
                                                    .refBy(new TransactionID(-1))
                                                    .build(networkID, signer);
        checkTransaction(tx,
                         "d10:attachmentde8:deadlinei3600e3:feei1e2:id23:EON-T-T3E22-265ZS-BHYNU9:reference23:EON-T-ZZZZZ-ZZZZZ-ZZZ9J6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:9311593dd0f9904ef6124a398a777461adbb2d36fcf2fe3cc0954d6ff80fd436b917cfabbed32772c92de39fbdeb3ce9c99b8da6a030f00dbccd9c867fe860069:timestampi12345e4:typei1e7:versioni1ee");
    }

    @Test
    public void transaction_confirmations() throws Exception {

        Transaction tx = (new TransactionBuilder(1)).forFee(1L)
                                                    .validity(12345, 3600)
                                                    .build(networkID, signer, new ISigner[] {signer});

        checkTransaction(tx,
                         "d10:attachmentde13:confirmationsd21:EON-87VQX-TKLTD-3JJNK128:5ef562be95347e48689c4452aab8a1a8917673150b5097082587453c326672ad880212c5472b18fd344a5c0eea8a4349e8f5a942d003e198f8877c112c421a0fe8:deadlinei3600e3:feei1e2:id23:EON-T-T3E22-2SG3U-EN8MJ6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:5ef562be95347e48689c4452aab8a1a8917673150b5097082587453c326672ad880212c5472b18fd344a5c0eea8a4349e8f5a942d003e198f8877c112c421a0f9:timestampi12345e4:typei1e7:versioni1ee");
    }

    @Test
    public void transaction_nested_transaction() throws Exception {

        Transaction tx = (new TransactionBuilder(1)).forFee(1L)
                                                    .validity(12345, 3600)
                                                    .addNested((new TransactionBuilder(1)).forFee(1L)
                                                                                          .validity(12345, 3600)
                                                                                          .build(networkID, signer))
                                                    .addNested((new TransactionBuilder(2)).forFee(1L)
                                                                                          .validity(12345, 3600)
                                                                                          .build(networkID, signer))
                                                    .build(networkID, signer, new ISigner[] {signer});

        checkTransaction(tx,
                         "d10:attachmentde5:billsd23:EON-T-T3E22-2SG3U-EN8MJd10:attachmentde8:deadlinei3600e3:feei1e2:id23:EON-T-T3E22-2SG3U-EN8MJ6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:5ef562be95347e48689c4452aab8a1a8917673150b5097082587453c326672ad880212c5472b18fd344a5c0eea8a4349e8f5a942d003e198f8877c112c421a0f9:timestampi12345e4:typei1e7:versioni1ee23:EON-T-T3E22-2SVD9-JRXFPd10:attachmentde8:deadlinei3600e3:feei1e2:id23:EON-T-T3E22-2SVD9-JRXFP6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:679eebe9f4f31ead05743f35197f305d83dfa643e5165afe9ce23eae1fab65c89864b562c5bba485588e8e6ea054e37ce5cba081980bfe24a99fc029dffcd10f9:timestampi12345e4:typei2e7:versioni1eee13:confirmationsd21:EON-87VQX-TKLTD-3JJNK128:022319d6100ab74b3c56d8ab3220af4a3202b806c79954c1efc2e273027f619c84c3169d2e808f977576423f36c3c92cb9a73c56e3da6909080a00f7b47b2b01e8:deadlinei3600e3:feei1e2:id23:EON-T-T3E22-2JK2T-YRNHZ6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:022319d6100ab74b3c56d8ab3220af4a3202b806c79954c1efc2e273027f619c84c3169d2e808f977576423f36c3c92cb9a73c56e3da6909080a00f7b47b2b019:timestampi12345e4:typei1e7:versioni1ee");
    }

    //
    // Transaction Types
    //

    @Test
    public void transaction_payment() throws Exception {
        Transaction tx =
                PaymentBuilder.createNew(100L, new AccountID(12345L)).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd6:amounti100e9:recipient21:EON-T3E22-22222-22JUJe8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2WHN4-NLQ9Z6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:c2dfeb6ac50c4b57dc040cde1e1d8e0278fdf41ed0f33f85dc35b373c4bb9e2b8a1a9f7961c60164040bf9206e54b1ceb59f2b297fadb6cb7de9a4b84db0cd069:timestampi12345e4:typei200e7:versioni1ee");
    }

    @Test
    public void transaction_account_registration() throws Exception {
        Transaction tx = RegistrationBuilder.createNew(signer.getPublicKey())
                                            .validity(12345 + 60, 3600)
                                            .build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd21:EON-87VQX-TKLTD-3JJNK64:3ccd241cffc9b3618044b97d036d8614593d8b017c340f1dee8773385517654be8:deadlinei3600e3:feei10e2:id23:EON-T-P5E22-22PG6-TJT5K6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:1c0f79c4f2343c1d4e9146dad0ec189577f426a000480909d48af185641a8477d98cc01dba64a9f95d004051bc86b515158837dbec03ad8a4d7a244f05f52d0c9:timestampi12405e4:typei100e7:versioni1ee");
    }

    @Test
    public void transaction_deposit() throws Exception {
        Transaction tx = DepositBuilder.createNew(999L).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd6:amounti999ee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2NJTF-L3VJY6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:7f308da9be37183b992c0ec5d0f95ce1a15121e52fa912478a3f09aa8533b39d32d27dbff27f7a56372adb3e105730de747dd63f4d6f8b377b69e276639dd8029:timestampi12345e4:typei300e7:versioni1ee");
    }

    @Test
    public void transaction_quorum() throws Exception {
        Transaction tx = QuorumBuilder.createNew(80)
                                      .quorumForType(TransactionType.Quorum, 70)
                                      .validity(12345, 3600)
                                      .build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd3:410i70e3:alli80ee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2S6XQ-KFU7Z6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:beb3a8611efa87d1b15f88f38fed3af78e591f75da08a21ffb51abd5dfcd3226a69b885ca0484bab6efc24da9fc0ab04d5e6a6e325730b61727ea4a5bac72f069:timestampi12345e4:typei410e7:versioni1ee");
    }

    @Test
    public void transaction_delegate() throws Exception {
        Transaction tx =
                DelegateBuilder.createNew(new AccountID(1L), 10).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd21:EON-32222-22222-22J2Ji10ee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2AM4Z-7XABS6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:d9135734b591dd5fa3a72425f8c83585eb3db2de7125a5824fedba2d49a1f1b09893f6bde63dcce26129c252daabf35dd5b198c40683a4949be7d08f49b002089:timestampi12345e4:typei400e7:versioni1ee");
    }

    @Test
    public void transaction_rejection() throws Exception {
        Transaction tx = RejectionBuilder.createNew(new AccountID(1L)).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd7:account21:EON-32222-22222-22J2Je8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2AHKD-SMMDV6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:7b2d84808543249b73bcbc64edd621b3fba1e1a4aa54b5d630c5eb728beefeab7c0524d4f6da3d8410275321f3bf8f511648fd84eca92b1e0dfea9360de6f2069:timestampi12345e4:typei420e7:versioni1ee");
    }

    @Test
    public void transaction_account_publication() throws Exception {
        Transaction tx = PublicationBuilder.createNew("112233445566778899qqwweerrttyyuu")
                                           .validity(12345, 3600)
                                           .build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd4:seed32:112233445566778899qqwweerrttyyuue8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2NB58-W4GAQ6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:ff85b9a4648decd23278c23ef81b30d78de3aed266f06cdafc76203b393a5f8e2928c722644b3b87fb6ecdaf553e9e03ce4fa26c6f57e9ac0371520072ac12029:timestampi12345e4:typei430e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_registration() throws Exception {
        Transaction tx =
                ColoredCoinRegistrationBuilder.createNew(1000L, 2).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd7:decimali2e8:emissioni1000ee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2N25Z-D7HMX6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:3a38570bf334540f5da9c4bee56da69750f555433e53578a75b8c494bb8063382b1b2f807a379945f5c054ca23b3f6e67b572af3f15bb7f8122727414a3619019:timestampi12345e4:typei500e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_payment() throws Exception {
        Transaction tx = ColoredPaymentBuilder.createNew(100L, new ColoredCoinID(9999999L), new AccountID(1111L))
                                              .validity(12345, 3600)
                                              .build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd6:amounti100e5:color23:EON-C-ZM7KB-22222-22JBK9:recipient21:EON-R4322-22222-222DKe8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2JSLJ-ZJXLW6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:08f3cdbe70d25f37c7eb9be6d0bd8588ca001fbadf27d8f99e1864441237c340ff0dbc8751c16bc66f87931f69ffd0bcfcf93a55c70f8b738152a3d46a740e029:timestampi12345e4:typei510e7:versioni1ee");
    }

    @Test
    public void transaction_colored_coin_supply() throws Exception {
        Transaction tx = ColoredCoinSupplyBuilder.createNew(2000L).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentd6:supplyi2000ee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2NK38-BGB2W6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:de89b430767172e3ccf33c2fd7f4ff68dca08db68aa13e0e30c669b93e8e5aec0ecb5a06bf95b5e339251b1269e714c3bd3462150bb50f7b5a3e0f37b1b007029:timestampi12345e4:typei520e7:versioni1ee");
    }

    @Test
    public void transaction_complex_payment() throws Exception {
        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {
                PaymentBuilder.createNew(100L, new AccountID(0L)).validity(12345, 3600).build(networkID, signer)
        }).validity(12345, 3600).build(networkID, signer);

        checkTransaction(tx,
                         "d10:attachmentde5:billsd23:EON-T-T3E22-2SJA6-DC8NZd10:attachmentd6:amounti100e9:recipient21:EON-22222-22222-2222Je8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2SJA6-DC8NZ6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:b5da3562e28424455a87e72d2b851728339f964c05d8280999a5698fa410446e77abe55c112f00af2c00c8f0e9a81a6c668795cd054cf2a71332c65fbefba50b9:timestampi12345e4:typei200e7:versioni1eee8:deadlinei3600e3:feei10e2:id23:EON-T-T3E22-2AA6G-7HRFN6:sender21:EON-87VQX-TKLTD-3JJNK9:signature128:d5a027a64e5a15f7dace3f37c7edebf043569036f680a8304a5a245c3fc3f79520932ede9be10c8dba165fbde7b8d64b7d1e89e520dac2bafd4977a0ed6f510f9:timestampi12345e4:typei600e7:versioni1ee");
    }

    private void checkTransaction(Transaction tran, String data) throws IllegalArgumentException {
        byte[] bytes = bencode.encode(TransaportTransactionMapper.convert(tran));
        String s = new String(bytes);
        Assert.assertEquals(data, s);

        Map<String, Object> decoded = bencode.decode(data.getBytes(), Type.DICTIONARY);
        assertEquals(tran, TransaportTransactionMapper.convert(decoded));
    }

    private void assertEquals(Transaction a, Transaction b) {

        Assert.assertEquals(a.getType(), b.getType());
        Assert.assertEquals(a.getTimestamp(), b.getTimestamp());
        Assert.assertEquals(a.getDeadline(), b.getDeadline());
        Assert.assertEquals(a.getReference(), b.getReference());
        Assert.assertEquals(a.getSenderID(), b.getSenderID());
        Assert.assertEquals(a.getFee(), b.getFee());
        Assert.assertEquals(Format.convert(bencode.encode(a.getData())), Format.convert(bencode.encode(b.getData())));
        Assert.assertEquals(Format.convert(a.getSignature()), Format.convert(b.getSignature()));
        Assert.assertEquals(a.getID(), b.getID());
        Assert.assertEquals(a.getVersion(), b.getVersion());
        Assert.assertEquals(a.getConfirmations(), b.getConfirmations());
        Assert.assertEquals(a.hasNestedTransactions(), b.hasNestedTransactions());
        if (a.hasNestedTransactions() || b.hasNestedTransactions()) {
            Set<String> aSet = a.getNestedTransactions().keySet();
            Set<String> bSet = b.getNestedTransactions().keySet();
            Assert.assertEquals(aSet.size(), bSet.size());

            aSet.removeAll(bSet);
            Assert.assertEquals(aSet.size(), 0);
        }
    }
}
