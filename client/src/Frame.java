import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public abstract class Frame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private JLabel cpu;
//	private JLabel mem;
//	private JLabel drv;
	public OutputStream stream;
	private DataFlavor flavor = DataFlavor.javaFileListFlavor;
	private BufferedImage icon;

	public abstract void drop_file(File file);

	private int getEndPosition() {
		return textArea.getDocument().getLength();
	}

	private void setEndPosition() {
		textArea.setCaretPosition(getEndPosition());
	}

	private String usage(String val) {
		int p = val.indexOf(" ");
		if(p > 0)
			val = val.substring(p + 1);
		p = val.indexOf(" ");
		if(p > 0)
			val = val.substring(0, p);
		p = val.indexOf("%");
		if(p > 0)
			val = val.substring(0, p);
		//val = "  " + val + "%";
		//val = val.substring(val.length() - 4);
		return val;
	}
	private int pcpu = 0; 
	public void setCPU(String val) {
		val = usage(val).trim();
		int px = Integer.parseInt(val) / 7; 
		val = "  " + val;
		val = val.substring(val.length() - 3);
		cpu.setText(" CPU" + val + "% ");
		if(pcpu != px) {
			pcpu = px;
			BufferedImage bi = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics g = bi.getGraphics();
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, 16, 16);
			if(px > 0) {
				g.setColor(new Color(0, 0x00aa, 0));
				g.fillRect(0, 16 - px, 16, px);
			}
			g.drawImage(icon, 0,  0, null);
			g.dispose();
			setIconImage(bi);
		}
	}
	public void setMEM(String val) {
//		mem.setText(" MEM" + usage(val) + " ");
	}
	public void setDRV(String val) {
//		drv.setText(" DRV" + usage(val) + " ");
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
			icon = ImageIO.read(is);
			setIconImage(icon);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// setTitle("Swing Demo");
		setSize(400, 300);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		menuBar.add(panel, BorderLayout.WEST);
		JLabel label = new JLabel(" Clear ");
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				textArea.setText("");
			}
		});
		panel.add(label);
		label = new JLabel(" Upload ");
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				select();
			}
		});
		panel.add(label);
		label = new JLabel(" Downloads ");
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
		panel.add(label);
		
		panel = new JPanel();
		panel.setOpaque(false);
		menuBar.add(panel, BorderLayout.EAST);
		Font font = label.getFont();
		font = new Font(Font.MONOSPACED, 0, font.getSize());
		panel.add(cpu = new JLabel(" CPU   0% "));
		cpu.setFont(font);
//		panel.add(mem = new JLabel(" MEM   0% "));
//		mem.setFont(font);
//		panel.add(drv = new JLabel(" DRV   0% "));
//		drv.setFont(font);
		setJMenuBar(menuBar);

		textArea = new JTextArea("");
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setFocusable(false);
		font = textArea.getFont();
		textArea.setFont(new Font("Meiryo UI", 0, font.getSize()));
		scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(new EmptyBorder(0, 4, 0, 0));
		scrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		new DropTarget(textArea, new DropTargetListener() {
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
							drop_file(file);
						}
						flg = true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					dtde.dropComplete(flg);
				}
			}
		});
		setVisible(true);		
	}

	private void select() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		if(chooser.showDialog(this, "Select upload files")
				== JFileChooser.APPROVE_OPTION) {
			File[] list = chooser.getSelectedFiles();
			for (File file : list) {
				drop_file(file);
			}
		}
	}
}
