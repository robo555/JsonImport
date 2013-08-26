import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 *
 * Imports a collection of {@code JsonNode} to a database table. Import tasks are intended to be run concurrently,
 * each task containing its own collection of {@code JsonNode} to import.
 */
class ImportTask implements Runnable {
    private ConnectionProperties p;
    private String tableName;
    private List<Field> fields;
    private Collection<JsonNode> importNodes;
    private Logger log = Logger.getLogger(ImportTask.class.getName());

    /**
     * Creates a new {@code ImportTask}. {@code fields} and {@code importNodes} do NOT get defensive copy. The lists
     * and the objects inside should not be modified once they have been passed to {@code ImportTask}.
     * @param p connection properties the task will use to connect to the database.
     * @param tableName the table to import to.
     * @param fields list of fields to import. The field names must exist in the table.
     * @param importNodes collection of {@code JsonNode} to import from. Field names are used to retrieve the values
     *                    from each {@code JsonNode}. If a field name does not exist in a node, the field will be
     *                    imported as null, therefore the database table should set optional fields to allow null.
     */
    ImportTask(ConnectionProperties p, String tableName, List<Field> fields, Collection<JsonNode> importNodes) {
        this.p = p;
        this.tableName = tableName;
        this.fields = fields;
        this.importNodes = importNodes;
    }

    @Override
    public void run() {
        try {
            String threadName = Thread.currentThread().getName();
            long startTime = System.currentTimeMillis();
            log.info(String.format("Started task in thread %s.", threadName));

            JsonArrayImporter importer = new JsonArrayImporter(p);
            importer.doImport(tableName, fields, importNodes);

            log.info(String.format("Finished task in thread %s in %f sec.", threadName,
                    (System.currentTimeMillis() - startTime) / 1000f));

        } catch (SQLException e) {
            System.err.println("An error occurred when importing to database: "+e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("An error occurred when connecting to database: " + e.getMessage());
        }
    }
}
