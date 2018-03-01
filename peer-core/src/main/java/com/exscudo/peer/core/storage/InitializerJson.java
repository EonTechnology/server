package com.exscudo.peer.core.storage;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.Ledger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.migrate.DBInitMigrateAction;
import com.exscudo.peer.core.storage.migrate.IMigrate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Basic implementation of the {@code IInitializer} interface.
 * <p>
 * Controls the version of the database and performs the necessary migration
 * actions.
 */
public class InitializerJson implements IInitializer {

    private final String genesisJson;

    public InitializerJson(String genesisJson) {

        this.genesisJson = genesisJson;
    }

    @Override
    public void initialize(Storage storage) throws IOException {
        storage.run(new IStorageAction() {
            @Override
            public void run(Connection connection, Storage.Metadata metadata) throws SQLException, IOException {
                initialize(connection, metadata);
            }
        });

        try {
            loadGenesis(storage);
        } catch (SQLException e) {
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private void loadGenesis(Storage storage) throws IOException, SQLException, URISyntaxException {

        if (storage.metadata().getProperty("GENESIS_BLOCK_ID") != null) {
            return;
        }

        Block zeroBlock = new Block() {
            @Override
            public BlockID getID() {
                return new BlockID(0);
            }
        };

        zeroBlock.setVersion(-1);
        zeroBlock.setTimestamp(0);
        zeroBlock.setPreviousBlock(new BlockID(0));
        zeroBlock.setSenderID(new AccountID(0));
        zeroBlock.setGenerationSignature(new byte[0]);
        zeroBlock.setSignature(new byte[0]);
        zeroBlock.setHeight(-1);
        zeroBlock.setCumulativeDifficulty(BigInteger.ZERO);
        zeroBlock.setTransactions(new ArrayList<>());
        zeroBlock.setSnapshot("");
        storage.getBlockHelper().save(zeroBlock);
        storage.getBlockchainHelper().attachBlock(zeroBlock.getID());

        byte[] jsonData = Files.readAllBytes(Paths.get(this.getClass().getResource(genesisJson).toURI()));
        Map<String, Object> map = new HashMap<>();
        map = new ObjectMapper().readValue(jsonData, new TypeReference<Map<String, Object>>() {
        });

        Block block = new Block();
        block.setVersion(-1);
        block.setTimestamp(Integer.parseInt(map.get("timestamp").toString()));
        block.setPreviousBlock(new BlockID(0));
        block.setSenderID(new AccountID(0));
        block.setGenerationSignature(new byte[64]);
        block.setSignature(Format.convert(map.get("signature").toString()));
        block.setHeight(0);
        block.setCumulativeDifficulty(BigInteger.ZERO);
        block.setTransactions(new ArrayList<>());

        Map<String, Object> accSetMap = (Map<String, Object>) map.get("accounts");
        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        Ledger ledger = ledgerProvider.getLedger(block);

        for (String id : accSetMap.keySet()) {
            Map<String, Object> accMap = (Map<String, Object>) accSetMap.get(id);
            Account acc = new Account(new AccountID(id));
            for (String p : accMap.keySet()) {
                Map<String, Object> data = (Map<String, Object>) accMap.get(p);
                AccountProperty property = new AccountProperty(p, data);
                acc = acc.putProperty(property);
            }

            ledger = ledger.putAccount(acc);
        }

        block.setSnapshot(ledger.getHash());

        ledgerProvider.addLedger(ledger);
        storage.getBlockHelper().save(block);
        storage.getBlockchainHelper().attachBlock(block.getID());

        storage.metadata().setProperty("GENESIS_BLOCK_ID", Long.toString(block.getID().getValue()));
        storage.metadata().setProperty("LastBlockId", Long.toString(block.getID().getValue()));
    }

    private void initialize(Connection connection, Storage.Metadata metadata) throws IOException, SQLException {

        List<IMigrate> migrates = new ArrayList<>();
        migrates.add(new DBInitMigrateAction(connection));

        int db_version = metadata.getVersion();

        List<IMigrate> actualMigrates = new ArrayList<>();
        for (IMigrate migrate : migrates) {
            if (migrate.getTargetVersion() > db_version) {
                actualMigrates.add(migrate);
            }
        }

        // Sorting by targetVersion
        actualMigrates.sort(Comparator.comparingInt(IMigrate::getTargetVersion));

        if (actualMigrates.size() == 0) {
            return;
        }

        // Step 1 - migrating the Database Structure
        for (IMigrate migrate : actualMigrates) {
            migrate.migrateDataBase();
        }

        // Step 2 - data migration
        for (IMigrate migrate : actualMigrates) {
            migrate.migrateLogicalStructure();
        }

        // Step 3 - cleaning after migration
        for (IMigrate migrate : actualMigrates) {
            migrate.cleanUp();
        }

        IMigrate lastMigration = actualMigrates.get(actualMigrates.size() - 1);
        int lastVersion = lastMigration.getTargetVersion();

        metadata.setVersion(lastVersion);
    }
}
