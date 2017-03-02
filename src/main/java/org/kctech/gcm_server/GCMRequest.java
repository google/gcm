package org.kctech.gcm_server;
/**
 * Per https://developers.google.com/cloud-messaging/http-server-ref#downstream-http-messages-json
 * 
 * fields are wrapper classes to allow them to be null
 * @author Ephraim
 *
 * @param <T> Type of data
 */
public abstract class GCMRequest<T> {
	private String to;
	private String[] registrationIds;
	private String collapseKey;
	private Priority priority;
	private Boolean contentAvailable;
	private Boolean delayWhileIdle;
	private Long timeToLive;
	private String restrictedPackageName;
	private Boolean dryRun;
	
	public GCMRequest(String to, String[] registrationIds, String collapseKey, Priority priority,
			Boolean contentAvailable, Boolean delayWhileIdle, Long timeToLive, String restrictedPackageName,
			Boolean dryRun) {
		this.to = to;
		this.registrationIds = registrationIds;
		this.collapseKey = collapseKey;
		this.priority = priority;
		this.contentAvailable = contentAvailable;
		this.delayWhileIdle = delayWhileIdle;
		this.timeToLive = timeToLive;
		this.restrictedPackageName = restrictedPackageName;
		this.dryRun = dryRun;
	}

	public GCMRequest(String to) {
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String[] getRegistrationIds() {
		return registrationIds;
	}

	public void setRegistrationIds(String[] registrationIds) {
		this.registrationIds = registrationIds;
	}

	public String getCollapseKey() {
		return collapseKey;
	}

	public void setCollapseKey(String collapseKey) {
		this.collapseKey = collapseKey;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public boolean isContentAvailable() {
		return contentAvailable;
	}

	public void setContentAvailable(boolean contentAvailable) {
		this.contentAvailable = contentAvailable;
	}

	public boolean isDelayWhileIdle() {
		return delayWhileIdle;
	}

	public void setDelayWhileIdle(boolean delayWhileIdle) {
		this.delayWhileIdle = delayWhileIdle;
	}

	public long getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	public String getRestrictedPackageName() {
		return restrictedPackageName;
	}

	public void setRestrictedPackageName(String restrictedPackageName) {
		this.restrictedPackageName = restrictedPackageName;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public abstract T getData();

	public static enum Priority {NORMAL, HIGH}
}
