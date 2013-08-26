import java.io.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 *
 * Reads a JSON file, searches for the first array then returns nodes in the array.
 */
class JsonArrayReader {
    private enum ParserState {BEFORE_ARRAY, IN_ARRAY, AFTER_ARRAY}

    private JsonParser jp;
    private ParserState state = ParserState.BEFORE_ARRAY;
    private String arrayName = "";

    /**
     * Creates a new {@code JsonArrayReader} that reads the specified {@code fileName}.
     * @param fileName The JSON file containing array
     * @throws IOException
     */
    JsonArrayReader(String fileName) throws IOException {
        JsonFactory f = new MappingJsonFactory();
        jp = f.createParser(new File(fileName));
    }

    /**
     * Returns the next node in a JSON array. The first time this is called, it will locate the beginning of the first
     * array in the JSON file then return the first node. Subsequent calls will return the next node.
     * <p>
     * If it has reached the end of the array, or no array is found, it will return {@code null}.
     *
     * @return the next JSON node in the array.
     * @throws IOException
     */
    JsonNode read() throws IOException {
        if (state == ParserState.BEFORE_ARRAY) {
            moveToArray();
        }

        if (state == ParserState.IN_ARRAY) {
            if (jp.nextToken() != JsonToken.END_ARRAY) {
                return jp.readValueAsTree();
            } else {
                state = ParserState.AFTER_ARRAY;
            }
        }

        return null;
    }

    /**
     * Move parser cursor to beginning of the array.
     * @throws IOException
     */
    private void moveToArray() throws IOException {
        if (state == ParserState.BEFORE_ARRAY) {
            JsonToken token = jp.nextToken();
            while (token != null) {
                if (token == JsonToken.START_ARRAY) {
                    arrayName = jp.getCurrentName();
                    state = ParserState.IN_ARRAY;
                    break;
                }
                token = jp.nextToken();
            }
        }
    }

    /**
     * Close file handle. Should be called in a finally block after creating a {@code JsonArrayReader} object.
     * @throws IOException
     */
    void close() throws IOException {
        jp.close();
    }

    /**
     * Returns the name of the array found.
     * @return the name of the array found.
     */
    String getArrayName() {
        return arrayName;
    }
}
