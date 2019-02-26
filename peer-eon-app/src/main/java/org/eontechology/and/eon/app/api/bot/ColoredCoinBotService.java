package org.eontechology.and.eon.app.api.bot;

import java.io.IOException;

import org.eontechology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.ledger.LedgerProvider;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.tx.ColoredCoinID;

/**
 * Colored coin service.
 */
public class ColoredCoinBotService {

    private final IBlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;

    public ColoredCoinBotService(LedgerProvider ledgerProvider, IBlockchainProvider blockchain) {
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
    }

    /**
     * Get a colored coin information.
     *
     * @param id
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public Info getInfo(String id) throws RemotePeerException, IOException {
        Account account = getColoredAccount(id);

        Info info = new Info();
        info.state = State.Unauthorized;

        if (account == null) {
            return info;
        }

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
        if (!coloredCoin.isIssued()) {
            return info;
        }

        info.state = State.OK;
        info.decimal = coloredCoin.getAttributes().decimalPoint;
        info.timestamp = coloredCoin.getAttributes().timestamp;
        info.supply = coloredCoin.getMoneySupply();
        info.auto = (coloredCoin.getEmitMode() == ColoredCoinEmitMode.AUTO);

        return info;
    }

    Account getColoredAccount(String id) throws RemotePeerException {
        AccountID accountID;
        try {
            accountID = new ColoredCoinID(id).getIssierAccount();
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }

        final ILedger ledgerState = ledgerProvider.getLedger(blockchain.getLastBlock());
        return ledgerState.getAccount(accountID);
    }

    public static class State {

        /**
         * Colored coin is registered
         */
        public static final State OK = new State(200, "OK");

        /**
         * No associated colored coins
         */
        public static final State Unauthorized = new State(401, "Unauthorized");

        public final int code;
        public final String name;

        private State(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public static class Info {
        public State state;
        public Long supply;
        public Boolean auto;
        public Integer decimal;
        public Integer timestamp;
    }
}
