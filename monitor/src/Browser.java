import java.awt.Dimension;

import jakarta.websocket.Session;

public class Browser {
	private WS ws;
	private Session session;
	private String monitor_id;

	public void sendText(String text) {
		try {
			if(session.isOpen())
				session.getBasicRemote().sendText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String monitorId() {
		synchronized (this) {
			return monitor_id;
		}
	}

	public Browser(WS ws, Session session) {
		this.ws = ws;
		this.session = session;
	}
	
	public void onBrowser(String message) {
		String[] ope = message.split(" ");
		if(ope[0].equalsIgnoreCase("string")
		|| ope[0].equalsIgnoreCase("mousedown")
		|| ope[0].equalsIgnoreCase("mouseup")
		|| ope[0].equalsIgnoreCase("mousemove")
		|| ope[0].equalsIgnoreCase("mousewheel")
		|| ope[0].equalsIgnoreCase("dblclick")
		|| ope[0].equalsIgnoreCase("keydown")
		|| ope[0].equalsIgnoreCase("keyup")
		) {
			String id = null;
			synchronized (this) {
				id = monitor_id;
			}
			ws.client_sendText(id, message);
			return;
		}

		if(ope[0].equalsIgnoreCase("browser")) {
			synchronized (this) {
				ws.browser_sendClients();
			}
			return;
		}

		if(ope[0].equalsIgnoreCase("view")) {
			if(ope.length == 1) {
				synchronized (this) {
					monitor_id = null;
				}
				return;
			}

			try {
				Dimension size = ws.client_size(ope[1]);
				if(size != null) {
					session.getBasicRemote().sendText("screen " + size.width + " " + size.height);
					synchronized (this) {
						monitor_id = ope[1]; 
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
	}
}
