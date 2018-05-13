package com.exscudo.eon.app.IT;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.app.cfg.Config;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.env.ExecutionContext;
import org.mockito.Mockito;

class PeerStarterFactory {

    private static int DB = 1;

    static PeerStarter create(Config config,
                              TimeProvider timeProvider) throws SQLException, IOException, ClassNotFoundException {

        PeerStarter starter = new PeerStarter(config);
        starter.setTimeProvider(timeProvider);
        ExecutionContext context = starter.getExecutionContext();
        context.connectPeer(context.getAnyPeerToConnect(), 0);
        context = Mockito.spy(context);
        starter.setExecutionContext(Mockito.spy(context));

        return starter;
    }

    static PeerStarter create(String seed,
                              TimeProvider timeProvider) throws SQLException, IOException, ClassNotFoundException {
        return create(seed, timeProvider, true);
    }

    static PeerStarter create(String seed,
                              TimeProvider timeProvider,
                              boolean fullSync) throws SQLException, IOException, ClassNotFoundException {
        PeerStarter ps = create(createDefaultConfig(seed, fullSync), timeProvider);
        ps.setFork(Utils.createFork(ps.getStorage()));
        return ps;
    }

    private static Config createDefaultConfig(String seed, boolean fullSync) {

        Config config = new Config();

        config.setHost("0");
        config.setBlacklistingPeriod(30000);
        config.setPublicPeers(new String[] {"1"});
        config.setSeed(seed);
        config.setGenesisFile("./com/exscudo/eon/app/IT/genesis_block.json");
        config.setFullSync(fullSync);
        config.setDbUrl("jdbc:sqlite:file:memTestITDB" + DB + "?mode=memory&cache=shared");
        DB++;

        return config;
    }
}
