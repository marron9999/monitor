import java.awt.Dimension;
import java.awt.image.BufferedImage;
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
	private static HashMap<String/*Session id*/, Session> clients = new HashMap<>();
	private static HashMap<String/*Session id*/, Monitor> monitors = new HashMap<>();
	private static HashMap<String/*Session id*/, Session> browsers = new HashMap<>();
	private static HashMap<String/*Session id*/, String/*Session id*/> view = new HashMap<>();

	public static BufferedImage image(String id) {
		Monitor monitor = monitors.get(id);
		if(monitor != null) {
			BufferedImage img = monitor.image();
			return img;
		}
		return null;
	}
	public static void image(String id, BufferedImage img) {
		Monitor monitor = monitors.get(id);
		if(monitor != null) {
			monitor.image(img);
	    	for(String key : browsers.keySet()) {
	    		String vid = view.get(key);
	    		if(vid != null
	    		&& vid.equalsIgnoreCase(id)) {
	    			try {
	    				Session browser = browsers.get(key);
	    				if(browser.isOpen())
	    					browser.getBasicRemote().sendText("redraw");
	    	    	} catch (Exception e) {
	    	    		e.printStackTrace();
	    	    	}
	    		}
	    	}
    		Monitor.sleep(100);
		}
	}
	
	@OnOpen
    public void onOpen(Session session) {
		// NONE
	}

    @OnMessage
    public void onMessage(String message, Session session) {
    	int p = message.indexOf(" ");
    	String ope = message;
    	if(p >= 0) ope = message.substring(0, p).trim();
    	if(ope.equalsIgnoreCase("screen")) {
			onClient(message, session);
			return;
    	}

    	if(ope.equalsIgnoreCase("browser")) {
    		onBrowser(message, session);
			return;
    	}

    	boolean sw = false;
		synchronized (monitors) {
	    	if(clients.containsKey(session.getId())) sw = true;
		}
	    if(sw) {
			onClient(message, session);
			return;
	    }

	    sw = false;
		synchronized (browsers) {
	    	if(browsers.containsKey(session.getId())) sw = true;
		}
		if(sw) {
			onBrowser(message, session);
			return;
		}
    }
    
    private void onBrowser(String message, Session session) {
    	String[] ope = message.split(" ");
    	if(ope[0].equalsIgnoreCase("string")
    	|| ope[0].equalsIgnoreCase("mousedown")
    	|| ope[0].equalsIgnoreCase("mouseup")
    	|| ope[0].equalsIgnoreCase("mousemove")
    	|| ope[0].equalsIgnoreCase("mousewheel")
    	|| ope[0].equalsIgnoreCase("keydown")
    	|| ope[0].equalsIgnoreCase("keyup")
    	) {
    		Session client = null;
    		synchronized (monitors) {
    			client = clients.get(view.get(session.getId()));
    		}
			try {
	    		if(client != null
   	    		&& client.isOpen())
	    			client.getBasicRemote().sendText(message, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
    		return;
    	}

    	if(ope[0].equalsIgnoreCase("browser")) {
    		Monitor monitor = null;
    		synchronized (monitors) {
    			if(ope.length == 1) {
            		synchronized (browsers) {
	            		browsers.put(session.getId(), session);
	            		clients();
            		}
            		return;
    			}
        		monitor = monitors.get(ope[1]);
        		if(monitor == null) return;
			}

    		try {
    			Dimension size = monitor.size();
        		session.getBasicRemote().sendText("screen " + size.width + " " + size.height);
        		synchronized (browsers) {
            		view.put(session.getId(), ope[1]); 
            		browsers.put(session.getId(), session);
        		}
			} catch (Exception e) {
				// NoNE
			}
    		return;
    	}
    }
    
    private void onClient(String message, Session session) {
    	//System.out.println(session.getId() + ":" + message);
    	String[] ope = message.split(" ");
    	if(ope[0].equalsIgnoreCase("screen")) {
    		int w = Integer.parseInt(ope[1]);
    		int h = Integer.parseInt(ope[2]);
    		String n = (ope.length > 3)? ope[3] : null;
    		synchronized (monitors) {
        		clients.put(session.getId(), session);
        		Monitor monitor = new Monitor(new Dimension(w, h), n);
        		monitors.put(session.getId(), monitor);
    		}
    		try {
    			if(session.isOpen())
    				session.getBasicRemote().sendText("id " + session.getId());
			} catch (Exception e) {
				e.printStackTrace();
			}

    		clients();
    		return;
    	}
    	if(ope[0].equalsIgnoreCase("mouse")) {
    		String t = String.join(" ", ope);
    		synchronized (browsers) {
	        	for(String key : browsers.keySet()) {
	    			try {
	            		Session browser = browsers.get(key);
	            		if(browser.isOpen())
	            			browser.getBasicRemote().sendText(t);
					} catch (Exception e) {
						e.printStackTrace();
					}
	        	}
    		}
    		return;
    	}
    }

    private void clients() {
		synchronized (browsers) {
			Session session;
        	Set<String> keyset = clients.keySet();
        	String[] clientKeys = keyset.toArray(new String[keyset.size()]);
        	keyset = browsers.keySet();
        	for(String key : keyset) {
    			try {
    				session = browsers.get(key);
            		if(session.isOpen())
            			session.getBasicRemote().sendText("clients [");
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	for(String ckey : clientKeys) {
        		session = clients.get(ckey);
        		Monitor monitor = monitors.get(ckey);
       			String t = ckey + ": " + monitor.name();
	        	for(String key : keyset) {
	    			try {
	    				session = browsers.get(key);
	            		if(session.isOpen())
	            			session.getBasicRemote().sendText("clients " + t);
					} catch (Exception e) {
						e.printStackTrace();
					}
	        	}
        	}
        	for(String key : keyset) {
    			try {
            		session = browsers.get(key);
            		if(session.isOpen())
            			session.getBasicRemote().sendText("clients ]");
				} catch (Exception e) {
					e.printStackTrace();
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
		synchronized (monitors) {
			if(clients.containsKey(id)) {
        		monitors.remove(id);
        		clients.remove(id);
        		clients();
        		return;
			}
    	}
		synchronized (browsers) {
	    	if(browsers.containsKey(id)) {
		    	browsers.remove(id);
		    	view.remove(id);
	    		return;
	    	}
		}
    }
}
