package com.ibm.de103252.lockdemo;

import java.awt.Color;
import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom table cell renderer that highlights lock conflicts. Rows with
 * conflicting locks are highlighted in different colors.
 */
public class LockStateRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	// Color scheme for highlighting
	static final Color CONFLICT_COLOR = Color.RED;
	static final Color WAITING_COLOR = Color.ORANGE;
	static final Color EXCLUSIVE_COLOR = new Color(255, 100, 100);
	static final Color INTENT_EXCLUSIVE_COLOR = new Color(255, 180, 180);
	static final Color SHARED_COLOR = Color.GREEN;
	static final Color INTENT_SHARED_COLOR = SHARED_COLOR.brighter();
	static final Color UPDATE_COLOR = new Color(150, 150, 255);

	private int xidColumn = -1;
	private int modeColumn = -1;
	private int stateColumn = -1;
	private int statusColumn = -1;
	private int tableColumn = -1;
	private int locknameColumn = -1;

	private boolean columnIndicesInitialized;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		// Don't override selection color
		if (isSelected) {
			return c;
		}
		findColumnIndices(table);
		Set<String> resourcesWithWaiters = new HashSet<>();
		for (int r = 0; r < table.getRowCount(); r++) {
			if (getColumnValue(table, r, stateColumn).toUpperCase().equals("WAIT")) {
				resourcesWithWaiters.add(getResourceKey(table, r));
			}
		}

		String resource = getResourceKey(table, row);
		String mode = getColumnValue(table, row, modeColumn).toUpperCase();
		String state = getColumnValue(table, row, stateColumn).toUpperCase();
		String status = getColumnValue(table, row, statusColumn).toUpperCase();

		Color col;
		if (state.equals("WAIT")) {
			col = WAITING_COLOR;
		} else if (resourcesWithWaiters.contains(resource)) {
			col = CONFLICT_COLOR;
		} else {
			col = switch (mode) {
			case "IX" -> INTENT_EXCLUSIVE_COLOR;
			case "X" -> EXCLUSIVE_COLOR;
			case "IS" -> INTENT_SHARED_COLOR;
			case "S" -> SHARED_COLOR;
			case "U" -> UPDATE_COLOR;
			default -> table.getBackground();
			};
		}
		c.setBackground(col);
		return c;
	}

	/**
	 * Find the indices of important columns.
	 */
	private void findColumnIndices(JTable table) {
		if (columnIndicesInitialized)
			return;
		for (int i = 0; i < table.getColumnCount(); i++) {
			String colName = table.getColumnName(i).toUpperCase();
			if (colName.contains("XID") || colName.contains("LUWID")) {
				xidColumn = i;
			} else if (colName.contains("MODE")) {
				modeColumn = i;
			} else if (colName.equals("STATE")) {
				stateColumn = i;
			} else if (colName.equals("STATUS")) {
				statusColumn = i;
			} else if (colName.contains("TABLENAME") || colName.contains("OBJECT")) {
				tableColumn = i;
			} else if (colName.contains("LOCKNAME") || colName.contains("RID") || colName.contains("PAGENUM")) {
				locknameColumn = i;
			}
		}
		columnIndicesInitialized = true;
	}

	/**
	 * Get a unique key for the resource being locked.
	 */
	private String getResourceKey(JTable table, int row) {
		String tableName = getColumnValue(table, row, tableColumn);
		String lockName = getColumnValue(table, row, locknameColumn);

		if (tableName == null)
			return null;

		// Combine table and lock name for unique resource identifier
		if (lockName != null && !lockName.trim().isEmpty()) {
			return tableName + ":" + lockName;
		}
		return tableName;
	}

	/**
	 * Get the value of a column as a string.
	 */
	private String getColumnValue(JTable table, int row, int column) {
		if (column == -1 || row >= table.getRowCount())
			return "";
		Object value = table.getValueAt(row, column);
		return value != null ? value.toString().trim() : "";
	}

	/**
	 * Check if there's an exclusive lock conflict on a resource.
	 */
	private boolean hasExclusiveLockConflict(JTable table, Set<String> rows, String currentMode) {
		if (rows.size() <= 1)
			return false;

		// Check if any lock is exclusive
		for (String rowStr : rows) {
			int row = Integer.parseInt(rowStr);
			String mode = getColumnValue(table, row, modeColumn);
			if (mode != null) {
				String upperMode = mode.toUpperCase();
				// Exclusive locks conflict with everything
				if (upperMode.contains("X") || upperMode.equals("EXCLUSIVE")) {
					return true;
				}
			}
		}

		return false;
	}
}

// Made with Bob
