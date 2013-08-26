import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 * Tests JsonNodes get inserted into the database table correctly. The test setup is not ideal as the connection
 * details are hard coded, table needs to be setup manually, and a bit slow to run each test.
 *
 * Could look into using interfaces and mock objects, but that's a bit much for this little project.
 */
public class JsonArrayImporterTest {
    @Before
    public void setUp() throws Exception {
        truncateTable();
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test importing string, int, and decimal values work successfully
     * @throws Exception
     */
    @Test
    public void testImportSimple() throws Exception {
        ConnectionProperties p = getConnectionProperties();
        Connection cn = DatabaseConnection.getConnection(p);
        try {
            List<Field> verifiedFields = new Vector<Field>();
            String[] fields = "ListingId, Title, StartPrice".split(",");
            String tableName = "Listings";
            DatabaseConnection.verifyTable(p, tableName, fields, verifiedFields);

            Collection<JsonNode> importNodes = new LinkedList<JsonNode>();
            JsonNodeFactory f = JsonNodeFactory.instance;

            int listingId = 1559350;
            String title = "Product Pro1-Beta-0066";
            BigDecimal startPrice = BigDecimal.valueOf(20.64);

            ObjectNode n;
            n = new ObjectNode(f);
            n.put("ListingId", listingId);
            n.put("Title", title);
            n.put("StartPrice", startPrice);
            importNodes.add(n);

            JsonArrayImporter importer = new JsonArrayImporter(p);
            importer.doImport(tableName, verifiedFields, importNodes);

            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT ListingId, Title, StartPrice FROM Listings");
            assertTrue("Expected result set to contain a record", rs.next());
            assertEquals(listingId, rs.getInt("ListingId"));
            assertEquals(title, rs.getString("Title"));
            assertEquals(startPrice, rs.getBigDecimal("StartPrice"));
            assertFalse("Expect result set to only contain one record", rs.next());
        } finally {
            cn.close();
        }
    }

    /**
     * Test importing an unsupported field e.g. datetime, the program should continue and assign null value to the field.
     * @throws Exception
     */
    @Test
    public void testImportUnsupportedField() throws Exception {
        ConnectionProperties p = getConnectionProperties();
        Connection cn = DatabaseConnection.getConnection(p);
        try {
            List<Field> verifiedFields = new Vector<Field>();
            String[] fields = "ListingId, Title".split(",");
            String tableName = "Listings";
            DatabaseConnection.verifyTable(p, tableName, fields, verifiedFields);

            Collection<JsonNode> importNodes = new LinkedList<JsonNode>();
            JsonNodeFactory f = JsonNodeFactory.instance;

            int listingId = 1559350;

            ObjectNode n;
            n = new ObjectNode(f);
            n.put("ListingId", listingId);
            importNodes.add(n);

            JsonArrayImporter importer = new JsonArrayImporter(p);
            importer.doImport(tableName, verifiedFields, importNodes);

            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT ListingId, Title FROM Listings");
            assertTrue("Expected result set to contain a record", rs.next());
            assertEquals(listingId, rs.getInt("ListingId"));
            assertEquals(null, rs.getString("Title"));
        } finally {
            cn.close();
        }
    }


    /**
     * Test importing a field that exists in the database, but not in the JsonNode. The program will continue
     * and assign null value to the field.
     * @throws Exception
     */
    @Test
    public void testImportMissingField() throws Exception {
        ConnectionProperties p = getConnectionProperties();
        Connection cn = DatabaseConnection.getConnection(p);
        try {
            List<Field> verifiedFields = new Vector<Field>();
            String[] fields = "ListingId, StartDate".split(",");
            String tableName = "Listings";
            DatabaseConnection.verifyTable(p, tableName, fields, verifiedFields);

            Collection<JsonNode> importNodes = new LinkedList<JsonNode>();
            JsonNodeFactory f = JsonNodeFactory.instance;

            int listingId = 1559350;

            ObjectNode n;
            n = new ObjectNode(f);
            n.put("ListingId", listingId);
            importNodes.add(n);

            JsonArrayImporter importer = new JsonArrayImporter(p);
            importer.doImport(tableName, verifiedFields, importNodes);

            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT ListingId, Title FROM Listings");
            assertTrue("Expected result set to contain a record", rs.next());
            assertEquals(listingId, rs.getInt("ListingId"));
            assertEquals(null, rs.getString("Title"));
        } finally {
            cn.close();
        }
    }

    public ConnectionProperties getConnectionProperties() {
        return new ConnectionProperties("DJANGO", 1434, "auctions", "SQLEXPRESS", "abm", "abm");
    }

    private void truncateTable() throws SQLException, ClassNotFoundException {
        ConnectionProperties p = getConnectionProperties();
        Connection cn = DatabaseConnection.getConnection(p);
        Statement st = cn.createStatement();
        st.execute("TRUNCATE TABLE Listings");
    }
}
