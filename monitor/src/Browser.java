import java.awt.Dimension;

import jakarta.websocket.Session;

public class Browser {
	@SuppressWarnings("unused")
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
			WS.client_sendText(id, message);
			return;
		}

		if(ope[0].equalsIgnoreCase("sysmon")) {
			String[] val = WS.client_sysmon(monitor_id);
			sendText("sysmon cpu " + val[0]);
			sendText("sysmon mem " + val[1]);
			sendText("sysmon drv " + val[2]);
			return;
		}
		if(ope[0].equalsIgnoreCase("cpu")) {
			String val = WS.client_cpu(monitor_id);
			sendText("cpu " + val);
			return;
		}
		if(ope[0].equalsIgnoreCase("mem")) {
			String val = WS.client_mem(monitor_id);
			sendText("mem " + val);
			return;
		}
		if(ope[0].equalsIgnoreCase("drv")) {
			String val = WS.client_drv(monitor_id);
			sendText("drv " + val);
			return;
		}

		if(ope[0].equalsIgnoreCase("browser")) {
			synchronized (this) {
				WS.browser_sendClients();
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
				Dimension size = WS.client_size(ope[1]);
				String[] mouse = WS.client_mouse(ope[1]);
				if(size != null) {
					sendText("screen " + size.width + " " + size.height);
					if(mouse[0].length() > 0)
						sendText("mouse " + mouse[0] + " " + mouse[1]);
					if(mouse[2].length() > 0)
						sendText("cursor " + mouse[2] + " " + mouse[3] + " " + mouse[4]);
					synchronized (this) {
						monitor_id = ope[1]; 
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		if(ope[0].equalsIgnoreCase("delete")) {
			WS.client_delete(ope[1], ope[2]);
			return;
		}

		if(ope[0].equalsIgnoreCase("fileinfo")) {
			String val = WS.client_fileinfo(ope[1], ope[2]);
			sendText("fileinfo " + val);
			return;
		}
	}
}
