package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrableTask.Status;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.DeferrablesContainer.DeferrableRecord;
import it.usna.shellyscan.model.Devices;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeferrables extends JFrame implements UsnaEventListener<DeferrableTask.Status, Integer> {
	private static final long serialVersionUID = 1L;
	//	private final static Logger LOG = LoggerFactory.getLogger(RestoreAction.class);

	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_time"), LABELS.getString("col_devName"), LABELS.getString("col_actionDesc"), LABELS.getString("col_status"));
	private final ExTooltipTable table = new ExTooltipTable(tModel);
	private final DeferrablesContainer deferrables;
	//	private final static int COL_TIME = 0;

	public DialogDeferrables(Window owner, Devices model) {
		super(LABELS.getString("labelShowDeferrables"));
		setIconImages(owner.getIconImages());

		deferrables = DeferrablesContainer.getInstance(model);

		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(c instanceof JLabel label) {
					Status status = deferrables.get(table.convertRowIndexToModel(row)).getStatus();
					if(status == Status.SUCCESS) {
						label.setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_success.png")));
					} else if(status == Status.WAITING) {
						label.setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_waiting.png")));
					} else if(status == Status.CANCELLED) {
						label.setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_cancelled.png")));
					} else if(status == Status.RUNNING) {
						label.setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_running.png")));
					} else if(status == Status.FAIL) {
						label.setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_error.png")));
					}
				}
				return c;
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		JButton abortButton = new JButton(LABELS.getString("btnAbort"));
		abortButton.addActionListener(e -> {
			for(int r: table.getSelectedModelRows()) {
				deferrables.cancel(r);
			}
			abortButton.setEnabled(false);
		});
		abortButton.setEnabled(false);

		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());

		buttonsPanel.add(abortButton);
		buttonsPanel.add(closeButton);

		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		table.getSelectionModel().addListSelectionListener(e -> {
			abortButton.setEnabled(false);
			for(int idx: table.getSelectedRows()) {
				if(deferrables.get(idx).getStatus() == Status.WAITING) {
					abortButton.setEnabled(true);
					break;
				}
			}
		});

		setSize(600, 300);
	}

	@Override
	public synchronized void setVisible(boolean v) {
		super.setVisible(v);
		if(v) {
			fill();
			table.columnsWidthAdapt();
			if(deferrables.hasListener(this) == false) {
				deferrables.addListener(this);
			}
		} else {
			deferrables.removeListener(this);
		}
	}

	private void fill() {
		SwingUtilities.invokeLater(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			tModel.clear();
			for(int i = 0; i < deferrables.size(); i++) {
				tModel.addRow(row(deferrables.get(i)));
			}
			setCursor(Cursor.getDefaultCursor());
		});
	}

	private static Object[] row(DeferrableRecord def) {
		String status = LABELS.getString("defStatus_" + def.getStatus().name());
		String retMsg = def.getRetMsg();
		if(retMsg != null && retMsg.length() > 0) {
			status += " - " + retMsg.replace("\n", "; ");
		}
		return new Object[] {
				String.format(LABELS.getString("formatDataTime"), def.getTime()),
				def.getDeviceName(),
				def.getDescription(),
				status};
	}

	@Override
	public void update(Status mesgType, Integer msgBody) {
		//		System.out.println(mesgType + "-" + msgBody);
		if(mesgType == Status.WAITING) {
			tModel.addRow(row(deferrables.get(msgBody)));
		} else {
			tModel.setRow(msgBody, row(deferrables.get(msgBody)));
		}
	}
}