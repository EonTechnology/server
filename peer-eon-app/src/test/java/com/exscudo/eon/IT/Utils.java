package com.exscudo.eon.IT;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.cfg.Fork;
import com.exscudo.eon.cfg.ForkInitializer;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.store.sqlite.Backlog;
import com.exscudo.peer.store.sqlite.IInitializer;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

class Utils {

	public static Storage createStorage(IInitializer initializer)
			throws ClassNotFoundException, IOException, SQLException {
		Storage storage = Storage.create("jdbc:sqlite:", initializer);
		storage.setBacklog(new Backlog());
		return storage;
	}

	public static Storage createStorage() throws ClassNotFoundException, IOException, SQLException {
		return createStorage(new DefaultInitializer(new String[] { "/com/exscudo/eon/IT/genesis_block.sql" }));
	}

	public static Storage createStorage(String[] sripts) throws ClassNotFoundException, IOException, SQLException {
		return createStorage(new DefaultInitializer(sripts));
	}

	public static Block getLastBlock(Storage storage) {
		long lastBlockID = Long.parseLong(SettingHelper.getValue(storage.getConnection(), SettingName.lastBlockID), 10);
		return BlockHelper.get(storage.getConnection(), lastBlockID);
	}

	public static long getGenesisBlockID(Storage storage) {
		return Long.parseLong(SettingHelper.getValue(storage.getConnection(), SettingName.genesisBlockID), 10);
	}

	public static IFork createFork(Storage storage) {
		Fork fork = new Fork(Utils.getGenesisBlockID(storage), new Fork.Item[] { new Fork.Item(1,
				"2017-10-04T12:00:00.00Z", "2017-11-04T12:00:00.00Z", ForkInitializer.items[1].handler, 1) });
		return fork;
	}

}
