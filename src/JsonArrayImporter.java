import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.sql.*;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 */
class JsonArrayImporter {
    private ConnectionProperties p;

    JsonArrayImporter(ConnectionProperties p) {
        this.p = p;
    }

    /**
     * Imports the collection of {@code JsonNode} to the database table specified when creating the object. For each
     * node, it will insert the specified field values to the specified table. If a node does not contain a specified
     * field name, or the table field contains an unsupported field type, null will be inserted.
     * @throws SQLException
     */
    void doImport(String tableName, List<Field> fields, Collection<JsonNode> importNodes)
            throws SQLException, ClassNotFoundException {
        Connection cn = DatabaseConnection.getConnection(p);
        try {
            StringBuilder fieldsClause = new StringBuilder();
            StringBuilder valuesClause = new StringBuilder();

            for (Field f: fields) {
                if (fieldsClause.length() != 0) {
                    fieldsClause.append(", ");
                }
                //wrap field names in [] in case a field name conflicts with SQL keywords
                fieldsClause.append("[").append(f.getFieldName()).append("]");

                if (valuesClause.length() != 0) {
                    valuesClause.append(", ");
                }
                valuesClause.append("?");
            }

            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldsClause, valuesClause);
            PreparedStatement st = cn.prepareStatement(sql);
            for (JsonNode n: importNodes) {
                int i = 1;
                st.clearParameters();
                for (Field f: fields) {
                    JsonNode val = n.get(f.getFieldName());

                    if (val == null) {
                        //the field does not exist in current node, try to set value to null
                        st.setNull(i, f.getFieldType());

                    } else if (f.isTextual()) {
                        st.setString(i, val.asText());

                    } else if (f.isInt()) {
                        st.setLong(i, val.asInt());

                    } else if (f.isBoolean()) {
                        st.setBoolean(i, val.asBoolean());

                    } else if (f.isDecimal()) {
                        st.setBigDecimal(i, val.decimalValue());

                    } else if (f.isFloatingPoint()) {
                        st.setDouble(i, val.doubleValue());

                    } else if (f.isLong()) {
                        st.setLong(i, val.longValue());

                    } else {
                        //unrecognised field type val, try to set value to null
                        st.setNull(i, f.getFieldType());
                    }

                    i++;
                }
                st.execute();
            }
        } finally {
            cn.close();
        }
    }
}
