import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.http.WebSocket;

public class Main extends MainBase {

	private String message;
	private String[] ope;
	private boolean ctrl; 
	@SuppressWarnings("unused")
	private boolean alt; 
	private boolean shift;
	protected int button;
	protected int key;

	private Boolean ope_string() {
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

			if(ope[1].equalsIgnoreCase("@iconcpu")) {
				message = "iconcpu " + verbose;
				try {
					frame.iconcpu = Boolean.parseBoolean(ope[2]);
					message = "iconcpu " + frame.iconcpu;
					if( ! frame.iconcpu) {
						frame.setIconImage(null);
					}
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
					ope[2] = message.substring(6).trim().replace(" ", "_");
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

			if(ope[1].equalsIgnoreCase("@reset")) {
				mouse.reset();
				if((button & 0x01) != 0) {
					System.out.println("reset BUTTON1_DOWN_MASK");
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				}
				if((button & 0x02) != 0) {
					System.out.println("reset BUTTON2_DOWN_MASK");
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
				}
				if((button & 0x04) != 0) {
					System.out.println("reset BUTTON3_DOWN_MASK");
					robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				}
				button = 0;
				if((key & 0x01) != 0) {
					System.out.println("reset VK_CONTROL");
					robot.keyRelease(KeyEvent.VK_CONTROL);
				}
				if((key & 0x02) != 0) {
					System.out.println("reset VK_SHIFT");
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
				if((key & 0x04) != 0) {
					System.out.println("reset VK_ALT");
					robot.keyRelease(KeyEvent.VK_ALT);
				}
				key = 0;
				BufferedImage img = monitor.new_image();
				monitor.image(img);
				upload_image(img);
				return true;
			}

			message = message.substring(ope[0].length()+1).trim();
			
			if(ope[1].charAt(0) == '@') {
				System.out.println("? " + message);
				return true;
			}

			paste(message.substring(ope[0].length()+1).trim());
			return true;
		}

		return null;
	}

	private Boolean ope_key() {
		if(ope[0].trim().equalsIgnoreCase("keydown")) {
			boolean rc = true;
			try {
				int _key = Integer.parseInt(ope[1].trim()); 
				int vkey = Monitor.keycode(_key, shift);
				if(vkey == KeyEvent.VK_CONTROL) key |= 0x01;
				else if(vkey == KeyEvent.VK_SHIFT) key |= 0x02;
				else if(vkey == KeyEvent.VK_ALT) key |= 0x04;
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
				if(vkey == KeyEvent.VK_CONTROL) key &= 0x00fe;
				else if(vkey == KeyEvent.VK_SHIFT) key &= 0x00fd;
				else if(vkey == KeyEvent.VK_ALT) key &= 0x00fb;
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
		
		return null;
	}
	
	private boolean mouse_xy() {
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

	private Boolean ope_mouse() {
		if(ope[0].trim().equalsIgnoreCase("dblclick")) {
			if(ope[3].equals("0")) { 
				if(mouse_xy()) {
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("1")) { 
				if(mouse_xy()) {
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("2")) {
				if(mouse_xy()) {
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
				if(mouse_xy()) {
					button |= 0x01;
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					//if( ! ctrl)
					//	robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("1")) { 
				if(mouse_xy()) {
					button |= 0x02;
					robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
					//if( ! ctrl)
					//	robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
					return true;
				}
			} else if(ope[3].equals("2")) {
				button |= 0x04;
				if(mouse_xy()) {
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
				mouse_xy();
				if(ope[3].equals("0")) { 
					button &= 0x00fe;
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				} else if(ope[3].equals("1")) { 
					button &= 0x00fd;
					robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
				} else if(ope[3].equals("2")) { 
					button &= 0x00fb;
					robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				}
			}
			return true;
		}

		if(ope[0].trim().equalsIgnoreCase("mousemove")) {
			if(mouse_xy()) {
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
				if(mouse_xy()) {
					robot.mouseWheel(w);
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		return null;
	}

	private Boolean ope_download() {
		if(ope[0].trim().equalsIgnoreCase("download")) {
			String iname = ope[1];
			String oname = ope[2];
			service.execute(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("download file");
					try {
						File file = http_get(iname, oname);
						if(file != null) {
							System.out.println("download: " + file.getName() + ", "
									+ String.format("%,d", file.length()) + " bytes");
						} else {
							System.out.println("Fail: " + oname);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					Thread.currentThread().setName("-");
				}
			});
			return true;
		}

		return null;
	}
	
	private Boolean ope_sysmon() {
		if(ope[0].trim().equalsIgnoreCase("sysmon")) {
			String rc = sysmon.curr_cpu();
			if(rc != null) {
				ws.sendText(rc, true);
			}
			rc = sysmon.curr_mem();
			if(rc != null) {
				ws.sendText(rc, true);
			}
			rc = sysmon.curr_drv();
			if(rc != null) {
				ws.sendText(rc, true);
			}
			return true;
		}

		return null;
	}

	@Override
	protected boolean onMessage(WebSocket webSocket, String message) {
		this.message = message;
		ope = message.split(" ");
		ctrl = (message.indexOf(" ctrl") >= 0)? true : false; 
		alt = (message.indexOf(" alt") >= 0)? true : false; 
		shift = (message.indexOf(" shift") >= 0)? true : false; 
		Boolean rc;
		if((rc = ope_string()) != null) return rc;
		if((rc = ope_key()) != null) return rc;
		if((rc = ope_mouse()) != null) return rc;
		if((rc = ope_sysmon()) != null) return rc;
		if((rc = ope_download()) != null) return rc;

		if(ope[0].trim().equalsIgnoreCase("id")) {
			System.out.println(message);
			id = ope[1].trim();
			frame.setTitle(id + ": " + monitor.name());
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

	public static void main(String[] args) {
		try {
			new Main().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
