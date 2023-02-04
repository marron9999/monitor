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

public class Client implements WebSocket.Listener {
	private HttpClient client;
	private WebSocket ws;
	private Robot robot;
	private Monitor monitor;
	private String site;
	private String id = null;
	private int mx = -1, my = -1;

	public void run(String site, String pc) throws Exception {
		this.site = site;
		robot = new Robot();
		System.out.println("Server: " + site);
		URI uri = URI.create(site);
		monitor = new Monitor(Monitor.toolkit.getScreenSize(), null);
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
			Monitor.sleep(100);
		}

		String msg = "screen " + rect.width + " " + rect.height + " " + monitor.name();
		System.out.println(msg);
		ws.sendText(msg, true);
		
		while(id == null) {
			Monitor.sleep(100);
		}

		while(ws != null) {
			BufferedImage img = robot.createScreenCapture(rect);
			boolean need_sleep = monitor.diff(img);
			if( ! need_sleep) {
				try {
					post(monitor.buffer());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Point p = MouseInfo.getPointerInfo().getLocation();
			if(p.x != mx || p.y != mx) {
				mx = p.x;
				my = p.y;
				if(ws != null) {
					ws.sendText("mouse " + mx + " " + my, true);
				}
				need_sleep = false;
			}

			if(need_sleep) {
				Monitor.sleep(100);
			}
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
				int w = Integer.parseInt(ope[1].trim()); 
				if(w == 13) w = KeyEvent.VK_ENTER;
				if(ctrl) robot.keyPress(KeyEvent.VK_CONTROL);
				if(alt) robot.keyPress(KeyEvent.VK_ALT);
				if(shift) robot.keyPress(KeyEvent.VK_SHIFT);
				try {
					robot.keyPress(w);
					if( ! ctrl) {
						robot.keyRelease(w);
					}
				} catch (Exception e) {
					e.printStackTrace();
					rc = false;
				}
				if(shift) robot.keyRelease(KeyEvent.VK_SHIFT);
				if(alt) robot.keyRelease(KeyEvent.VK_ALT);
				if(ctrl) robot.keyRelease(KeyEvent.VK_CONTROL);
			} catch (Exception e) {
				e.printStackTrace();
				rc = false;
			}
			return rc;
		} 

		if(ope[0].trim().equalsIgnoreCase("keyup")) {
			boolean rc = true;
			try {
				int w = Integer.parseInt(ope[1].trim()); 
				try {
					if(ctrl)
						robot.keyRelease(w);
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
			if(mx >= 0 && my >= 0) {
				if(mx != x || my != y)
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
			String site = "ws://localhost:8080/monitor/ws";
			String pc = null;
			if(args.length > 0) {
				if(args.length > 1) {
					pc = args[1];
				}
				if(args[0].indexOf(":") > 0)
					site = "ws://" + args[0] + "/monitor/ws";
				else site = "ws://" + args[0] + ":8080/monitor/ws";
			}
			new Client().run(site, pc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
