import java.io.File;
import java.io.IOException;
import java.util.Collection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/api2")
@MultipartConfig()
public class API2 extends APIBase {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setAccessControlAllow(request, response);
		String id = "";
		String name = "";
		Collection<Part> parts = request.getParts();
		for(Part part : parts) {
			if(part.getName().equalsIgnoreCase("form_id")) {
				byte[] buf = getStream_bytes(part.getInputStream());
				id = new String(buf, "utf-8");
				continue;
			}
			if(part.getName().equalsIgnoreCase("form_name")) {
				byte[] buf = getStream_bytes(part.getInputStream());
				name = new String(buf, "utf-8");
				continue;
			}
		}
		for(Part part : parts) {
			if(part.getName().equalsIgnoreCase("form_file")) {
				try {
					File dir = new File(System.getProperty("java.io.tmpdir"));
					dir = File.createTempFile("mon_", "", dir);
					dir = getStream_file(part.getInputStream(), dir);
					if(dir != null) {
						WS.client_sendText(id, "download " + dir.getName() + " " + name);
						response.setStatus(HttpServletResponse.SC_OK);
						return;
					}
				} catch (Exception e) {
					// NONE
				}
			}
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return;
	}
}
