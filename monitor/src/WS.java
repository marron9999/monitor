import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Set;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws")
public class WS {
	private static HashMap<String/*Session id*/, Client> clients = new HashMap<>();
	private static HashMap<String/*Session id*/, Browser> browsers = new HashMap<>();

	public static BufferedImage client_image(String id) {
		Client client = clients.get(id);
		if(client != null) {
			return client.image();
		}
		return null;
	}
	public static void client_update_files(String id) {
		Client client = clients.get(id);
		if(client != null) {
			client.update_files();
			browser_sendClients();
		}
	}
	public static File client_file(String id, String name) {
		Client client = clients.get(id);
		if(client != null) {
			return client.file(name);
		}
		return null;
	}
	public static void client_image(String id, BufferedImage img) {
		Client client = clients.get(id);
		if(client != null) {
			client.image(img);
			monitor_sendText(id, "redraw");
		}
	}

	public static void client_sendText(String id, String message) {
		if(id == null) return;
		Client client = null;
		synchronized (clients) {
			client = clients.get(id);
		}
		if(client != null)
			client.sendText(message);
	}
	public static Dimension client_size(String id) {
		if(id == null) return null;
		Client client = null;
		synchronized (clients) {
			client = clients.get(id);
		}
		if(client != null)
			return client.size();
		return null; 
	}

	public static void monitor_sendText(String id, String message) {
		if(id == null) return;
		Browser browser = null;
		Set<String> keyset = browsers.keySet();
		for(String key : keyset) {
			synchronized (browsers) {
				browser = browsers.get(key);
			}
			if(browser != null) {
				String view = browser.monitorId();
				if(view != null
				&& view.equalsIgnoreCase(id))
					browser.sendText(message);
			}
		}
	}
	public static void browser_sendText(String id, String message) {
		if(id == null) return;
		Browser browser = null;
		synchronized (browsers) {
			browser = browsers.get(id);
		}
		if(browser != null)
			browser.sendText(message);
	}
	public static String browser_monitorId(String id) {
		if(id == null) return null;
		Browser browser = null;
		synchronized (browsers) {
			browser = browsers.get(id);
		}
		if(browser != null)
			return browser.monitorId();
		return null;
	}

	@OnOpen
	public void onOpen(Session session) {
		synchronized (browsers) {
			
		}
		// NONE
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		int p = message.indexOf(" ");
		String ope = message;
		if(p >= 0) ope = message.substring(0, p).trim();
		Client client = null;
		Browser browser = null;

		if(ope.equalsIgnoreCase("screen")) {
			synchronized (clients) {
				client = clients.get(session.getId());
				if(client == null) {
					client = new Client(this, session);
					clients.put(session.getId(), client);
				}
			}
			client.onClient(message);
			return;
		}

		if(ope.equalsIgnoreCase("browser")) {
			synchronized (browsers) {
				browser = browsers.get(session.getId());
				if(browser == null) {
					browser = new Browser(this, session);
					browsers.put(session.getId(), browser);
				}
			}
			browser.onBrowser(message);
			return;
		}

		synchronized (clients) {
			client = clients.get(session.getId());
		}
		if(client != null) {
			client.onClient(message);
			return;
		}

		synchronized (browsers) {
			browser = browsers.get(session.getId());
		}
		if(browser != null) {
			browser.onBrowser(message);
			return;
		}
	}

	public static void browser_sendClients() {
		synchronized (browsers) {
			synchronized (clients) {
				Client client = null;
				Browser browser = null;
				Set<String> keyset = browsers.keySet();
				Set<String> ckeyset = clients.keySet();
				for(String key : keyset) {
					browser = browsers.get(key);
					browser.sendText("clients [");
				}
				for(String ckey : ckeyset) {
					client = clients.get(ckey);
					String t = ckey + ": " + client.name();
					for(String key : keyset) {
						browser = browsers.get(key);
						browser.sendText("clients " + t);
					}
				}
				for(String key : keyset) {
					browser = browsers.get(key);
					browser .sendText("clients ]");
				}
				for(String ckey : ckeyset) {
					client = clients.get(ckey);
					String[] names = client.files();
					if(names.length > 0) {
						for(String key : keyset) {
							browser = browsers.get(key);
							browser.sendText("files " + ckey + " [");
						}
						for(String name : names) {
							for(String key : keyset) {
								browser = browsers.get(key);
								browser.sendText("files " + ckey + " " + name);
							}
						}
						for(String key : keyset) {
							browser = browsers.get(key);
							browser.sendText("files " + ckey + " ]");
						}
					}
				}
			}
		}
	}

	@OnError
	public void onError(Session session, Throwable cause) {
		// NONE
	}

	@OnClose
	public void onClose(Session session) {
		String id = session.getId();
		remove(id);
	}

	private void remove(String id) {
		synchronized (clients) {
			if(clients.containsKey(id)) {
				clients.remove(id);
				browser_sendClients();
				return;
			}
		}
		synchronized (browsers) {
			if(browsers.containsKey(id)) {
				browsers.remove(id);
				return;
			}
		}
	}
}
