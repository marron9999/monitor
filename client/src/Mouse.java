import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;

public class Mouse {
	static {
		System.setProperty("jna.platform.library.path", ".");
	}

	private static List<Long> mouses = new ArrayList<>();
	private static List<int[]> mouseh = new ArrayList<>();
	
	public boolean isAdd(int ix) {
		return false;
	}

	int cursor_x;
	int cursor_y;
	int cursor_c;
	long cursor_h;
	int hotspot_x;
	int hotspot_y;
	
	public void getCursor() {
		cursor_c = -1;
		getCursorInfo();
		if(cursor_h > 0) {
			int i = mouses.indexOf(cursor_h);
			if(i < 0) {
				System.out.println("");
				i = mouses.size();
				if(isAdd(i)) {
					cursor_c = i;
					mouses.add(cursor_h);
					mouseh.add(new int[] { hotspot_x, hotspot_y });
				}
			} else {
				int[] h = mouseh.get(i);
				cursor_c = i;
				hotspot_x = h[0];
				hotspot_y = h[1];
			}
		}
	}
	
	private void getCursorInfo() {
		CURSORINFO cursorinfo = new CURSORINFO();
		User32x.INSTANCE.GetCursorInfo(cursorinfo);
		cursor_x = cursorinfo.ptPos.x;
		cursor_y = cursorinfo.ptPos.y;
		cursor_h = 0;
		if(cursorinfo.hCursor != null) {
			cursor_h = cursorinfo.hCursor.getPointer().hashCode();
		}
	}

	public BufferedImage getCursorImage() {
		WinDef.HICON h = new WinDef.HICON(new Pointer(cursor_h));
		return getImageByHICON(32, 32, h);
	}

	public static class CURSORINFO extends Structure {
		public int cbSize;
		public int flags;
		public WinDef.HCURSOR hCursor;
		public WinDef.POINT ptPos;
		public CURSORINFO() {
			this.cbSize = Native.getNativeSize(CURSORINFO.class, null);
		}
		@Override
		public List<String> getFieldOrder() {
			return Arrays.asList("cbSize", "flags", "hCursor", "ptPos");
		}
	}

	public interface User32x extends com.sun.jna.Library {
		@SuppressWarnings("deprecation")
		User32x INSTANCE = Native.loadLibrary("User32.dll", User32x.class);
		int GetCursorInfo(CURSORINFO cursorinfo);
	}

	private BufferedImage getImageByHICON(int width, int height, WinDef.HICON hicon) {
		final WinGDI.ICONINFO iconinfo = new WinGDI.ICONINFO();
		try {
			User32.INSTANCE.GetIconInfo(hicon, iconinfo);
			hotspot_x = iconinfo.xHotspot;
			hotspot_y = iconinfo.yHotspot;
			WinDef.HWND hwdn = new WinDef.HWND();
			WinDef.HDC dc = User32.INSTANCE.GetDC(hwdn);
			try {
				int nBits = width * height * 4;
				Memory colorBitsMem = new Memory(nBits);
				WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
				bmi.bmiHeader.biWidth = width;
				bmi.bmiHeader.biHeight = -height;
				bmi.bmiHeader.biPlanes = 1;
				bmi.bmiHeader.biBitCount = 32;
				bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
				GDI32.INSTANCE.GetDIBits(dc, iconinfo.hbmColor, 0, height, colorBitsMem, bmi, WinGDI.DIB_RGB_COLORS);
				int[] colorBits = colorBitsMem.getIntArray(0, width * height);
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				bi.setRGB(0, 0, width, height, colorBits, 0, height);
				return bi;
			} finally {
				User32.INSTANCE.ReleaseDC(hwdn, dc);
			}
		} finally {
			GDI32.INSTANCE.DeleteObject(iconinfo.hbmColor);
			GDI32.INSTANCE.DeleteObject(iconinfo.hbmMask);
			User32.INSTANCE.DestroyIcon(new WinDef.HICON(hicon.getPointer()));
		}
	}
}
