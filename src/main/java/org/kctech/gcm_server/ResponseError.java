package org.kctech.gcm_server;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResponseError{
	MISSING_REG_TOKEN("MissingRegistration"),
	INVALID_REG_TOKEN("InvalidRegistration"),
	UNREGISTERED_DEV("NotRegistered"),
	INVALID_PACKAGE_NAME("InvalidPackageName"),
	MISMATCHED_SENDER("MismatchSenderId"),
	MESSAGE_TOO_BIG("MessageTooBig"),
	INVALID_DATA_KEY("InvalidDataKey"),
	INVALID_TTL("InvalidTtl"),
	TIMEOUT("Unavailable"),
	INTERNAL_SERV_ERR("InternalServerError"),
	DEV_MESSAGE_RATE_EXCEEDED("DeviceMessageRateExceeded"),
	TOPIC_MESSAGE_RATE_EXCEEDED("TopicsMessageRateExceeded");
	
	private String errCode;
	
	ResponseError(String errCode){
		this.errCode = errCode;
	}

	@JsonValue
	public String getErrCode() {
		return errCode;
	}

}