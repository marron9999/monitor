import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public abstract class Frame extends JFrame implements DropTargetListener {
	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	public OutputStream stream;
	private DataFlavor flavor = DataFlavor.javaFileListFlavor;

	public abstract void drop(File file);

	public int getEndPosition() {
		return textArea.getDocument().getLength();
	}

	public void setEndPosition() {
		textArea.setCaretPosition(getEndPosition());
	}

	public Frame() { // Constructor
		stream = new OutputStream() {
			public void write(byte[] b) throws IOException {
				textArea.append(new String(b));
				setEndPosition();
			}

			public void write(byte[] b, int off, int len) throws IOException {
				textArea.append(new String(b, off, len));
				setEndPosition();
			}

			public void write(int b) throws IOException {
				textArea.append(Character.toString((char) b));
				setEndPosition();
			}
		};

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			InputStream is = getClass().getResourceAsStream("monitor.png");
			BufferedImage bi = ImageIO.read(is);
			setIconImage(bi);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// setTitle("Swing Demo");
		setSize(400, 300);

		JMenuBar menuBar = new JMenuBar();
		JLabel label = new JLabel("　");
		menuBar.add(label);
		label = new JLabel("Clear");
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				textArea.setText("");
			}
		});
		menuBar.add(label);
		label = new JLabel("　");
		menuBar.add(label);
		label = new JLabel("Upload");
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				select();
			}
		});
		menuBar.add(label);
		label = new JLabel("　");
		menuBar.add(label);
		label = new JLabel("Downloads");
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					File dir = new File(System.getenv("USERPROFILE"), "Downloads");
					Runtime runtime = Runtime.getRuntime();
					runtime.exec(new String[] { "explorer", dir.getAbsolutePath() });
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		menuBar.add(label);
		setJMenuBar(menuBar);

		textArea = new JTextArea("");
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setFocusable(false);
		Font font = textArea.getFont();
		textArea.setFont(new Font("Meiryo UI", 0, font.getSize()));
		scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(new EmptyBorder(0, 4, 0, 0));
		scrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		new DropTarget(textArea, this);
		setVisible(true);		
	}

	public void select() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		if(chooser.showDialog(this, "Select upload files")
				== JFileChooser.APPROVE_OPTION) {
			File[] list = chooser.getSelectedFiles();
			for (File file : list) {
				drop(file);
			}
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		if (dtde.isDataFlavorSupported(flavor)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
			return;
		}
		dtde.rejectDrag();
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		if (dtde.isDataFlavorSupported(flavor)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
			return;
		}
		dtde.rejectDrag();
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		dtde.acceptDrop(DnDConstants.ACTION_COPY);
		boolean flg = false;
		try {
			if (dtde.isDataFlavorSupported(flavor)) {
				Transferable tr = dtde.getTransferable();
				@SuppressWarnings("unchecked")
				List<File> list = (List<File>) tr.getTransferData(flavor);
				for (File file : list) {
					drop(file);
				}
				flg = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dtde.dropComplete(flg);
		}
	}
}
