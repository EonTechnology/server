package com.exscudo.eon.IT;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import com.dampcake.bencode.Bencode;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.TransactionComparator;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.importer.IFork;
import com.exscudo.peer.core.ledger.Ledger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.IInitializer;
import com.exscudo.peer.core.storage.InitializerJson;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.eon.Fork;
import com.exscudo.peer.eon.ForkInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.mockito.Mockito;

class Utils {

    private static int DB = 1;

    public static Storage createStorage(IInitializer initializer) throws ClassNotFoundException, IOException, SQLException {
        Storage storage =
                Storage.create("jdbc:sqlite:file:memTestITDB" + DB + "?mode=memory&cache=shared", initializer);
        DB++;
        return storage;
    }

    public static Storage createStorage() throws ClassNotFoundException, IOException, SQLException {
        return createStorage(new InitializerJson("/com/exscudo/eon/IT/genesis_block.json"));
    }

    public static Block getLastBlock(Storage storage) throws SQLException {
        BlockID lastBlockID = storage.metadata().getLastBlockID();
        return storage.getBlockHelper().get(lastBlockID);
    }

    public static BlockID getGenesisBlockID(Storage storage) {
        return storage.metadata().getGenesisBlockID();
    }

    public static IFork createFork(Storage storage) {
        Fork fork = new Fork(Utils.getGenesisBlockID(storage), new Fork.Item[] {
                new Fork.Item(1,
                              "2017-10-04T12:00:00.00Z",
                              "2017-11-04T12:00:00.00Z",
                              ForkInitializer.items[0].handler,
                              1)
        });
        return Mockito.spy(fork);
    }

    public static void comparePeer(PeerContext ctx1, PeerContext ctx2) {

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        BlockID lastBlockID = ctx1.blockchain.getLastBlock().getID();
        Bencode bencode = new Bencode();

        while (lastBlockID.getValue() != 0) {
            Block block = ctx1.blockchain.getBlock(lastBlockID);
            Block blockNew = ctx2.blockchain.getBlock(lastBlockID);

            Assert.assertEquals(block.getVersion(), blockNew.getVersion());
            Assert.assertEquals(block.getTimestamp(), blockNew.getTimestamp());
            Assert.assertEquals(block.getPreviousBlock(), blockNew.getPreviousBlock());
            Assert.assertEquals(Format.convert(block.getGenerationSignature()),
                                Format.convert(blockNew.getGenerationSignature()));
            Assert.assertEquals(block.getSenderID(), blockNew.getSenderID());
            Assert.assertEquals(Format.convert(block.getSignature()), Format.convert(blockNew.getSignature()));
            Assert.assertEquals(block.getID(), blockNew.getID());
            Assert.assertEquals(block.getHeight(), blockNew.getHeight());
            Assert.assertEquals(block.getCumulativeDifficulty().toString(),
                                blockNew.getCumulativeDifficulty().toString());
            Assert.assertEquals(block.getSnapshot(), blockNew.getSnapshot());

            Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
            Transaction[] transactionsNew = blockNew.getTransactions().toArray(new Transaction[0]);

            Arrays.sort(transactions, new TransactionComparator());
            Arrays.sort(transactionsNew, new TransactionComparator());

            Assert.assertEquals(transactions.length, transactionsNew.length);

            for (int i = 0; i < transactions.length; i++) {
                Transaction transaction = transactions[i];
                Transaction transactionNew = transactionsNew[i];

                Assert.assertEquals(transaction.getType(), transactionNew.getType());
                Assert.assertEquals(transaction.getTimestamp(), transactionNew.getTimestamp());
                Assert.assertEquals(transaction.getDeadline(), transactionNew.getDeadline());
                Assert.assertEquals(transaction.getReference(), transactionNew.getReference());
                Assert.assertEquals(transaction.getSenderID(), transactionNew.getSenderID());
                Assert.assertEquals(transaction.getFee(), transactionNew.getFee());
                Assert.assertEquals(Format.convert(bencode.encode(transaction.getData())),
                                    Format.convert(bencode.encode(transactionNew.getData())));
                Assert.assertEquals(Format.convert(transaction.getSignature()),
                                    Format.convert(transactionNew.getSignature()));
                Assert.assertEquals(transaction.getID(), transactionNew.getID());
                Assert.assertEquals(transaction.getLength(), transactionNew.getLength());
            }

            lastBlockID = block.getPreviousBlock();
        }
    }

    public String getGenesisBlockAsJSON(Storage storage) throws Exception {

        HashMap<String, Object> map = new HashMap<>();

        Block lastBlock = getLastBlock(storage);

        map.put("timestamp", lastBlock.getTimestamp());
        map.put("signature", Format.convert(lastBlock.getSignature()));

        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        Ledger ledger = ledgerProvider.getLedger(lastBlock);

        HashMap<String, Object> accSet = new HashMap<>();
        for (Account account : ledger) {
            HashMap<String, Object> accMap = new HashMap<>();

            for (AccountProperty property : account.getProperties()) {
                accMap.put(property.getType(), property.getData());
            }

            accSet.put(account.getID().toString(), accMap);
        }
        map.put("accounts", accSet);

        String json = new ObjectMapper().writeValueAsString(map);
        return json;
    }
}
