import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
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

public abstract class Base {
	protected HttpClient client;
	protected WebSocket ws;
	protected Robot robot;
	protected Monitor monitor = null;
	protected SysMon sysmon = null;
	protected String site = "ws://192.168.1.127:8080/monitor/ws";
	protected String id = null;
	protected int mouse_x = -1, mouse_y = -1;
	protected boolean verbose = false;
	protected int sleep = 500;
	protected Frame frame;
	protected ExecutorService service = Executors.newCachedThreadPool();

	protected abstract boolean onMessage(WebSocket webSocket, String message);

	private boolean parse_args(String opt) throws Exception {
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
	
	private void sysmon() {
		String sm = sysmon.diff_cpu();
		if(sm != null) {
			frame.setCPU(sm);
			if(ws != null) {
				ws.sendText("sysmon " + sm, true);
			}
		}
		sm = sysmon.diff_mem();
		if(sm != null) {
			frame.setMEM(sm);
			if(ws != null) {
				ws.sendText("sysmon " + sm, true);
			}
		}
		sm = sysmon.diff_drv();
		if(sm != null) {
			frame.setDRV(sm);
			if(ws != null) {
				ws.sendText("sysmon " + sm, true);
			}
		}
	}

	public void run(String[] args) throws Exception {
		frame = new Frame() {
			protected static final long serialVersionUID = 1L;
			@Override
			public void drop_file(File file) {
				upload_file(file);
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
			if( ! parse_args(args[i])) break;
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
				CompletableFuture<WebSocket> comp = wsb.buildAsync(uri, new WebSocket.Listener() {
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
				});
				ws = comp.get();
			} catch (Exception e) {
				//e.printStackTrace();
			}

			sysmon();

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
			sysmon();

			Monitor.sleep(sleep);
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
				upload_image(diff);
			}

			Point p = MouseInfo.getPointerInfo().getLocation();
			if(p.x != mouse_x || p.y != mouse_y) {
				mouse_x = p.x;
				mouse_y = p.y;
				if(ws != null) {
					ws.sendText("mouse " + mouse_x + " " + mouse_y, true);
				}
			}

			sysmon();

			Monitor.sleep(sleep);
		}

		if(frame != null)
			frame.dispose();
		System.exit(0);
	}
	
	protected void upload_file(File file) {
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
					if(http_post_bytes(name, buf) == 200) {
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

	protected void upload_image(BufferedImage image) {
		service.execute(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("upload_image");
				try {
					http_post_image(monitor.buffer(image));
				} catch (Exception e) {
					e.printStackTrace();
				}
				Thread.currentThread().setName("-");
			}
		});
	}
	
	protected int http_post_bytes(String file, byte[] buf) throws Exception {
		String arg = "file=" + URLEncoder.encode(file, "utf-8");
		return http_post(arg, "binary/octet-stream", buf);
	}

	protected int http_post_image(byte[] buf) throws Exception {
		return http_post(null, "image/PNG", buf);
	}

	protected int http_post(String arg, String type, byte[] buf) throws Exception {
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

	protected File http_get(String ifile, String ofile) throws Exception {
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

//	protected void close() {
//		try {
//			CompletableFuture<WebSocket> end = ws.sendClose(WebSocket.NORMAL_CLOSURE, "Bye");
//			end.get();
//		} catch (Exception e) {
//			// NONE
//		}
//	}
}
