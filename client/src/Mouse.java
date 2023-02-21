import java.awt.Dimension;
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
import com.sun.jna.platform.WindowUtils;

public class Mouse {
	static {
		System.setProperty("jna.platform.library.path", ".");
	}

	private static List<Long> mouses = new ArrayList<>();
	private static List<int[]> mouseh = new ArrayList<>();
	
	public void reset() {
		mouses = new ArrayList<>();
		mouseh = new ArrayList<>();
	}

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
			Dimension sz = WindowUtils.getIconSize(cursorinfo.hCursor);
			if(cursorinfo.flags > 0) {
				hotspot_x = sz.width / 2; 
				hotspot_y = sz.height / 2;
			}
			cursor_h = cursorinfo.hCursor.getPointer().hashCode();
		}
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

	public BufferedImage getCursorImage() {
		WinDef.HICON hIcon = new WinDef.HICON(new Pointer(cursor_h));
		final Dimension iconSize = WindowUtils.getIconSize(hIcon);
		if (iconSize.width == 0
		|| iconSize.height == 0)
			return null;

		WinGDI.ICONINFO iconInfo = new WinGDI.ICONINFO();
		User32.INSTANCE.GetIconInfo(hIcon, iconInfo);
		iconInfo.read();

		int width = iconSize.width;
		int height = iconSize.height;
		short depth = 24;

		WinGDI.BITMAPINFOHEADER hdr = new WinGDI.BITMAPINFOHEADER();
		hdr.biWidth = width;
		hdr.biHeight = height;
		if (iconInfo.hbmColor == null) {
			hdr.biHeight += height;
		}
		hdr.biPlanes = 1;
		hdr.biBitCount = depth;
		hdr.biCompression = 0;
		hdr.write();
		WinGDI.BITMAPINFO bitmapInfo = new WinGDI.BITMAPINFO();
		bitmapInfo.bmiHeader = hdr;
		bitmapInfo.write();

		WinDef.HDC hDC = User32.INSTANCE.GetDC(null);

		byte[] lpBitsColor = null;
		Pointer lpBitsColorPtr = null;
		byte[] lpBitsMask = null;
		Pointer lpBitsMaskPtr = null;
		int masklen = width * height * depth / 8;
		int j = 0;
		if (iconInfo.hbmColor == null) {
			j = masklen;
			lpBitsMask = new byte[masklen * 2];
			lpBitsMaskPtr = new Memory(lpBitsMask.length);
			GDI32.INSTANCE.GetDIBits(hDC, iconInfo.hbmMask, 0, hdr.biHeight, lpBitsMaskPtr, bitmapInfo, 0);
			lpBitsMaskPtr.read(0, lpBitsMask, 0, lpBitsMask.length);
			lpBitsColor = lpBitsMask;
		} else {
			lpBitsMask = new byte[masklen];
			lpBitsMaskPtr = new Memory(lpBitsMask.length);
			GDI32.INSTANCE.GetDIBits(hDC, iconInfo.hbmMask, 0, hdr.biHeight, lpBitsMaskPtr, bitmapInfo, 0);
			lpBitsMaskPtr.read(0, lpBitsMask, 0, lpBitsMask.length);
			lpBitsColor = new byte[masklen];
			lpBitsColorPtr = new Memory(lpBitsColor.length);
			GDI32.INSTANCE.GetDIBits(hDC, iconInfo.hbmColor, 0, hdr.biHeight, lpBitsColorPtr, bitmapInfo, 0);
			lpBitsColorPtr.read(0, lpBitsColor, 0, lpBitsColor.length);
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int r, g, b, a, argb;
		int x = 0, y = height - 1;
		for (int i = 0; i < masklen; i = i + 3, j = j + 3) {
			b = lpBitsColor[i] & 0x00FF;
			g = lpBitsColor[i + 1] & 0x00FF;
			r = lpBitsColor[i + 2] & 0x00FF;
			a = 0xFF - lpBitsMask[j] & 0x00FF;
			argb = (a << 24) | (r << 16) | (g << 8) | b;
			image.setRGB(x, y, argb);
			x = (x + 1) % width;
			if (x == 0)
				y--;
		}

		User32.INSTANCE.ReleaseDC(null, hDC);
		if (iconInfo.hbmColor != null)
			GDI32.INSTANCE.DeleteObject(iconInfo.hbmColor);
		GDI32.INSTANCE.DeleteObject(iconInfo.hbmMask);

		return image;
	}
}
