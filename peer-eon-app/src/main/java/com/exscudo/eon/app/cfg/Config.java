package com.exscudo.eon.app.cfg;

public class Config {

    private String dbUrl = "";
    private String host = "";
    private int blacklistingPeriod = 300000;
    private String[] publicPeers = new String[0];
    private String[] innerPeers = new String[0];
    private int readTimeout = 20000;
    private int connectTimeout = 5000;
    private String seed = "";
    private String genesisFile = "";
    private String forksFile = "";
    private boolean fullSync = true;
    private String version = "";
    private boolean useCleaner = false;
    private int connectedPoolSize = 25;
    private boolean inner = false;

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getBlacklistingPeriod() {
        return blacklistingPeriod;
    }

    public void setBlacklistingPeriod(int blacklistingPeriod) {
        this.blacklistingPeriod = blacklistingPeriod;
    }

    public String[] getPublicPeers() {
        return publicPeers;
    }

    public void setPublicPeers(String[] publicPeers) {
        this.publicPeers = publicPeers;
    }

    public String[] getInnerPeers() {
        return innerPeers;
    }

    public void setInnerPeers(String[] innerPeers) {
        this.innerPeers = innerPeers;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getGenesisFile() {
        return genesisFile;
    }

    public void setGenesisFile(String genesisFile) {
        this.genesisFile = genesisFile;
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public void setFullSync(boolean fullSync) {
        this.fullSync = fullSync;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isUseCleaner() {
        return useCleaner;
    }

    public void setUseCleaner(boolean useCleaner) {
        this.useCleaner = useCleaner;
    }

    public int getConnectedPoolSize() {
        return connectedPoolSize;
    }

    public void setConnectedPoolSize(int connectedPoolSize) {
        this.connectedPoolSize = connectedPoolSize;
    }

    public boolean isInner() {
        return inner;
    }

    public void setInner(boolean inner) {
        this.inner = inner;
    }

    public String getForksFile() {
        return forksFile;
    }

    public void setForksFile(String forksFile) {
        this.forksFile = forksFile;
    }
}
