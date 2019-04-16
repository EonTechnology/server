package org.eontechnology.and.eon.app.IT;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.TransactionComparator;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.junit.Assert;

public class Utils {
    public static final long MIN_DEPOSIT_SIZE = 500000000L;
    private static int DB = 1;

    public static String getDbUrl() {
        String url = "jdbc:sqlite:file:memTestITDB" + DB + "?mode=memory&cache=shared";
        DB++;
        return url;
    }

    public static long getGenesisBlockTimestamp() throws IOException {

        URI uri;
        try {
            uri = Utils.class.getClassLoader()
                             .getResource("org/eontechnology/and/eon/app/IT/genesis_block.json")
                             .toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        byte[] json = Files.readAllBytes(Paths.get(uri));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> genesisBlock = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        return Integer.parseInt(genesisBlock.get("timestamp").toString());
    }

    public static BlockID getGenesisBlockID() throws IOException {

        URI uri;
        try {
            uri = Utils.class.getClassLoader()
                             .getResource("org/eontechnology/and/eon/app/IT/genesis_block.json")
                             .toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        byte[] json = Files.readAllBytes(Paths.get(uri));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> genesisBlock = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        byte[] signature = Format.convert(genesisBlock.get("signature").toString());
        int timestamp = Integer.parseInt(genesisBlock.get("timestamp").toString());
        return new BlockID(signature, timestamp);
    }

    public static void comparePeer(PeerContext ctx1, PeerContext ctx2) throws Exception {

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());

        BlockID lastBlockID = ctx1.blockExplorerService.getLastBlock().getID();
        Bencode bencode = new Bencode();

        while (lastBlockID.getValue() != 0) {
            Block block = ctx1.blockExplorerService.getById(lastBlockID);
            Block blockNew = ctx2.blockExplorerService.getById(lastBlockID);

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

    public String getGenesisBlockAsJSON(Block lastBlock, LedgerProvider ledgerProvider) throws Exception {

        HashMap<String, Object> map = new HashMap<>();

        map.put("timestamp", lastBlock.getTimestamp());
        map.put("signature", Format.convert(lastBlock.getSignature()));

        ILedger ledger = ledgerProvider.getLedger(lastBlock);

        HashMap<String, Object> accSet = new HashMap<>();
        for (Account account : ledger) {
            HashMap<String, Object> accMap = new HashMap<>();

            for (AccountProperty property : account.getProperties()) {
                accMap.put(property.getType(), property.getData());
            }

            accSet.put(account.getID().toString(), accMap);
        }
        map.put("accounts", accSet);

        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
    }
}
