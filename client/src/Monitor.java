import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
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
		pre = new BufferedImage(size.width,  size.height,  BufferedImage.TYPE_INT_ARGB);
    	Graphics g = pre.getGraphics();
    	g.setColor(Color.WHITE);
    	g.fillRect(0, 0, size.width, size.height);
   		g.dispose();
	}

	public Dimension size() {
		return size;
	}

	public String name() {
		return name;
	}

	public boolean diff(BufferedImage img) {
		int w = size.width;
		int h = size.height;
		for(int yi = 0; yi < h; yi++) {
			for(int xi = 0; xi < w; xi++) {
				if(pre.getRGB(xi, yi) != img.getRGB(xi, yi)) {
					synchronized (pre) {
				    	Graphics g = pre.getGraphics();
				    	g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);
				   		g.dispose();
					}
					return false;
				}
			}
		}
		return true;
	}

	public byte[] buffer() {
		synchronized (pre) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(pre, "PNG", baos);
				byte[] buf = baos.toByteArray();
				baos.close();
				return buf;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new byte[0];
	}

	public BufferedImage image() {
		BufferedImage ii = null;
		synchronized (pre) {
			ii = pre;
		}
		return ii;
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
}
