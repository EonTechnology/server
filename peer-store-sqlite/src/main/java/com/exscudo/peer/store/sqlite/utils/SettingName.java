package com.exscudo.peer.store.sqlite.utils;

/**
 * Settings names
 */
public class SettingName {

	/**
	 * DB version
	 */
	public static final String dbVersion = "DB_VERSION";

	/**
	 * Step when database is updating
	 */
	public static final String dbUpdateStep = "DB_UPDATE_STEP";

	/**
	 * Genesis block ID
	 */
	public static final String genesisBlockID = "GENESIS_BLOCK_ID";

	/**
	 * Top block in blockchain
	 */
	public static final String lastBlockID = "LastBlockId";

	/**
	 * Begin time of current fork
	 */
	public static final String forkBegin = "FORK_BEGIN";

	/**
	 * End time of current fork
	 */
	public static final String forkEnd = "FORK_END";

	/**
	 * Supported transactions in last fork
	 */
	public static final String prevTranVersions = "PREV_TRAN_VERSIONS";

	/**
	 * Supported transactions in current fork
	 */
	public static final String targetTranVersions = "TARGET_TRAN_VERSIONS";
}
