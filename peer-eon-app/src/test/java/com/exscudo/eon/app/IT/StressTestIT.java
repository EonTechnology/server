package com.exscudo.eon.app.IT;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.Signer;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParserV1;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParserV1;
import com.exscudo.peer.eon.midleware.parsers.PaymentParser;
import com.exscudo.peer.eon.midleware.parsers.RegistrationParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StressTestIT {

    private static final int NEW_ACC_COUNT = 5000;
    private static ISigner[] NEW_SIGNERS = new ISigner[NEW_ACC_COUNT];
    private static int THREAD_COUNT = 5;
    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String SENDER = "9e641020d3803008bf4e8a15ad05f84fb8eb3220037322ebc5fa58b70c3f1bd1";
    private static int DEAD_LINE = 3 * 60 * 60;
    private static TestTimeProvider mockTimeProvider;
    private static PeerContext ctx;
    private static PeerContext ctx2;
    private static List<Transaction> txSet;

    @BeforeClass
    public static void init() throws Exception {
        mockTimeProvider = new TestTimeProvider();

        ctx = new PeerContext(PeerStarterFactory.create()
                                                .seed(GENERATOR)
                                                .db("jdbc:sqlite:tmp_" + UUID.randomUUID().toString() + ".db")
                                                .route(TransactionType.Payment, new PaymentParser())
                                                .route(TransactionType.Registration, new RegistrationParser())
                                                .route(TransactionType.ColoredCoinRegistration,
                                                       new ColoredCoinRegistrationParserV1())
                                                .route(TransactionType.ColoredCoinPayment,
                                                       new ColoredCoinPaymentParser())
                                                .route(TransactionType.ColoredCoinSupply,
                                                       new ColoredCoinSupplyParserV1())
                                                .build(mockTimeProvider));
        ctx2 = new PeerContext(PeerStarterFactory.create()
                                                 .seed(GENERATOR)
                                                 .db("jdbc:sqlite:tmp_" + UUID.randomUUID().toString() + ".db")
                                                 .route(TransactionType.Payment, new PaymentParser())
                                                 .route(TransactionType.Registration, new RegistrationParser())
                                                 .route(TransactionType.ColoredCoinRegistration,
                                                        new ColoredCoinRegistrationParserV1())
                                                 .route(TransactionType.ColoredCoinPayment,
                                                        new ColoredCoinPaymentParser())
                                                 .route(TransactionType.ColoredCoinSupply,
                                                        new ColoredCoinSupplyParserV1())
                                                 .build(mockTimeProvider));

        ctx2.setPeerToConnect(ctx);

//        ctx.backlogCleaner.setSyncClearing(true);
//        ctx2.backlogCleaner.setSyncClearing(true);

        int time = ctx.blockExplorerService.getLastBlock().getTimestamp();
        mockTimeProvider.set(time);
    }

    private static void waitEnd(AtomicInteger queueLength) throws InterruptedException {

        long begin = System.currentTimeMillis();

        int s = queueLength.get();
        int diff;
        while (s != 0) {
            int s2 = queueLength.get();
            diff = s - s2;
            s = s2;
            long end = System.currentTimeMillis();
            System.out.println("Pool size: " + s + " - " + (end - begin) + "ms - " + diff);
            Thread.sleep(1000);
        }
    }

    private List<Transaction> genTx(ITranGenerator generator) throws InterruptedException {
        List<Transaction> txSet = Collections.synchronizedList(new LinkedList<>());

        long begin = System.currentTimeMillis();

        AtomicInteger queueLength = new AtomicInteger();
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < NEW_ACC_COUNT; i++) {
            final ISigner newSigner = NEW_SIGNERS[i];
            queueLength.incrementAndGet();

            threadPool.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        Transaction tx = generator.getTx(newSigner);
                        txSet.add(tx);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        queueLength.decrementAndGet();
                    }
                }
            });
        }
        waitEnd(queueLength);
        threadPool.shutdown();

        long diff = System.currentTimeMillis() - begin;
        Loggers.debug(StressTestIT.class, "genTx timing: {}ms", diff / 1000.0);

        return txSet;
    }

    private void putAll(List<Transaction> txs) throws IOException {
        long begin = System.currentTimeMillis();

        for (Transaction tx : txs) {
            ctx.transactionBotService.putTransaction(tx);
        }

        long diff = System.currentTimeMillis() - begin;
        Loggers.debug(StressTestIT.class, "putAll timing: {}ms", diff / 1000.0);
    }

    private void generateAll() throws IOException, InterruptedException {
        long begin = System.currentTimeMillis();

        int time = ctx.blockExplorerService.getLastBlock().getTimestamp();
        while (ctx.backlog.size() > 0) {

            time += Constant.BLOCK_PERIOD;
            mockTimeProvider.set(time + 1);
            ctx.generateBlockForNow();
            int size = 0;
            int size2 = 0;
            do {
                size = size2;
                Thread.sleep(1000);
                size2 = ctx.backlog.size();
            } while (size != size2 && size2 < 4000);
        }

        long diff = System.currentTimeMillis() - begin;
        Loggers.debug(StressTestIT.class, "generateAll timing: {}ms", diff / 1000.0);
    }

    private void syncAll() throws IOException {
        long begin = System.currentTimeMillis();

        ctx2.fullBlockSync();

        long diff = System.currentTimeMillis() - begin;
        Loggers.debug(StressTestIT.class, "syncAll timing: {}ms", diff / 1000.0);
    }

    @Test
    public void step_0_gen_acc() throws InterruptedException {

        Random r = new Random(132);

        AtomicInteger queueLength = new AtomicInteger();
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < NEW_ACC_COUNT; i++) {
            queueLength.incrementAndGet();

            final byte[] new_seed = new byte[32];
            r.nextBytes(new_seed);

            final int I = i;

            threadPool.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        ISigner newSigner = Signer.createNew(Format.convert(new_seed));
                        NEW_SIGNERS[I] = newSigner;
                    } finally {
                        queueLength.decrementAndGet();
                    }
                }
            });
        }
        waitEnd(queueLength);

        threadPool.shutdown();
    }

    @Test
    public void step_1_1_gen_tx_register() throws Exception {
        final ISigner fromSigner = Signer.createNew(SENDER);
        int time = ctx.blockExplorerService.getLastBlock().getTimestamp() + 1;

        txSet = genTx(new ITranGenerator() {
            private AtomicInteger i = new AtomicInteger(0);

            @Override
            public Transaction getTx(ISigner signer) throws Exception {
                return RegistrationBuilder.createNew(signer.getPublicKey())
                                          .validity(time -
                                                            i.incrementAndGet() * Constant.BLOCK_PERIOD /
                                                                    Constant.BLOCK_TRANSACTION_LIMIT, DEAD_LINE)
                                          .build(ctx.getNetworkID(), fromSigner);
            }
        });
    }

    @Test
    public void step_1_2_put_tx_register() throws Exception {
        putAll(txSet);
    }

    @Test
    public void step_1_3_gen_block_register() throws Exception {
        generateAll();
    }

    @Test
    public void step_1_3_sync_block_register() throws Exception {
        syncAll();
    }

    @Test
    public void step_2_1_gen_tx_payment() throws Exception {
        final ISigner fromSigner = Signer.createNew(SENDER);
        int time = ctx.blockExplorerService.getLastBlock().getTimestamp() + 1;

        txSet = genTx(new ITranGenerator() {
            private AtomicInteger i = new AtomicInteger(0);

            @Override
            public Transaction getTx(ISigner signer) throws Exception {
                return PaymentBuilder.createNew(1000, new AccountID(signer.getPublicKey()))
                                     .validity(time -
                                                       i.incrementAndGet() * Constant.BLOCK_PERIOD /
                                                               Constant.BLOCK_TRANSACTION_LIMIT, DEAD_LINE)
                                     .build(ctx.getNetworkID(), fromSigner);
            }
        });
    }

    @Test
    public void step_2_2_put_tx_payment() throws Exception {
        putAll(txSet);
    }

    @Test
    public void step_2_3_gen_block_payment() throws Exception {
        generateAll();
    }

    @Test
    public void step_2_3_sync_block_payment() throws Exception {
        syncAll();
    }

    @Test
    public void step_3_1_gen_tx_payment() throws Exception {
        final ISigner fromSigner = Signer.createNew(SENDER);
        final ISigner feePayer = Signer.createNew(GENERATOR);
        final AccountID feeAccountID = new AccountID(feePayer.getPublicKey());
        int time = ctx.blockExplorerService.getLastBlock().getTimestamp() + 1;

        txSet = genTx(new ITranGenerator() {
            private AtomicInteger i = new AtomicInteger(0);

            @Override
            public Transaction getTx(ISigner signer) throws Exception {
                return PaymentBuilder.createNew(1000, new AccountID(signer.getPublicKey()))
                                     .payedBy(feeAccountID)
                                     .validity(time -
                                                       i.incrementAndGet() * Constant.BLOCK_PERIOD /
                                                               Constant.BLOCK_TRANSACTION_LIMIT, DEAD_LINE)
                                     .build(ctx.getNetworkID(), fromSigner, new ISigner[] {feePayer});
            }
        });
    }

    @Test
    public void step_3_2_put_tx_payment() throws Exception {
        putAll(txSet);
    }

    @Test
    public void step_3_3_gen_block_payment() throws Exception {
        generateAll();
    }

    @Test
    public void step_3_3_sync_block_payment() throws Exception {
        syncAll();
    }

    @Test
    public void step_4_1_gen_tx_payment() throws Exception {
        final ISigner fromSigner = Signer.createNew(SENDER);
        final AccountID accountID = new AccountID(fromSigner.getPublicKey());
        int time = ctx.blockExplorerService.getLastBlock().getTimestamp() + 1;

        txSet = genTx(new ITranGenerator() {
            private AtomicInteger i = new AtomicInteger(0);

            @Override
            public Transaction getTx(ISigner signer) throws Exception {
                return PaymentBuilder.createNew(10, accountID)
                                     .validity(time -
                                                       i.incrementAndGet() * Constant.BLOCK_PERIOD /
                                                               Constant.BLOCK_TRANSACTION_LIMIT, DEAD_LINE)
                                     .build(ctx.getNetworkID(), signer);
            }
        });
    }

    @Test
    public void step_4_2_put_tx_payment() throws Exception {
        putAll(txSet);
    }

    @Test
    public void step_4_3_gen_block_payment() throws Exception {
        generateAll();
    }

    @Test
    public void step_4_3_sync_block_payment() throws Exception {
        syncAll();
    }

    private interface ITranGenerator {
        Transaction getTx(ISigner signer) throws Exception;
    }

    private static class TestTimeProvider implements ITimeProvider {
        int value = 0;

        @Override
        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }
}
