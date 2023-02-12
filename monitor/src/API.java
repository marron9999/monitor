import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api")
public class API extends APIBase {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = getParameter(request, "id");
		String file = getParameter(request, "file");
		String temp = getParameter(request, "temp");
		if(id.length() > 0) {

			if(file.length() > 0) {
				File dfile = WS.client_file(id, file);
				byte[] buf = getLocalFile(dfile);
				if(buf != null) {
					setCache(response);
					response.setHeader("Content-Disposition",
							"attachment; filename=\"" + file + "\"");
					String type = Files.probeContentType(new File(file).toPath());
					if(type.startsWith("text/")) type = "text/plain";
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
					WS.client_update_files(id);
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
