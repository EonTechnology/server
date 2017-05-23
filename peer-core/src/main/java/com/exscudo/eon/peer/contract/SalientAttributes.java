package com.exscudo.eon.peer.contract;

public class SalientAttributes {
	
	private String announcedAddress;
	private String hallmark;
	private String application;
	private long peerID;
	private String version;

	public String getAnnouncedAddress() {
		return announcedAddress;
	}

	public void setAnnouncedAddress(String announcedAddress) {
		this.announcedAddress = announcedAddress;
	}

	public String getHallmark() {
		return hallmark;
	}

	public void setHallmark(String hallmark) {
		this.hallmark = hallmark;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public long getPeerId() {
		return peerID;
	}

	public void setPeerId(long peerID) {
		this.peerID = peerID;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
