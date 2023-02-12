import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.websocket.Session;

public class Client {
	@SuppressWarnings("unused")
	private WS ws;
	private Session session;
	private Monitor monitor;
	private Map<String, File> files = null;
	private String[] names = null;

	public void sendText(String text) {
		try {
			if(session.isOpen())
				session.getBasicRemote().sendText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update_files() {
		files = new HashMap<>();
		String host = monitor.name().toLowerCase() + "_";
		for(File file : API.downloads.listFiles()) {
			if(file.isDirectory()) continue;
			String name = file.getName();
			if(name.toLowerCase().startsWith(host)) {
				name = name.substring(host.length()).replace(" ", "_");
				files.put(name, file);
			}
		}
		Set<String> set = files.keySet();
		names = set.toArray(new String[set.size()]);
		Arrays.sort(names);
	}

	public File file(String name) {
		return files.get(name);
	}

	public String[] files() {
		if(names == null)
			update_files();
		return names;
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
			String n = (ope.length > 3)? ope[3] : "" + session.getId();
			synchronized (this) {
				monitor = new Monitor(new Dimension(w, h), n);
			}
			sendText("id " + session.getId());
			WS.browser_sendClients();
			return;
		}

		if(ope[0].equalsIgnoreCase("mouse")) {
			String t = String.join(" ", ope);
			WS.monitor_sendText(session.getId(), t);
			return;
		}

		if(ope[0].equalsIgnoreCase("sysmon")) {
			String t = String.join(" ", ope);
			WS.monitor_sendText(session.getId(), t);
			return;
		}
		if(ope[0].equalsIgnoreCase("cpu")) {
			String t = String.join(" ", ope);
			WS.monitor_sendText(session.getId(), t);
			return;
		}
		if(ope[0].equalsIgnoreCase("mem")) {
			String t = String.join(" ", ope);
			WS.monitor_sendText(session.getId(), t);
			return;
		}
		if(ope[0].equalsIgnoreCase("drv")) {
			String t = String.join(" ", ope);
			WS.monitor_sendText(session.getId(), t);
			return;
		}

		if(ope[0].equalsIgnoreCase("name")) {
			synchronized (this) {
				monitor.name(ope[1]);
			}
			update_files();
			WS.browser_sendClients();
			return;
		}
	}
}
