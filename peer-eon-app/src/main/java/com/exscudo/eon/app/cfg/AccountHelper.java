package com.exscudo.eon.app.cfg;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.IAccountHelper;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

public class AccountHelper implements IAccountHelper {

    private final Fork fork;

    public AccountHelper(Fork fork) {
        this.fork = fork;
    }

    @Override
    public boolean validateGenerator(Account generator, int timestamp) {

        if (generator == null) {
            return false;
        }

        GeneratingBalanceProperty deposit = AccountProperties.getDeposit(generator);
        if (deposit.getValue() < fork.getMinDepositSize()) {
            return false;
        }

        if (timestamp - deposit.getTimestamp() < Constant.SECONDS_IN_DAY && deposit.getTimestamp() != 0) {
            return false;
        }

        return true;
    }

    @Override
    public BigInteger getDifficultyAddition(Block block, Account generator, int timestamp) {
        byte[] generationSignatureHash;
        try {
            generationSignatureHash = MessageDigest.getInstance("SHA-512").digest(block.getGenerationSignature());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        BigInteger hit = new BigInteger(1, new byte[] {
                generationSignatureHash[7],
                generationSignatureHash[6],
                generationSignatureHash[5],
                generationSignatureHash[4],
                generationSignatureHash[3],
                generationSignatureHash[2],
                generationSignatureHash[1],
                generationSignatureHash[0]
        });

        // Decimal points in 1 EON - 1000000L
        Long scale = AccountProperties.getDeposit(generator).getValue() / 1000000L;
        if (scale != 0) {
            hit = hit.divide(BigInteger.valueOf(scale));
        }
        return Format.TWO64.divide(hit);
    }

    @Override
    public Account reward(Account account, long totalFee, int timestamp) {

        BalanceProperty balance = AccountProperties.getBalance(account);
        long oldBalance = balance.getValue();
        long newBalance = oldBalance + totalFee;
        balance = balance.setValue(newBalance);
        return AccountProperties.setProperty(account, balance);
    }

    @Override
    public Set<AccountID> getConfirmingAccounts(Account sender, int timestamp) {
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        if (validationMode.isMultiFactor()) {
            HashSet<AccountID> set = new HashSet<>();
            for (Map.Entry<AccountID, Integer> entry : validationMode.delegatesEntrySet()) {
                AccountID id = entry.getKey();
                set.add(id);
            }
            return set;
        }

        return null;
    }

    @Override
    public boolean validConfirmation(Transaction transaction, Map<AccountID, Account> set, int timestamp) {
        Account sender = set.get(transaction.getSenderID());
        if (sender == null) {
            throw new IllegalArgumentException();
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        int maxWeight = 0;
        for (Map.Entry<AccountID, Account> e : set.entrySet()) {

            AccountID id = e.getKey();

            if (transaction.getSenderID().equals(id)) {
                maxWeight += validationMode.getBaseWeight();
            } else if (validationMode.containWeightForAccount(id)) {
                maxWeight += validationMode.getWeightForAccount(id);
            }
        }

        if (validationMode.quorumForType(transaction.getType()) <= maxWeight ||
                (maxWeight == validationMode.getMaxWeight() && maxWeight != 0)) {
            return true;
        }

        return false;
    }

    @Override
    public <T> boolean verifySignature(T obj, byte[] signature, Account account, int timestamp) {

        byte[] publicKey = AccountProperties.getRegistration(account).getPublicKey();
        return CryptoProvider.getInstance().verify(obj, fork.getGenesisBlockID(), signature, publicKey);
    }
}
