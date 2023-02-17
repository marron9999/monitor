import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class APIBase extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static File downloads = 
			new File(System.getenv("USERPROFILE"), "Downloads");

	protected byte[] getDownloadsFile(String file) {
		if(file == null) return null;
		File dir = new File(downloads, file);
		try {
			byte[] buffer = getLocalFile(dir);
			return buffer;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}

	protected  byte[] getLocalFile(File file) {
		if(file == null) return null;
		if( ! file.exists()) return null;
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int length;
			while ((length = fis.read(buffer)) >= 0) {
				body.write(buffer, 0, length);
			}
			fis.close();
			buffer = body.toByteArray();
			body.close();
			return buffer;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}

	protected byte[] getRequestBody(HttpServletRequest request) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		InputStream is = null;
		try {
			is = request.getInputStream();
			byte[] buffer = new byte[4096];
			int length;
			while ((length = is.read(buffer)) >= 0) {
				body.write(buffer, 0, length);
			}
			is.close();
			buffer = body.toByteArray();
			body.close();
			return buffer;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}

	protected File getRequestBody_downloads(HttpServletRequest request, String file) {
		if(file == null) return null;
		File dir = new File(downloads, file);
		return getRequestBody_local(request, dir);
	}

	protected File getRequestBody_local(HttpServletRequest request, File file) {
		if(file == null) return null;
		if(file.exists()) file.delete();
		try {
			FileOutputStream fos = new FileOutputStream(file);
			InputStream is = request.getInputStream();
			byte[] buffer = new byte[4096];
			int length;
			while ((length = is.read(buffer)) >= 0) {
				fos.write(buffer, 0, length);
			}
			is.close();
			fos.close();
			return file;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}

	protected BufferedImage getRequestBody_Image(HttpServletRequest request) {
		try {
			InputStream is = request.getInputStream();
			BufferedImage img = ImageIO.read(is);
			is.close();
			return img;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}
	
//	protected void getRequestURI(PrintStream ps, HttpServletRequest request, boolean uri) {
//		ps.print(request.getRemoteAddr() + " ");
//		//ps.print(request.getRemoteUser() + " ");
//		if(uri) {
//			ps.print(request.getRequestURI() + "?");
//			boolean c = false;
//			Enumeration<String> n = request.getParameterNames();
//			while(n.hasMoreElements()) {
//				String s = (String) n.nextElement();
//				String[] v = request.getParameterValues(s);
//				for(String t : v) {
//					if(c) ps.print("&");
//					c = true;
//					ps.print(s + "=" + t);
//				}
//			}
//		}
//		ps.println();
//	}

	protected void setAccessControlAllow(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String value = request.getHeader("referer");
		if (value != null) {
			int p = value.indexOf("//");
			p = value.indexOf("/", p + 2);
			response.setHeader("Access-Control-Allow-Origin", value.substring(0, p));
		} else {
			response.setHeader("Access-Control-Allow-Origin", "*");
		}
		response.setHeader("Access-Control-Allow-Credentials", "true");
	}

	protected void setCache(HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");
	}

//	protected ServletOutputStream getHTMLOutputStream(HttpServletResponse response)
//			throws ServletException, IOException {
//		setCache(response);
//		response.setContentType("text/html");
//		ServletOutputStream sos = response.getOutputStream();
//		return sos;
//	}

	protected ServletOutputStream getImageOutputStream(HttpServletResponse response, String type)
			throws ServletException, IOException {
		setCache(response);
		response.setContentType("image/" + type);
		ServletOutputStream sos = response.getOutputStream();
		return sos;
	}

	protected String getParameter(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		if (value == null)
			return "";
		return value.trim();
	}

	protected byte[] getStream_bytes(InputStream is) {
		try {
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int length;
			while ((length = is.read(buffer)) >= 0) {
				body.write(buffer, 0, length);
			}
			is.close();
			buffer = body.toByteArray();
			body.close();
			return buffer;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}

	protected File getStream_file(InputStream is, File file) {
		if(file == null) return null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			byte[] buffer = new byte[4096];
			int length;
			while ((length = is.read(buffer)) >= 0) {
				fos.write(buffer, 0, length);
			}
			is.close();
			fos.close();
			return file;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}
}
