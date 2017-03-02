package org.kctech.gcm_server;

public class GCMTopicResponse extends GCMResponse{
	private static final long serialVersionUID = -6061269111593859356L;

	private String messageId;
	private ResponseError error;
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public ResponseError getError() {
		return error;
	}
	public void setError(ResponseError error) {
		this.error = error;
	}
}
