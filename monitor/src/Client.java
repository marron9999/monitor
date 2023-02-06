import java.awt.Dimension;
import java.awt.image.BufferedImage;

import jakarta.websocket.Session;

public class Client {
	private WS ws;
	private Session session;
	private Monitor monitor;

	public void sendText(String text) {
		try {
			if(session.isOpen())
				session.getBasicRemote().sendText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public BufferedImage image() {
		try {
			return monitor.image();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public void image(BufferedImage img) {
		try {
			monitor.image(img);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String name() {
		try {
			return monitor.name();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public Dimension size() {
		try {
		return monitor.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Client(WS ws, Session session) {
		this.ws = ws;
		this.session = session;
	}

	public void onClient(String message) {
		//System.out.println(session.getId() + ":" + message);
		String[] ope = message.split(" ");

		if(ope[0].equalsIgnoreCase("screen")) {
			int w = Integer.parseInt(ope[1]);
			int h = Integer.parseInt(ope[2]);
			String n = (ope.length > 3)? ope[3] : null;
			synchronized (this) {
				monitor = new Monitor(new Dimension(w, h), n);
			}
			try {
				if(session.isOpen())
					session.getBasicRemote().sendText("id " + session.getId());
			} catch (Exception e) {
				e.printStackTrace();
			}

			ws.browser_sendClients();
			return;
		}

		if(ope[0].equalsIgnoreCase("mouse")) {
			String t = String.join(" ", ope);
			ws.monitor_sendText(session.getId(), t);
			return;
		}
	}
}
