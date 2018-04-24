package atj;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ApplicationScoped
@ServerEndpoint("/websocketendpoint")
public class WebSocketEndpoint {

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("Session with id: "+ session.getId() +" started");
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Session with id: "+ session.getId() +" closed");
	}

	@OnError
	public void onError(Throwable error) {
		System.out.println("Error: "+ error.getMessage());
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen()) {
					oneSession.getBasicRemote().sendText(message);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@OnMessage
	public void onMessage(ByteBuffer buffer,Session session) {
		try {
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen())
					if(oneSession.getId()!=session.getId()) {
						oneSession.getBasicRemote().sendBinary(buffer);
					}
				}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
} // public class WebSocketEndpoint