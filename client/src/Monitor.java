import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public class Monitor {

	public static Toolkit toolkit = Toolkit.getDefaultToolkit();
	
	public static void sleep(int msec) {
		try {
			Thread.sleep(100);
		} catch (Exception e) {
			// NONE
		}
	}

	public static String clipboard() {
		Clipboard clip = toolkit.getSystemClipboard();
		try {
			return (String) clip.getData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			// NONE
		}
		return "";
	}
	public static void clipboard(String str) {
		Clipboard clip = toolkit.getSystemClipboard();
		StringSelection ss = new StringSelection(str);
		clip.setContents(ss, ss);
	}
	
	private BufferedImage pre = null;
	private Dimension size;
	private String name = System.getenv("COMPUTERNAME");

	public Monitor(Dimension size, String name) {
		this.size = size;
		if(name != null)
			this.name = name;
		pre = new_image();
	}

	public BufferedImage new_image() {
		BufferedImage img = new BufferedImage(
				size.width,  size.height,
				BufferedImage.TYPE_INT_ARGB);
    	Graphics g = img.getGraphics();
    	g.setColor(Color.WHITE);
    	g.fillRect(0, 0, size.width, size.height);
   		g.dispose();
   		return img;
	}

	public Dimension size() {
		return size;
	}

	public String name() {
		return name;
	}
	public void name(String name) {
		this.name = name;
	}

	public BufferedImage diff_image(BufferedImage img) {
		int w = size.width;
		int h = size.height;
		boolean same = true;
		synchronized (pre) {
			for(int yi = 0; yi < h; yi++) {
				for(int xi = 0; xi < w; xi++) {
					int c = img.getRGB(xi, yi);
					if(pre.getRGB(xi, yi) != c) {
						pre.setRGB(xi, yi, c);
						same = false;
					} else {
						img.setRGB(xi, yi, 0);
					}
				}
			}
		}
		if( ! same) {
			return img;
		}
		return null;
	}

	public byte[] buffer() {
		synchronized (pre) {
			return buffer(pre);
		}
	}
	public byte[] buffer(BufferedImage img) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "PNG", baos);
			byte[] buf = baos.toByteArray();
			baos.close();
			return buf;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	public BufferedImage image() {
		return pre;
	}

	public void image(BufferedImage img) {
		synchronized (pre) {
			int w = img.getWidth();
			int h = img.getHeight();
	    	Graphics g = pre.getGraphics();
	    	g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);
	   		g.dispose();
		}
	}

	public static int keycode(int code, boolean shift) {
		if(code == 13) return KeyEvent.VK_ENTER;
		if(code == 186) return KeyEvent.VK_COLON;
		if(code == 187) return KeyEvent.VK_SEMICOLON;
		if(code == 188) return KeyEvent.VK_COMMA;
		if(code == 189) return KeyEvent.VK_MINUS;
		if(code == 190) return KeyEvent.VK_PERIOD;
		if(code == 191) return KeyEvent.VK_SLASH;
		if(code == 192) return KeyEvent.VK_AT;
		if(code == 219) return KeyEvent.VK_OPEN_BRACKET;
		if(code == 220) return KeyEvent.VK_BACK_SLASH;
		if(code == 221) return KeyEvent.VK_CLOSE_BRACKET;
		if(code == 222) return KeyEvent.VK_CIRCUMFLEX;
		if(code == 226) {
			if(shift) return KeyEvent.VK_UNDERSCORE;
			return KeyEvent.VK_BACK_SLASH;
		}
		if(code == 44) return KeyEvent.VK_PRINTSCREEN;
		if(code == 45) return KeyEvent.VK_INSERT;
		if(code == 46) return KeyEvent.VK_DELETE;
		//if(code == 36) return KeyEvent.VK_HOME;
		//if(code == 35) return KeyEvent.VK_END;
		//if(code == 33) return KeyEvent.VK_PAGE_UP;
		//if(code == 34) return KeyEvent.VK_PAGE_DOWN;
		if(code == 91) return KeyEvent.VK_WINDOWS;
		//if(code == 145) return KeyEvent.VK_SCROLL_LOCK;
		//if(code == 144) return KeyEvent.VK_NUM_LOCK;
		//if(code == 20) return KeyEvent.VK_CAPS_LOCK;
		//if(code == 8) return KeyEvent.VK_BACK_SPACE;
		//if(code == 32) return KeyEvent.VK_SPACE;
		//if(code == 16) return KeyEvent.VK_SHIFT;
		//if(code == 17) return KeyEvent.VK_CONTROL;
		//if(code == 18) return KeyEvent.VK_ALT;
		//if(code == 112) return KeyEvent.VK_F1;
		//if(code == 27) return KeyEvent.VK_ESCAPE;
		//if(code == 9) return KeyEvent.VK_TAB;
		return code;
	}
}
