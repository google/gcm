package org.kctech.gcm_server;

public class GCMDownstreamResponse extends GCMResponse{
	private static final long serialVersionUID = -7276813668005148954L;
	
	private long multicastId;
	private int success;
	private int failure;
	private int canonicalIds;
	private ResponseResult[] results;
	
	public GCMDownstreamResponse(long multicastId, int success, int failure, int canonicalIds,
			ResponseResult[] results) {
		super();
		this.multicastId = multicastId;
		this.success = success;
		this.failure = failure;
		this.canonicalIds = canonicalIds;
		this.results = results;
	}

	public GCMDownstreamResponse() {
	}

	public long getMulticastId() {
		return multicastId;
	}

	public void setMulticastId(long multicastId) {
		this.multicastId = multicastId;
	}

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public int getFailure() {
		return failure;
	}

	public void setFailure(int failure) {
		this.failure = failure;
	}

	public int getCanonicalIds() {
		return canonicalIds;
	}

	public void setCanonicalIds(int canonicalIds) {
		this.canonicalIds = canonicalIds;
	}

	public ResponseResult[] getResults() {
		return results;
	}

	public void setResults(ResponseResult[] results) {
		this.results = results;
	}

	public static class ResponseResult{
		private String messageId;
		private String registrationId;
		private ResponseError error;
		
		
		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}
		public void setRegistrationId(String registrationId) {
			this.registrationId = registrationId;
		}
		public void setError(ResponseError error) {
			this.error = error;
		}
		public String getMessageId() {
			return messageId;
		}
		public String getRegistrationId() {
			return registrationId;
		}
		public ResponseError getError() {
			return error;
		}
	}
}
