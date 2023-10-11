package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.KVS;
import it.usna.shellyscan.model.device.g2.modules.KVS.KVItem;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class KVSPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private ExTooltipTable table;
	private JScrollPane scrollPane = null;

	private static int COL_KEY = 0;
	private static int COL_VALUE = 2;

	public KVSPanel(AbstractG2Device device) {
		setLayout(new BorderLayout(0, 0));

		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			final KVS kvs = new KVS(device);

			final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblKeyColName"), LABELS.getString("lblEtagColName"), LABELS.getString("lblValColName"));

			table = new ExTooltipTable(tModel) {
				private static final long serialVersionUID = 1L;
				{
//				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
					setAutoCreateRowSorter(true);
					setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//				columnModel.getColumn(2).setCellRenderer(new ButtonCellRenderer());
//				columnModel.getColumn(2).setCellEditor(new ButtonCellEditor());
				}

				@Override
				public boolean isCellEditable(final int row, final int column) {
					return true;//convertColumnIndexToModel(column) == COL_VALUE;
				}

				@Override
				public Component prepareEditor(TableCellEditor editor, int row, int column) {
					JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
					comp.setBackground(table.getSelectionBackground());
					comp.setForeground(table.getSelectionForeground());
//					comp.requestFocus();
					return comp;
				}

				@Override
				public void editingStopped(ChangeEvent e) {
					KVSPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						final int mRow = convertRowIndexToModel(getEditingRow());
						if (mRow < kvs.size()) { // existing element -> value
							KVItem item = kvs.edit(mRow, (String) getCellEditor().getCellEditorValue());
							tModel.setRow(mRow, item.key(), item.etag(), item.value());
						} else { // new element -> key edited
							String key = (String) getCellEditor().getCellEditorValue();
							if (kvs.getIndex(key) >= 0) {
								Msg.errorMsg(KVSPanel.this, "msgKVSExisting");
								tModel.removeRow(mRow);
							} else if (key.length() > 0) {
								KVItem item = kvs.add(key, "");
								tModel.setRow(mRow, key, item.etag(), item.value());
							} else {
								tModel.removeRow(mRow);
							}
						}
						super.editingStopped(e);
					} catch (IOException e1) {
						Msg.errorMsg(e1);
					} finally {
						KVSPanel.this.setCursor(Cursor.getDefaultCursor());
					}
				}
				
				@Override
				public void editingCanceled(ChangeEvent e) {
					final int mRow = convertRowIndexToModel(getEditingRow());
					super.editingCanceled(e);
				}
			};

			scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			add(scrollPane, BorderLayout.CENTER);

			JPanel operationsPanel = new JPanel();
			operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
			operationsPanel.setBackground(Color.WHITE);
			add(operationsPanel, BorderLayout.SOUTH);

			final JButton btnDelete = new JButton(LABELS.getString("btnDelete"));
			btnDelete.addActionListener(e -> {
				TableCellEditor editor = table.getCellEditor();
				if(editor != null) {
					editor.cancelCellEditing();
				}
				final String cancel = UIManager.getString("OptionPane.cancelButtonText");
				if (JOptionPane.showOptionDialog(KVSPanel.this, LABELS.getString("msgDeleteConfirm"), LABELS.getString("btnDelete"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						new Object[] { UIManager.getString("OptionPane.yesButtonText"), cancel }, cancel) == 0) {
					try {
						final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
						kvs.delete(mRow);
						tModel.removeRow(mRow);
					} catch (IOException e1) {
						Msg.errorMsg(e1);
					}
				}
			});
			operationsPanel.add(btnDelete);

			final JButton btnNew = new JButton(LABELS.getString("btnNew"));
			btnNew.addActionListener(e -> {
				//			try {
				TableCellEditor editor = table.getCellEditor();
				if(editor != null) {
					editor.cancelCellEditing();
				}
				int row = tModel.addRow( "key", "", "" );
				table.editCellAt(row, COL_KEY);
				table.getEditorComponent().requestFocus();
				//			} catch (/*IO*/Exception e1) {
				//				Msg.errorMsg(e1);
				//			}
			});
			operationsPanel.add(btnNew);

//		JButton btnDownload = new JButton(LABELS.getString("btnDownload"));
//		operationsPanel.add(btnDownload);
//		btnDownload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showSaveDialog(KVSPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
//					w.write(sc.getCode());
//				} catch (IOException e1) {
//					Msg.errorMsg(KVSPanel.this, LABELS.getString("msgScrNoCode"));
//				} finally {
//					setCursor(Cursor.getDefaultCursor());
//				}
//			}
//		});
//
//		JButton btnUpload = new JButton(LABELS.getString("btnUpload"));
//		operationsPanel.add(btnUpload);
//		btnUpload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showOpenDialog(KVSPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				loadCodeFromFile(fc.getSelectedFile(), sc);
//				setCursor(Cursor.getDefaultCursor());
//			}
//		});

			for (KVS.KVItem item : kvs.getItems()) {
				tModel.addRow(item.key(), item.etag(), item.value());
			}
		} catch (IOException e) {
			Msg.errorMsg(e);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}

//		ListSelectionListener l = e -> {
//			final boolean selection = table.getSelectedRowCount() > 0;
//			btnDelete.setEnabled(selection);
//			btnDownload.setEnabled(selection);
//			btnUpload.setEnabled(selection);
////			editBtn.setEnabled(selection);
//		};
//		table.getSelectionModel().addListSelectionListener(l);
//		l.valueChanged(null);
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if (v) {
			table.columnsWidthAdapt();
			TableColumn col0 = table.getColumnModel().getColumn(0);
			col0.setPreferredWidth(col0.getPreferredWidth() * 120 / 100);
			TableColumn col1 = table.getColumnModel().getColumn(1);
			col1.setPreferredWidth(col1.getPreferredWidth() * 120 / 100);
			TableColumn col2 = table.getColumnModel().getColumn(2);
			col2.setPreferredWidth(col2.getPreferredWidth() * 120 / 100);
			if (scrollPane.getViewport().getWidth() > table.getPreferredSize().width) {
				table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			} else {
				table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_OFF);
			}
		}
	}
}