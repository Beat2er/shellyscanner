package it.usna.shellyscan.model.device.g2;

import java.util.function.Predicate;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.view.devsettings.PanelFWUpdate;

public class WebSocketDeviceListener extends WebSocketAdapter {
	public static String NOTIFY_STATUS = "NotifyStatus";
	public static String NOTIFY_FULL_STATUS = "NotifyFullStatus";
	public static String NOTIFY_EVENT = "NotifyEvent";
	private Predicate<JsonNode> notifyCondition;
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelFWUpdate.class);
	
	public WebSocketDeviceListener() {
		
	}
	
	public WebSocketDeviceListener(Predicate<JsonNode> condition) {
		this.notifyCondition = condition;
	}
	
	@Override
	public void onWebSocketConnect(Session session) {
		LOG.trace("sw-open");
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		LOG.trace("sw-close: reason: {}, status: {}", reason, statusCode);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		LOG.warn("sw-error", cause);
	}

	@Override
	public void onWebSocketText(String message) {
		try {
			JsonNode msg = JSON_MAPPER.readTree(message);
			if(notifyCondition == null || notifyCondition.test(msg)) {
				onMessage(msg);
			}
		} catch (JsonProcessingException e) {
			LOG.warn("sw-message-error: {}", message, e);
		}
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int length) {
		LOG.trace("sw-binary; length: {}", length );
	}
	
	public void onMessage(JsonNode msg) {
		System.out.println("M: " + msg);
	}
}

//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696108.45,"events":[{"component":"sys", "event":"ota_progress", "msg":"Waiting for data", "progress_percent":99, "ts":1677696108.45}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.49,"events":[{"component":"sys", "event":"ota_success", "msg":"Update applied, rebooting", "ts":1677696109.49}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.57,"events":[{"component":"sys", "event":"scheduled_restart", "time_ms": 435, "ts":1677696109.57}]}}