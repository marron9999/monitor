import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Main implements WebSocket.Listener {
	private HttpClient client;
	private WebSocket ws;
	private Robot robot;
	private Monitor monitor = null;
	private String site = "ws://192.168.1.127:8080/monitor/ws";
	private String id = null;
	private int mouse_x = -1, mouse_y = -1;
	private boolean verbose = false;
	private int sleep = 500;

	public boolean parse(String opt) throws Exception {
		opt = opt.replace("/", "-").toLowerCase();
		if(opt.equalsIgnoreCase("-v")
		|| opt.equalsIgnoreCase("--verbose")) {
			verbose = true;
			return true;
		}
		if(opt.startsWith("-s")) {
			try {
				sleep = Integer.parseInt(opt.substring(2));
			} catch (Exception e) {
				// NONE
			}
			return true;
		}
		if(opt.equalsIgnoreCase("--sleep")) {
			try {
				sleep = Integer.parseInt(opt.substring(7));
			} catch (Exception e) {
				// NONE
			}
			return true;
		}
		
		return false;
	}

	public void run(String[] args) throws Exception {
		robot = new Robot();
		monitor = new Monitor(Monitor.toolkit.getScreenSize(), null);
		int i=0;
		while(i < args.length) {
			if( ! parse(args[i])) break;
			i++;
		}
		if(i < args.length) {
			if(args[i].indexOf(":") > 0)
				site = "ws://" + args[i] + "/monitor/ws";
			else site = "ws://" + args[i] + ":8080/monitor/ws";
			i++;
			if(i < args.length) {
				monitor = new Monitor(Monitor.toolkit.getScreenSize(), args[i]);
			}
		}
		if(monitor == null)
			monitor = new Monitor(Monitor.toolkit.getScreenSize(), null);
		System.out.println("Server: " + site);
		URI uri = URI.create(site);
		Rectangle rect = new Rectangle(monitor.size());		
		client = HttpClient.newHttpClient();
		WebSocket.Builder wsb = client.newWebSocketBuilder();
		while(ws == null) {
			try {
				CompletableFuture<WebSocket> comp = wsb.buildAsync(uri, this);
				ws = comp.get();
			} catch (Exception e) {
				//e.printStackTrace();
			}
			Monitor.sleep(500);
		}

		int w = rect.width;
		int h = rect.height;
		String msg = "screen " + w + " " + h + " " + monitor.name();
		System.out.println(msg);
		ws.sendText(msg, true);
		
		while(id == null) {
			Monitor.sleep(500);
		}

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		while(ws != null) {
	    	Graphics g = img.getGraphics();
	    	g.drawImage(robot.createScreenCapture(rect), 0, 0, w, h, 0, 0, w, h, null);
	   		g.dispose();
	   		BufferedImage diff = monitor.diff(img);
			if(diff != null) {
				try {
					post(monitor.buffer(diff));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Point p = MouseInfo.getPointerInfo().getLocation();
			if(p.x != mouse_x || p.y != mouse_x) {
				mouse_x = p.x;
				mouse_y = p.y;
				if(ws != null) {
					ws.sendText("mouse " + mouse_x + " " + mouse_y, true);
				}
			}

			Monitor.sleep(sleep);
		}
	}
		
	private void post(byte[] buf) throws Exception {
		String s = site.replace("ws:", "http:").replace("/ws", "/api");
        URL url = new URL(s + "?id=" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "image/PNG");
        conn.setRequestProperty("Content-Length", Integer.toString(buf.length));
        conn.connect();
        OutputStream os = conn.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(buf);
		dos.close();
		os.close();
		InputStream is = conn.getInputStream();
        is.read();
        is.close();
        conn.disconnect();
	}
	
//	private void close() {
//		try {
//			CompletableFuture<WebSocket> end = ws.sendClose(WebSocket.NORMAL_CLOSURE, "Bye");
//			end.get();
//		} catch (Exception e) {
//			// NONE
//		}
//	}
	
	private String text = ""; 
	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		text += data.toString();
		if(last) {
			String message = text;
			text = "";
			boolean rc = onMessage(webSocket, message);
			if(verbose)
				System.out.println(rc + ": " + message);
		}
		return Listener.super.onText(webSocket, data, last);
	}

	public boolean onMessage(WebSocket webSocket, String message) {
		String[] ope = message.split(" ");
		boolean ctrl = (message.indexOf(" ctrl") >= 0)? true : false; 
		boolean alt = (message.indexOf(" alt") >= 0)? true : false; 
		boolean shift = (message.indexOf(" shift") >= 0)? true : false; 

		if(ope[0].trim().equalsIgnoreCase("string")) {
			String s = message;
			String clip = Monitor.clipboard();
			Monitor.clipboard(s.substring(ope[0].length()+1).trim());
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_V);
			robot.keyRelease(KeyEvent.VK_V);
			robot.keyRelease(KeyEvent.VK_CONTROL);
			Monitor.sleep(500);
			Monitor.clipboard(clip);
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("keydown")) {
			boolean rc = true;
			try {
				int _key = Integer.parseInt(ope[1].trim()); 
				int vkey = Monitor.keycode(_key, shift);
				try {
					if(shift && Monitor.noshift(_key))
						robot.keyRelease(KeyEvent.VK_SHIFT);
					robot.keyPress(vkey);
				} catch (Exception e) {
					e.printStackTrace();
					rc = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				rc = false;
			}
			return rc;
		} 

		if(ope[0].trim().equalsIgnoreCase("keyup")) {
			boolean rc = true;
			try {
				int _key = Integer.parseInt(ope[1].trim()); 
				int vkey = Monitor.keycode(_key, shift);
				try {
					robot.keyRelease(vkey);
				} catch (Exception e) {
					e.printStackTrace();
					rc = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				rc = false;
			}
			return rc;
		}

		if(ope[0].trim().equalsIgnoreCase("dblclick")) {
			if(ope[3].equals("0")) { 
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("1")) { 
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("2")) {
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					return true;
				}
			}
			return false;
		}

		if(ope[0].trim().equalsIgnoreCase("mousedown")) {
			if(ope[3].equals("0")) { 
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					if( ! ctrl)
						robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("1")) { 
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					if( ! ctrl)
						robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("2")) {
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					if( ! ctrl)
						robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					return true;
				}
			}
			return false;
		}
		
		if(ope[0].trim().equalsIgnoreCase("mouseup")) {
			if(ctrl) {
				mouse(ope);
				if(ope[3].equals("0")) { 
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				} else if(ope[3].equals("1")) { 
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
				} else if(ope[3].equals("2")) { 
					robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				}
			}
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("mousemove")) {
			if(mouse(ope)) {
				return true;
			}
			return false;
		}

		if(ope[0].trim().equalsIgnoreCase("mousewheel")) {
			try {
				int w = Integer.parseInt(ope[3].trim()); 
				if(w > 0) w = 2; else w = -2;
				if(shift) w *= 2;
				if(ctrl) w *= 2;
				if(mouse(ope)) {
					robot.mouseWheel(w);
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		if(ope[0].trim().equalsIgnoreCase("id")) {
			System.out.println(message);
			id = ope[1].trim();
			return true;
		}
		
		return false;
	}

	private boolean mouse(String[] ope) {
		try {
			//System.out.println(String.join(" ", ope));
			int x = Integer.parseInt(ope[1].trim()); 
			int y = Integer.parseInt(ope[2].trim());
			if(mouse_x >= 0 && mouse_y >= 0) {
				if(mouse_x != x || mouse_y != y)
					robot.mouseMove(x, y);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		System.out.println("onOpen: Connect to server");
		Listener.super.onOpen(webSocket);
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println("onClose: Disconnect from server");
		ws.abort();
		ws = null;
		return Listener.super.onClose(webSocket, statusCode, reason);
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		System.out.println("onError: " + error.toString());
		ws = null;
		Listener.super.onError(webSocket, error);
	}

	public static void main(String[] args) {
		try {
			new Main().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
