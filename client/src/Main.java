import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main implements WebSocket.Listener {
	private HttpClient client;
	private WebSocket ws;
	private Robot robot;
	private Monitor monitor = null;
	private SysMon sysmon = null;
	private String site = "ws://192.168.1.127:8080/monitor/ws";
	private String id = null;
	private int mouse_x = -1, mouse_y = -1;
	private boolean verbose = false;
	private int sleep = 500;
	private Frame frame;
	private ExecutorService service = Executors.newCachedThreadPool();

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
		frame = new Frame() {
			private static final long serialVersionUID = 1L;
			@Override
			public void drop(File file) {
				upload(file);
			}
			@Override
			public void dispose() {
				frame = null;
				super.dispose();
				if(ws != null) {
					ws.abort();
					ws = null;
				}
			}

		};
		robot = new Robot();
		sysmon = new SysMon();
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
		frame.setTitle(monitor.name());
		System.setOut(new PrintStream(frame.stream));
		System.setErr(System.out);
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

		String message = "screen " + rect.width + " " + rect.height;
		System.out.println(message);
		ws.sendText(message, true);

		message = "name " + monitor.name();
		System.out.println(message);
		ws.sendText(message, true);

		message = "sleep " + sleep;
		System.out.println(message);

		message = "verbose " + verbose;
		System.out.println(message);

		while(id == null) {
			Monitor.sleep(500);
		}

		BufferedImage img = monitor.new_image();
		while(ws != null) {
			Graphics g = img.getGraphics();
			g.drawImage(robot.createScreenCapture(rect),
					0, 0, rect.width, rect.height,
					0, 0, rect.width, rect.height, null);
			g.dispose();
			BufferedImage diff = monitor.diff_image(img);
			if(diff != null) {
				upload(diff);
			}

			Point p = MouseInfo.getPointerInfo().getLocation();
			if(p.x != mouse_x || p.y != mouse_y) {
				mouse_x = p.x;
				mouse_y = p.y;
				if(ws != null) {
					ws.sendText("mouse " + mouse_x + " " + mouse_y, true);
				}
			}

			String sm = sysmon.diff_cpu();
			if(sm != null) {
				if(ws != null) {
					ws.sendText("sysmon " + sm, true);
				}
			}
			sm = sysmon.diff_mem();
			if(sm != null) {
				if(ws != null) {
					ws.sendText("sysmon " + sm, true);
				}
			}
			sm = sysmon.diff_drv();
			if(sm != null) {
				if(ws != null) {
					ws.sendText("sysmon " + sm, true);
				}
			}

			Monitor.sleep(sleep);
		}

		if(frame != null)
			frame.dispose();
		System.exit(0);
	}
	
	private void upload(File file) {
		service.execute(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("upload_file");
				try {
					String name = file.getAbsolutePath();
					name = name.replace(":", "").replace("\\", "_").replace("/", "_");
					name = monitor.name() + "_" + name;
					FileInputStream fis = new FileInputStream(file);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buf = new byte[4096];
					int len;
					while((len = fis.read(buf)) >= 0) {
						baos.write(buf, 0, len);
					}
					buf = baos.toByteArray();
					baos.close();
					fis.close();
					if(post(name, buf) == 200) {
						System.out.println("Upload: " + file.getAbsolutePath());
					} else {
						System.out.println("Fail: " + file.getAbsolutePath());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				Thread.currentThread().setName("-");
			}
		});
	}

	private void upload(BufferedImage image) {
		service.execute(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("upload_image");
				try {
					post(monitor.buffer(image));
				} catch (Exception e) {
					e.printStackTrace();
				}
				Thread.currentThread().setName("-");
			}
		});
	}
	
	private void download(String iname , String oname) {
		service.execute(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("download file");
				try {
					File file = get(iname, oname);
					if(file != null) {
						System.out.println("download: " + file.getName() + ", " + String.format("%,d", file.length()) + " bytes");
					} else {
						System.out.println("Fail: " + oname);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				Thread.currentThread().setName("-");
			}
		});
	}
	
	private int post(String file, byte[] buf) throws Exception {
		String arg = "file=" + URLEncoder.encode(file, "utf-8");
		return post(arg, "binary/octet-stream", buf);
	}

	private int post(byte[] buf) throws Exception {
		return post(null, "image/PNG", buf);
	}

	private int post(String arg, String type, byte[] buf) throws Exception {
		if(arg == null) arg = "";
		if(arg.length() > 0) arg = "&" + arg;
		String url = site.replace("ws:", "http:").replace("/ws", "/api");
		url = url + "?id=" + id + arg;
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", type);
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
		return conn.getResponseCode();
	}

	private File get(String ifile, String ofile) throws Exception {
		File dir = new File(System.getenv("USERPROFILE"), "Downloads");
		dir = new File(dir, ofile);
		String url = site.replace("ws:", "http:").replace("/ws", "/api");
		url = url + "?id=0&temp=" + ifile;
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.connect();
		FileOutputStream fos = new FileOutputStream(dir);
		InputStream is = conn.getInputStream();
		int len = 0;
		long tlen = 0;
		byte[] buf = new byte[4096];
		while((len = is.read(buf)) >= 0) {
			fos.write(buf, 0, len);
			tlen += len;
		}
		is.close();
		fos.close();
		conn.disconnect();
		if(tlen <= 0) 
			dir.delete();
		return dir;
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
		@SuppressWarnings("unused")
		boolean alt = (message.indexOf(" alt") >= 0)? true : false; 
		boolean shift = (message.indexOf(" shift") >= 0)? true : false; 

		if(ope[0].trim().equalsIgnoreCase("string")) {
			if(ope[1].equalsIgnoreCase("@verbose")) {
				message = "verbose " + verbose;
				try {
					verbose = Boolean.parseBoolean(ope[2]);
					message = "verbose " + verbose;
				} catch (Exception e) {
					//e.printStackTrace();
				}
				System.out.println(message);
				return true;
			}
			if(ope[1].equalsIgnoreCase("@sleep")) {
				message = "sleep " + sleep;
				try {
					sleep = Integer.parseInt(ope[2]);
					message = "sleep " + sleep;
				} catch (Exception e) {
					//e.printStackTrace();
				}
				System.out.println(message);
				return true;
			}
			if(ope[1].equalsIgnoreCase("@name")) {
				message = "name " + monitor.name();
				try {
					monitor.name(ope[2]);
					message = "name " + monitor.name();
					ws.sendText(message, true);
					frame.setTitle(id + ": " + monitor.name());
				} catch (Exception e) {
					//e.printStackTrace();
				}
				System.out.println(message);
				return true;
			}
			paste(message.substring(ope[0].length()+1).trim());
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("keydown")) {
			boolean rc = true;
			try {
				int _key = Integer.parseInt(ope[1].trim()); 
				int vkey = Monitor.keycode(_key, shift);
				try {
					if(vkey == KeyEvent.VK_UNDERSCORE) {
						paste("_");
					} else {
						robot.keyPress(vkey);
					}
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
					if(vkey == KeyEvent.VK_UNDERSCORE) {
						// NONE
					} else {
						robot.keyRelease(vkey);
					}
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
					//if( ! ctrl)
					//	robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("1")) { 
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					//if( ! ctrl)
					//	robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("2")) {
				if(mouse(ope)) {
					robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					//if( ! ctrl)
					//	robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					return true;
				}
			}
			return false;
		}
		
		if(ope[0].trim().equalsIgnoreCase("mouseup")) {
			//if(ctrl) 
			{
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
			if( ! verbose)
				System.out.println(message);
			id = ope[1].trim();
			frame.setTitle(id + ": " + monitor.name());
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("sysmon")) {
			String rc = sysmon.curr_cpu();
			if(rc != null) {
				ws.sendText("sysmon " + rc, true);
			}
			rc = sysmon.curr_mem();
			if(rc != null) {
				ws.sendText("sysmon " + rc, true);
			}
			rc = sysmon.curr_drv();
			if(rc != null) {
				ws.sendText("sysmon " + rc, true);
			}
			return true;
		}
		if(ope[0].trim().equalsIgnoreCase("cpu")) {
			String rc = sysmon.getCpu();
			if(rc != null) {
				ws.sendText("cpu " + rc, true);
			}
			return true;
		}
		if(ope[0].trim().equalsIgnoreCase("mem")) {
			String rc = sysmon.getMem();
			if(rc != null) {
				ws.sendText("mem " + rc, true);
			}
			return true;
		}
		if(ope[0].trim().equalsIgnoreCase("drv")) {
			String rc = sysmon.getDrv();
			if(rc != null) {
				ws.sendText("drv " + rc, true);
			}
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("download")) {
			download(ope[1], ope[2]);
			return true;
		}

		return false;
	}

	private void paste(String text) {
		String clip = Monitor.clipboard();
		Monitor.clipboard(text);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		Monitor.sleep(100);
		Monitor.clipboard(clip);
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
