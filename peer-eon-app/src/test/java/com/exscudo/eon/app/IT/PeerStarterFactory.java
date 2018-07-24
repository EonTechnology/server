package com.exscudo.eon.app.IT;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exscudo.eon.app.cfg.Config;
import com.exscudo.eon.app.cfg.Fork;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.eon.app.cfg.forks.ForkItem;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.core.storage.Storage;
import org.mockito.Mockito;

class PeerStarterFactory {
    public static final long MIN_DEPOSIT_SIZE = 500000000L;

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private String dbUrl;
        private String seed;
        private boolean fullSync = true;
        private long minDepositSize = MIN_DEPOSIT_SIZE;
        private ForkItem[] items;
        private String endFork;
        private Map<Integer, ITransactionParser> parsers;

        public Builder db(String value) {
            this.dbUrl = value;
            return this;
        }

        public Builder seed(String value) {
            this.seed = value;
            return this;
        }

        public Builder disableFullSync() {
            this.fullSync = false;
            return this;
        }

        public Builder deposit(long minDepositSize) {
            this.minDepositSize = minDepositSize;
            return this;
        }

        public Builder route(int payment, ITransactionParser parser) {
            if (parsers == null) {
                parsers = new HashMap<>();
            }
            parsers.put(payment, parser);
            return this;
        }

        public PeerStarter build(TimeProvider timeProvider) throws SQLException, IOException, ClassNotFoundException {
            PeerStarter starter = new PeerStarter(createDefaultConfig(seed, fullSync));

            // context
            ExecutionContext context = starter.getExecutionContext();
            context.connectPeer(context.getAnyPeerToConnect(), 0);
            context = Mockito.spy(context);
            starter.setExecutionContext(Mockito.spy(context));

            // time provider
            starter.setTimeProvider(timeProvider);

            //fork
            Storage storage = starter.getStorage();

            if (items == null) {

                long time = Utils.getGenesisBlockTimestamp();

                endFork = Instant.ofEpochMilli((time + 10 * 180 * 1000) * 1000).toString();
                items = new ForkItem[] {new ForkItem(1, Instant.ofEpochMilli((time - 1) * 1000).toString())};
            }

            if (parsers != null) {
                for (ForkItem i : items) {
                    for (Map.Entry<Integer, ITransactionParser> p : parsers.entrySet()) {
                        i.addTxType(p.getKey(), p.getValue());
                    }
                }
            }

            List<String> validationRules = new ArrayList<String>() {
                {
                    add("com.exscudo.peer.core.middleware.rules.DeadlineValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.LengthValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.ReferencedTransactionValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.FeePerByteValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.VersionValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.NoteValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.PayerValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.TypeValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.ExpiredTimestampValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.FutureTimestampValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.ConfirmationsSetValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.ConfirmationsValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.SignatureValidationRule");
                    add("com.exscudo.peer.core.middleware.rules.NestedTransactionsValidationRule");
                }
            };
            for (ForkItem i : items) {
                i.setValidationRules(validationRules);
            }

            Fork fork = new Fork(Utils.getGenesisBlockID(), items, endFork);
            fork.setMinDepositSize(minDepositSize);

            starter.setFork(Mockito.spy(fork));
            starter.initialize();

            return starter;
        }

        public ForkBuilder beginFork(int number, String begin) {
            ForkBuilder forkBuilder = new ForkBuilder(this);
            return forkBuilder.nextItem(number, begin);
        }

        private Config createDefaultConfig(String seed, boolean fullSync) {

            Config config = new Config();

            config.setHost("0");
            config.setBlacklistingPeriod(30000);
            config.setPublicPeers(new String[] {"1"});
            config.setSeed(seed);
            config.setGenesisFile("./com/exscudo/eon/app/IT/genesis_block.json");
            config.setFullSync(fullSync);
            if (dbUrl == null) {
                config.setDbUrl(Utils.getDbUrl());
            } else {
                config.setDbUrl(dbUrl);
            }

            return config;
        }

        public static class ForkBuilder {
            private final Builder owner;
            private List<ForkItem> forkItems = new ArrayList<>();

            public ForkBuilder(Builder owner) {
                this.owner = owner;
            }

            public ForkBuilder nextItem(int number, String begin) {
                ForkItem i = new ForkItem(number, begin);
                forkItems.add(i);
                return this;
            }

            public Builder endFork(String end) {
                owner.items = forkItems.toArray(new ForkItem[0]);
                owner.endFork = end;
                return owner;
            }
        }
    }
}
