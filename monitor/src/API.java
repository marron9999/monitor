import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api")
public class API extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = getParameter(request, "id");
		if(id != null
		&& id.length() > 0) {
			BufferedImage img = WS.client_image(id);
			if(img != null) {
				ServletOutputStream sos = getImageOutputStream(response, "PNG");
				ImageIO.write(img, "PNG", sos);
				sos.close();
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = getParameter(request, "id");
		if(id != null
		&& id.length() > 0) {
			BufferedImage img = getRequestBody_Image(request);
			if(img != null) {
				WS.client_image(id, img);
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

//	private byte[] getRequestBody(HttpServletRequest request, int len) {
//		try {
//			InputStream is = request.getInputStream();
//			byte[] buffer = new byte[len];
//			is.read(buffer);
//			return buffer;
//		} catch (Exception e) {
//			// NONE
//		}
//		return null;
//	}

//	private byte[] getRequestBody(HttpServletRequest request) {
//		ByteArrayOutputStream body = new ByteArrayOutputStream();
//		InputStream is = null;
//		try {
//			is = request.getInputStream();
//			byte[] buffer = new byte[4096];
//			int length;
//			while ((length = is.read(buffer)) >= 0) {
//				body.write(buffer, 0, length);
//			}
//			is.close();
//			buffer = body.toByteArray();
//			body.close();
//			return buffer;
//		} catch (Exception e) {
//			// NONE
//		}
//		return null;
//	}

	private BufferedImage getRequestBody_Image(HttpServletRequest request) {
		InputStream is = null;
		try {
			is = request.getInputStream();
			BufferedImage img = ImageIO.read(is);
			is.close();
			return img;
		} catch (Exception e) {
			// NONE
		}
		return null;
	}
	
//	private void getRequestURI(PrintStream ps, HttpServletRequest request, boolean uri) {
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

	private void setAccessControlAllow(HttpServletRequest request, HttpServletResponse response)
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

	private void setCache(HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");
	}

//	private ServletOutputStream getHTMLOutputStream(HttpServletResponse response)
//			throws ServletException, IOException {
//		setCache(response);
//		response.setContentType("text/html");
//		ServletOutputStream sos = response.getOutputStream();
//		return sos;
//	}

	private ServletOutputStream getImageOutputStream(HttpServletResponse response, String type)
			throws ServletException, IOException {
		setCache(response);
		response.setContentType("image/" + type);
		ServletOutputStream sos = response.getOutputStream();
		return sos;
	}

	private String getParameter(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		if (value == null)
			return null;
		return value.trim();
	}
}
