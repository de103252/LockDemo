package com.ibm.de103252.lockdemo;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class RowSetTableModel extends AbstractTableModel implements RowSetListener {
    private RowSet rowSet = null;

    public RowSetTableModel(RowSet set) {
        rowSet = set;
        rowSet.addRowSetListener(this);
    }

    @Override
    public void cursorMoved(RowSetEvent event) {
    }

    @Override
    public Class<?> getColumnClass(int column) {
        final String cname;
        final int type;
        try {
            ResultSetMetaData meta = rowSet.getMetaData();
            if (meta == null) {
                return null;
            }
            type = meta.getColumnType(column + 1);
        } catch (SQLException e) {
            e.printStackTrace();
            return super.getColumnClass(column);
        }
        switch (type) {
        case Types.BIT: {
            cname = "java.lang.Boolean";
            break;
        }
        case Types.TINYINT: {
            cname = "java.lang.Byte";
            break;
        }
        case Types.SMALLINT: {
            cname = "java.lang.Short";
            break;
        }
        case Types.INTEGER: {
            cname = "java.lang.Integer";
            break;
        }
        case Types.BIGINT: {
            cname = "java.lang.Long";
            break;
        }
        case Types.FLOAT:
        case Types.REAL: {
            cname = "java.lang.Float";
            break;
        }
        case Types.DOUBLE: {
            cname = "java.lang.Double";
            break;
        }
        case Types.NUMERIC: {
            cname = "java.lang.Number";
            break;
        }
        case Types.DECIMAL: {
            cname = "java.math.BigDecimal";
            break;
        }
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR: {
            cname = "java.lang.String";
            break;
        }
        case Types.DATE: {
            cname = "java.sql.Date";
            break;
        }
        case Types.TIME: {
            cname = "java.sql.Time";
            break;
        }
        case Types.TIMESTAMP: {
            cname = "java.sql.Timestamp";
            break;
        }
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY: {
            cname = "byte[]";
            break;
        }
        case Types.OTHER:
        case Types.JAVA_OBJECT: {
            cname = "java.lang.Object";
            break;
        }
        case Types.CLOB: {
            cname = "java.sql.Clob";
            break;
        }
        case Types.BLOB: {
            cname = "java.ssql.Blob";
            break;
        }
        case Types.REF: {
            cname = "java.sql.Ref";
            break;
        }
        case Types.STRUCT: {
            cname = "java.sql.Struct";
            break;
        }
        default: {
            return super.getColumnClass(column);
        }
        }
        try {
            return Class.forName(cname);
        } catch (Exception e) {
            e.printStackTrace();
            return super.getColumnClass(column);
        }
    }

    @Override
    public int getColumnCount() {
        try {
            ResultSetMetaData meta = rowSet.getMetaData();
            if (meta == null) {
                return 0;
            }
            return meta.getColumnCount();
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public String getColumnName(int col) {
        try {
            ResultSetMetaData meta = rowSet.getMetaData();
            if (meta == null) {
                return null;
            }
            return meta.getColumnName(col + 1);
        } catch (SQLException e) {
            return "Error";
        }
    }

    @Override
    public int getRowCount() {
        try {
            if (rowSet.last()) {
                return (rowSet.getRow());
            } else {
                return 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        try {
            if (!rowSet.absolute(row + 1)) {
                return null;
            }
            return rowSet.getObject(col + 1);
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public void rowChanged(RowSetEvent event) {
        try {
            int row = rowSet.getRow();
            if (rowSet.rowDeleted()) {
                fireTableRowsDeleted(row, row);
            } else if (rowSet.rowInserted()) {
                fireTableRowsInserted(row, row);
            } else if (rowSet.rowUpdated()) {
                fireTableRowsUpdated(row, row);
            }
        } catch (SQLException e) {
        }
    }

    @Override
    public void rowSetChanged(RowSetEvent event) {
        fireTableDataChanged();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        try {
            if (!rowSet.absolute(row + 1)) {
                return;
            }
            rowSet.updateObject(column + 1, value);
        } catch (SQLException e) {
        }
    }
}