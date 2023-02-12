import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
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
	private String cpu_usage = "";
	private String cpu_info = "";
	private String mem_usage = "";
	private String mem_info = "";
	private String drv_usage = "";
	private String drv_info = "";

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
		for(File file : APIBase.downloads.listFiles()) {
			if(file.isDirectory()) continue;
			String name = file.getName();
			if(name.toLowerCase().startsWith(host)) {
				name = name.substring(host.length()).replace(" ", "_");
				files.put(name, file);
			}
		}
		Set<String> set = files.keySet();
		names = set.toArray(new String[set.size()]);
		Arrays.sort(names, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				File f1 = files.get(o1);
				File f2 = files.get(o2);
				if(f1.lastModified() < f2.lastModified()) return 1;
				if(f1.lastModified() > f2.lastModified()) return -1;
				return o1.compareToIgnoreCase(o2);
			}
		});
	}

	public void delete_file(String name) {
		File file = files.get(name);
		if(file != null)
			file.delete();
	}

	public String info_file(String name) {
		File file = files.get(name);
		if(file != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:MM:ss");
			String val = name + "\n";
			val += "Size: " + String.format("%,d", file.length()) + " bytes\n";
			val += "Modified: " + sdf.format(new Date(file.lastModified())) ;
			return val;
		}
		return null;
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

	public String info_cpu() {
		return cpu_info;
	}
	public String info_mem() {
		return mem_info;
	}
	public String info_drv() {
		return drv_info;
	}
	public String usage_cpu() {
		return cpu_usage;
	}
	public String usage_mem() {
		return mem_usage;
	}
	public String usage_drv() {
		return drv_usage;
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

		if(ope[0].equalsIgnoreCase("cpu")) {
			int p = message.indexOf(" ", 5);
			cpu_info = message.substring(p + 1).trim();
			cpu_usage = ope[1];
			String t = "sysmon " + ope[0] + " " + ope[1];
			WS.monitor_sendText(session.getId(), t);
			return;
		}
		if(ope[0].equalsIgnoreCase("mem")) {
			int p = message.indexOf(" ", 5);
			mem_info = message.substring(p + 1).trim();
			mem_usage = ope[1];
			String t = "sysmon " + ope[0] + " " + ope[1];
			WS.monitor_sendText(session.getId(), t);
			return;
		}
		if(ope[0].equalsIgnoreCase("drv")) {
			int p = message.indexOf(" ", 5);
			drv_info = message.substring(p + 1).trim();
			drv_usage = ope[1];
			String t = "sysmon " + ope[0] + " " + ope[1];
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
