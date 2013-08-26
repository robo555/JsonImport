import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 */
public class JsonArrayReaderTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    private void saveToFile(String json) throws IOException {
        saveToFile(json, getDefaultFileName());
    }

    private void saveToFile(String json, String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        try {
            writer.write(json);
        } finally {
            writer.close();
        }
    }

    private String getDefaultFileName() {
        return "C:\\Users\\Robo\\java\\test.json";
    }

    @Test
    public void testGetNullArrayName() throws Exception {
        String json = "[1, 2, 3]";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        rd.read();
        assertEquals(null, rd.getArrayName());
    }

    @Test
    public void testGetArrayName() throws Exception {
        String json = "{\"arr\":[1, 2, 3]}";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        rd.read();
        assertEquals("arr", rd.getArrayName());
    }

    @Test
    public void testReadSimpleObjects() throws Exception {
        String json = "{\"arr\":[{\"a\":1}, {\"a\":2}, {\"a\":3}]}";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(1, n.get("a").asInt());

        n = rd.read();
        assertEquals(2, n.get("a").asInt());

        n = rd.read();
        assertEquals(3, n.get("a").asInt());

        n = rd.read();
        assertEquals(null, n);
    }

    /**
     * test it can read array that is the root, not inside an object
     * @throws Exception
     */
    @Test
    public void testReadDirectArrayObjects() throws Exception {
        String json = "[{\"a\":1}, {\"a\":2}, {\"a\":3}]";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(1, n.get("a").asInt());

        n = rd.read();
        assertEquals(2, n.get("a").asInt());

        n = rd.read();
        assertEquals(3, n.get("a").asInt());

        n = rd.read();
        assertEquals(null, n);
    }

    @Test
    public void testReadEmptyArray() throws Exception {
        String json = "[]";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(null, n);
    }

    @Test
    public void testReadNoArray() throws Exception {
        String json = "{\"a\":1, \"b\":2}";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(null, n);
    }

    /**
     * test the reader only reads the first array it finds
     * @throws Exception
     */
    @Test
    public void testReadSkipSecondArray() throws Exception {
        String json = "{\"arr\":[{\"a\":1}, {\"a\":2}, {\"a\":3}], \"arr2\":[{\"a\":1}, {\"a\":2}, {\"a\":3}]}";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(1, n.get("a").asInt());

        n = rd.read();
        assertEquals(2, n.get("a").asInt());

        n = rd.read();
        assertEquals(3, n.get("a").asInt());

        n = rd.read();
        assertEquals(null, n);
    }

    @Test
    public void testReadNonObjects() throws Exception {
        String json = "{\"arr\":[{\"a\":1}, \"random string\", {\"a\":2}]}";
        saveToFile(json);

        JsonArrayReader rd = new JsonArrayReader(getDefaultFileName());
        JsonNode n;

        n = rd.read();
        assertEquals(1, n.get("a").asInt());

        n = rd.read();
        assertEquals("random string", n.asText());

        n = rd.read();
        assertEquals(2, n.get("a").asInt());

        n = rd.read();
        assertEquals(null, n);
    }

    @Test(expected = FileNotFoundException.class)
    public void testInvalidFileName() throws Exception {
        JsonArrayReader rd = new JsonArrayReader("no such file");
    }
}
