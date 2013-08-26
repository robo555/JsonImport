import java.util.*;
import java.sql.*;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 *
 * Utility methods for creating a connection to a SQL Server database, and to verify the fields of a table exists.
 */
class DatabaseConnection {
    /**
     * Creates a database connection to the database specified by {@code p}.
     * @param p connection parameters to a SQL Server database.
     * @return  connection to the database.
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    static Connection getConnection(ConnectionProperties p) throws SQLException, ClassNotFoundException {
        Class.forName("net.sourceforge.jtds.jdbc.Driver");

        String connectionString = String.format("jdbc:jtds:sqlserver://%s:%d/%s", p.server, p.port, p.database);
        if (!p.instance.isEmpty()) {
            connectionString += ";instance=" + p.instance;
        }
        return DriverManager.getConnection(connectionString, p.user, p.password);
    }

    /**
     * Verifies field names in {@code fields} exist in {@code table}, and add the list of field names and their
     * data type to {@code verifiedFields}.
     * @param p connection parameters to a SQL Server database.
     * @param table name of the table to verify.
     * @param fields list of field names that should exist in {code table}.
     * @param verifiedFields list of {@code Field} objects containing names and data types of the verified fields.
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    static void verifyTable(ConnectionProperties p, String table, String[] fields, List<Field> verifiedFields)
            throws SQLException, ClassNotFoundException {
        Connection cn = getConnection(p);
        try {
            StringBuilder fieldsClause = new StringBuilder();
            for (String s: fields) {
                if (fieldsClause.length() != 0) {
                    fieldsClause.append(", ");
                }
                fieldsClause.append(s);
            }

            String sql = String.format("SELECT TOP 1 %s FROM %s", fieldsClause, table);
            Statement st = cn.createStatement();
            ResultSetMetaData md = st.executeQuery(sql).getMetaData();
            int count = md.getColumnCount();
            verifiedFields.clear();
            for (int i = 1; i <= count; i++) {
                Field f = new Field(
                        md.getColumnName(i),
                        md.getColumnType(i),
                        md.getColumnTypeName(i));
                if (!isSupportedType(f)) {
                    System.out.format("Field %s is a non supported field type %s%n",
                            f.getFieldName(), f.getFieldTypeName());
                }
                verifiedFields.add(f);
            }
        } finally {
            cn.close();
        }
    }

    /**
     * returns true if {@code f} is supported for import. Return true if {@code isBoolean}, {@code isTextual},
     * {@code isInt}, {@code isLong}, {@code isDecimal}, or {@code isFloatingPoint} is true.
     * @return true if field is supported for import.
     */
    private static boolean isSupportedType(Field f) {
        return f.isBoolean()
                || f.isTextual()
                || f.isInt()
                || f.isLong()
                || f.isDecimal()
                || f.isFloatingPoint();
    }
}
