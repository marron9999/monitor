import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api")
public class API extends Base {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = getParameter(request, "id");
		String file = getParameter(request, "file");
		String temp = getParameter(request, "temp");
		if(id.length() > 0) {

			if(file.length() > 0) {
				byte[] buf = getDownloadsFile(file);
				if(buf != null) {
					setCache(response);
					String type = "application/octet-stream";
					if(file.endsWith(".txt")) type = "text/plain";
					else if(file.endsWith(".log")) type = "text/plain";
					else if(file.endsWith(".js")) type = "text/plain";
					else if(file.endsWith(".json")) type = "text/plain";
					else if(file.endsWith(".css")) type = "text/plain";
					else if(file.endsWith(".html")) type = "text/plain";
					else if(file.endsWith(".htm")) type = "text/plain";
					else if(file.endsWith(".png")) type = "image/png";
					else if(file.endsWith(".jpg")) type = "image/jpeg";
					else if(file.endsWith(".jpeg")) type = "image/jpeg";
					else if(file.endsWith(".bmp")) type = "image/bmp";
					else if(file.endsWith(".gif")) type = "image/gif";
					response.setContentType(type);
					ServletOutputStream sos = response.getOutputStream();
					sos.write(buf);
					sos.close();
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			if(temp.length() > 0) {
				File dir = new File(System.getProperty("java.io.tmpdir"));
				dir = new File(dir, temp);
				byte[] buf = getLocalFile(dir);
				if(buf != null) {
					dir.delete();
					setCache(response);
					response.setContentType("application/octet-stream");
					ServletOutputStream sos = response.getOutputStream();
					sos.write(buf);
					sos.close();
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			BufferedImage img = WS.client_image(id);
			if(img != null) {
				ServletOutputStream sos = getImageOutputStream(response, "PNG");
				ImageIO.write(img, "PNG", sos);
				sos.close();
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}

			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = getParameter(request, "id");
		String file = getParameter(request, "file");
		if(id.length() > 0) {

			if(file.length() > 0) {
				File dfile = getRequestBody_downloads(request, file);
				if(dfile != null) {
					WS.client_file(id, dfile);
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			BufferedImage img = getRequestBody_Image(request);
			if(img != null) {
				WS.client_image(id, img);
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}
}
