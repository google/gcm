package org.kctech.gcm_server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GCMServer {
	private final static Pattern TOPIC_REGEX = Pattern.compile("/topics/[a-zA-Z0-9-_.~%]+");
	private final static URI GCM_ADDR;
	static {
		try {
			GCM_ADDR = new URI("https://gcm-http.googleapis.com/gcm/send");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);// shouldn't happen
		}
	}
	private final Logger log = LoggerFactory.getLogger(GCMServer.class);

	private RestTemplate rest;

	private final HttpHeaders DEFAULT_HEADERS;

	protected static final String UTF8 = "UTF-8";
	
	private static final int retries = 12;

	/**
	 * Initial delay before first retry, without jitter.
	 */
	protected static final int BACKOFF_INITIAL_DELAY = 1000;
	/**
	 * Maximum delay before a retry.
	 */
	protected static final int MAX_BACKOFF_DELAY = 1024000;

	protected final Random random = new Random();

	@Autowired
	public GCMServer(RestTemplate rest, @Value("${gcmserverkey}") String googleKey) {
		this.rest = rest;
		HttpHeaders temp = new HttpHeaders();
		temp.add(HttpHeaders.AUTHORIZATION, "key=" + googleKey);
		temp.setContentType(MediaType.APPLICATION_JSON_UTF8);
		this.DEFAULT_HEADERS = HttpHeaders.readOnlyHttpHeaders(temp);
	}

	public <T> void sendToTopic(String topic, T message) {
		sendToTopic(topic, message, null);
	}

	public <T> void sendToTopic(String topic, T message, Class<?> serializatonView) {
		if (topic == null)
			throw new IllegalArgumentException("topic argument cannot be null");
		if (!TOPIC_REGEX.matcher(topic).matches())
			throw new IllegalArgumentException("Topic must fit Regex: /topics/[a-zA-Z0-9-_.~%]+");

		log.trace("sent to topic %s requested", topic);
		SimpleGCMRequest<T> req = new SimpleGCMRequest<>(topic);
		req.setData(message);

		GCMTopicResponse resp = sendToServer(req, GCMTopicResponse.class, serializatonView);
		log.debug("Received response for %d", resp.getMessageId());
	}

	public <T> void sendToDownstream(String to, String[] registrationIds, String collapseKey,
			GCMRequest.Priority priority, Boolean contentAvailable, Boolean delayWhileIdle, Long timeToLive,
			String restrictedPackageName, Boolean dryRun, T data, Class<?> serializationView) {
		if (StringUtils.isEmpty(to))
			throw new IllegalArgumentException("to field cannot be empty or null");
		SimpleGCMRequest<T> req = new SimpleGCMRequest<T>(to, registrationIds, collapseKey, priority, contentAvailable,
				delayWhileIdle, timeToLive, restrictedPackageName, dryRun, data);
		GCMDownstreamResponse resp = sendToServer(req, GCMDownstreamResponse.class, serializationView);
		log.debug("received response: %d", resp.getMulticastId());
	}

	public <T> void sendToDownstream(String to, T data, Class<?> serializationView) {
		sendToDownstream(to, null, null, null, null, null, null, null, null, data, serializationView);
	}

	private <RESP extends GCMResponse> RESP sendToServer(GCMRequest<?> req, Class<RESP> respType,
			Class<?> serializationView) {
		MappingJacksonValue value = new MappingJacksonValue(req);
		if (serializationView != null)
			value.setSerializationView(serializationView);

		HttpEntity<?> reqEntity = new HttpEntity<>(value, DEFAULT_HEADERS);

		ResponseEntity<RESP> response = sendWithRetries(reqEntity, respType);
		if(!response.getStatusCode().is2xxSuccessful())
			log.error("Sending failed %n%s", response);
		return response.getBody();
	}

	private <RESP> ResponseEntity<RESP> sendWithRetries(HttpEntity<?> reqEntity, Class<RESP> respType){
		int attempt = 0;
		ResponseEntity<RESP> result = null;
		int backoff = BACKOFF_INITIAL_DELAY;
		boolean tryAgain;
		do {
			attempt++;
			log.trace("Attempt #%d to send message %s", attempt, reqEntity);
			result = rest.postForEntity(GCM_ADDR, reqEntity, respType);
			tryAgain = !result.getStatusCode().is2xxSuccessful() && attempt <= retries;
			if (tryAgain) {
				int sleepTime = backoff / 2 + random.nextInt(backoff);
				sleep(sleepTime);
				if (2 * backoff < MAX_BACKOFF_DELAY) {
					backoff *= 2;
				}
			}
		} while (tryAgain);
		return result;
	}

	void sleep(long millis) {
	    try {
	      Thread.sleep(millis);
	    } catch (InterruptedException e) {
	      Thread.currentThread().interrupt();
	    }
	  }
	
	private static class SimpleGCMRequest<T> extends GCMRequest<T> {
		private T data;

		public SimpleGCMRequest(String to, String[] registrationIds, String collapseKey, GCMRequest.Priority priority,
				Boolean contentAvailable, Boolean delayWhileIdle, Long timeToLive, String restrictedPackageName,
				Boolean dryRun, T data) {
			super(to, registrationIds, collapseKey, priority, contentAvailable, delayWhileIdle, timeToLive,
					restrictedPackageName, dryRun);
			this.data = data;
		}

		public SimpleGCMRequest(String to) {
			super(to);
		}

		public void setData(T data) {
			this.data = data;
		}

		@Override
		public T getData() {
			return data;
		}
	}
}
