package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.IPv4Comparator;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeviceSelection extends JDialog {
	private static final long serialVersionUID = 1L;

	
	private Future<?> updateTaskFuture;

	public DialogDeviceSelection(final Window owner, UsnaEventListener<ShellyAbstractDevice, Future<?>> listener, Devices model) {
		super(owner, LABELS.getString("dlgSelectorTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				columnModel.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;
					@Override
					public void setValue(Object value) {
						setText(((InetAddress)value).getHostAddress());
					}
				});
			}

			@Override
			protected String cellTooltipValue(Object value, boolean cellTooSmall, int row, int column) {
				if(cellTooSmall && value instanceof InetAddress) {
					return ((InetAddress)value).getHostAddress();
				} else {
					return super.cellTooltipValue(value, cellTooSmall, row, column);
				}
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		for(int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice d = model.get(i);
//			if(d.getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
				tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress());
//			}
		}

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());
		panel.add(btnClose);
		
		// Find panel
		JPanel panelFind = new JPanel();
		getContentPane().add(panelFind, BorderLayout.NORTH);
		
		FlowLayout flowLayout = (FlowLayout) panelFind.getLayout();
		flowLayout.setVgap(4);
		flowLayout.setAlignment(FlowLayout.LEFT);
		panelFind.add(new JLabel(LABELS.getString("lblFilter")));
		
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
		textFieldFilter.setColumns(24);
		panelFind.add(textFieldFilter);
		textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
			private final int[] cols = new int[] {0, 1};
			@Override
			public void changedUpdate(DocumentEvent e) {
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setRowFilter(textFieldFilter.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				setRowFilter(textFieldFilter.getText());
			}
			
			public void setRowFilter(String filter) {
				TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
				if(filter.length() > 0) {
					filter = filter.replace("\\E", "\\e");
					sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
				} else {
					sorter.setRowFilter(null);
				}
			}
		});
		getRootPane().registerKeyboardAction(e -> textFieldFilter.requestFocus(), KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		final Action eraseFilterAction = new UsnaAction(this, "/images/erase-9-16.png", null, e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
			table.clearSelection();
		});
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, MainView.SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		panelFind.add(eraseFilterButton);
		
		// Sort
		TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
		sorter.setComparator(1, new IPv4Comparator());
		table.sortByColumn(0, true);

		// Selection
		ExecutorService exeService = Executors.newFixedThreadPool(1);
		table.getSelectionModel().addListSelectionListener(event -> {
			if(event.getValueIsAdjusting() == false) {
				if(updateTaskFuture != null) {
					updateTaskFuture.cancel(true);
				}
				updateTaskFuture = exeService.submit(() -> {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						listener.update(model.get(table.convertRowIndexToModel(table.getSelectedRow())), updateTaskFuture);
					} finally {
						setCursor(Cursor.getDefaultCursor());
					}
				});
			}
		});
		
		// Select & close (first click do select)
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
		        	dispose();
		        }
		    }
		});

		setSize(400, 400);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}
}