import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Robo
 */
enum ImportArgument {
    FIELD_NAMES("f"),
    SERVER("s"),
    DATABASE("d"),
    TABLE("T"),
    USER("u"),
    PASSWORD("p"),
    FILE_NAME("i"),
    PORT("P"),
    INSTANCE("I"),
    BLOCK_SIZE("B"),
    THREAD_COUNT("N");

    private final String opt;

    ImportArgument(String opt) {
        this.opt = opt;
    }

    String getOpt() {
        return opt;
    }
}

/**
 * A tool to parse an arbitrarily large JSON file into a Microsoft SQL Server database.
 * <p>
 * The tool will verify the fields specified exists in the database table, then processes the file concurrently by
 * creating blocks of JSON objects then passing them to a thread executor.
 * <p>
 * The tool uses its 'best effort' to perform the import, i.e. it will:
 * <ul>
 * <li>Continue to read the JSON array if it encounters an non-object</li>
 * <li>Assign null value if a field that is missing in a JSON object</li>
 * <li>Assign default value if the JSON value type is not compatible with the database field</li>
 * <li>Continues if it encounters an exception when importing an object</li>
 * </ul>
 * The tool will import the first array it finds in the JSON file, i.e. the array can be nested inside objects or come
 * after other fields in the file. It will skip any nodes in the array that is not an object.
 * <p>
 * For example, if the import table's field type is correct, the tool will import the 'auctions' array in the
 * following text without errors:
 *
 * <pre>{@code
 * {
 * "realm":{"name":"Aegwynn","slug":"aegwynn"},
 * "alliance":{"auctions":[
 * {"auc":2073382230,"item":[1, 2, 3],"owner":"Keely"},
 * "random string",
 * {"auc":2073949971,"item":52364,"owner":"Facepwnded"},
 * {"auc":2073218804,"item":23107,"owner":"Tephelie"}]}
 * }
 * }</pre>
 *
 * Note when using SQL Server with named instances:
 * <p>
 * Microsoft SQL Server defaults to listen on port 1433, but a named instance will listen on a different port. This can
 * be found by looking at the SQL Server log file in SQL Server Management Studio > Management > SQL Server Logs.
 * Look for something like {@code Server is listening on [ 'any' <ipv4> 56866]}. In this case, 56866 is the port number.
 * <p>
 * Alternatively, one can start the SQL Server Browser service, which listens on port 1434. Connections to port 1434
 * will get redirected to the correct SQL Server port.
 * <p>
 * The tool uses the following third party packages:
 * <pre>
 * commons-cli-1.2.jar            Apache Commons CLI for parsing command line options.
 * jackson-core-2.2.0.jar         Jackson JSON Processor for parsing JSON file.
 * jackson-annotations-2.2.0.jar
 * jackson-databind-2.2.0
 * jtds-1.3.1.jar                 jTDS JDBC Driver for connecting to Microsoft SQL Server.
 * </pre>
 */
public class JsonImport {
    /**
     * Accepts parameters of the JSON file location, connection details to the database,
     * and other options then concurrently imports the data to the SQL database.
     *
     * <pre>{@code
     * usage: java -jar JsonImport.jar -f <fieldNames> -s <server> -d <database>
     * -i <file> [OPTIONS]
     * -B <blockSize>     number of JSON objects per thread. If empty, it
     *                    defaults to 500.
     * -d <database>      name of the database to import to.
     * -f <fieldNames>    comma separated list of column names to import. Values
     *                    are imported from JSON objects' matching field names.
     * -I <instance>      instance name of the SQL server.
     * -i <fileName>      file path of the JSON import file.
     * -N <threadCount>   number of threads to use. If empty, it defaults to
     *                    number of processors available.
     * -P <port>          port of the SQL server. If empty, it defaults to 1433.
     * -p <password>      password to log in to database.
     * -s <server>        server name of the SQL server. Do not include instance
     *                    name.
     * -T <table>         name of the table to import to. If empty, it defaults
     *                    to name of JSON array.
     * -u <user>          username to log in to database.
     * }</pre>
     *
     * For example, to import auctions.json to table Listings on SQL server instance SERVER1\SQLEXPRESS,
     * using 10 threads:
     * <p>
     * {@code java -jar JsonImport.jar -f "ListingId, Title, StartPrice" -s SERVER1 -I SQLEXPRESS -P 1434 -d Auction
     * -T Listings -u robo -p hunter13 -N 10 -i C:\Users\Robo\auctions.json}
     *
     * @param args command line arguments
     */
    public static void main(String args[]) {
        Options options = buildCommandLineOptions();
        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "java -jar JsonImport.jar -f <fieldNames> -s <server> -d <database> -i <file> [OPTIONS]", options);
            return;
        }
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String fieldNames = cmd.getOptionValue(ImportArgument.FIELD_NAMES.getOpt());
            String server = cmd.getOptionValue(ImportArgument.SERVER.getOpt());
            String database = cmd.getOptionValue(ImportArgument.DATABASE.getOpt());
            String table = cmd.getOptionValue(ImportArgument.TABLE.getOpt());
            String user = cmd.getOptionValue(ImportArgument.USER.getOpt());
            String password = cmd.getOptionValue(ImportArgument.PASSWORD.getOpt());
            String jsonFile = cmd.getOptionValue(ImportArgument.FILE_NAME.getOpt());
            int port = parseIntDef(cmd.getOptionValue(ImportArgument.PORT.getOpt()), 1433);
            String instance = cmd.getOptionValue(ImportArgument.INSTANCE.getOpt());
            int blockSize = parseIntDef(cmd.getOptionValue(ImportArgument.BLOCK_SIZE.getOpt()), 500);
            int threadCount = parseIntDef(cmd.getOptionValue(ImportArgument.THREAD_COUNT.getOpt()),
                    Runtime.getRuntime().availableProcessors());

            ConnectionProperties p = new ConnectionProperties(server, port, database, instance, user, password);

            long startTime = System.currentTimeMillis();
            Logger log = Logger.getLogger(ImportTask.class.getName());
            log.info("Started import.");

            JsonArrayReader rd = new JsonArrayReader(jsonFile);
            Collection<JsonNode> list = new LinkedList<JsonNode>();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            try {
                JsonNode node = rd.read();

                if (table == null || table.isEmpty()) {
                    table = rd.getArrayName();
                }

                String[] arrFields = fieldNames.split(",");
                List<Field> fields = new Vector<Field>();
                try {
                    //test table exists, and all fields exists in the database before we continue much further
                    //if the fields exists, get the SQL field type so we can assign null values
                    DatabaseConnection.verifyTable(p, table, arrFields, fields);

                    while (node != null) {
                        if (node.isObject()) {
                            list.add(node);
                        } else {
                            System.out.printf("Skipping %s. Node is not an object.%n", node);
                        }

                        if (list.size() == blockSize) {
                            //pass the list to Executor to process
                            ImportTask task = new ImportTask(p, table, fields, list);
                            executor.execute(task);
                            list = new LinkedList<JsonNode>();
                        }
                        node = rd.read();
                    }

                    if (list.size() > 0) {
                        //pass the list to Executor to process
                        ImportTask task = new ImportTask(p, table, fields, list);
                        executor.execute(task);
                    }
                } catch (SQLException e) {
                    //something went wrong when verifying table
                    System.err.println("An error occurred when importing to database: "+e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("An error occurred when connecting to database: " + e.getMessage());
                }
            } finally {
                rd.close();
                executor.shutdown();
            }

            try {
                //wait for all the tasks to finish
                while (true) {
                    if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.info(String.format("Finished import in %f sec.",
                                (System.currentTimeMillis() - startTime) / 1000f));
                        break;
                    }
                }
            } catch (InterruptedException e) {
                ;
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (JsonParseException e) {
            System.err.println("An error occurred when parsing the json import file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("An error occurred when reading the json import file: " + e.getMessage());
        }
    }

    /**
     * The same as Integer.parseInt, but returns default value instead of exception.
     */
    private static int parseIntDef(String value, int defValue) {
        if (value == null) {
            return defValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    /**
     * Builds the list of command line options available.
     */
    private static Options buildCommandLineOptions() {
        Option o;
        Options options = new Options();

        o = OptionBuilder.withArgName("fieldNames")
                .hasArg()
                .withDescription("comma separated list of column names to import. "
                + "Values are imported from json objects' matching field names.")
                .isRequired()
                .create(ImportArgument.FIELD_NAMES.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("server")
                .hasArg()
                .withDescription("server name of the SQL server. Do not include instance name.")
                .isRequired()
                .create(ImportArgument.SERVER.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("database")
                .hasArg()
                .withDescription("name of the database to import to.")
                .isRequired()
                .create(ImportArgument.DATABASE.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("table")
                .hasArg()
                .withDescription("name of the table to import to. If empty, it defaults to name of json array.")
                .create(ImportArgument.TABLE.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("user")
                .hasArg()
                .withDescription("username to log in to database.")
                .create(ImportArgument.USER.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("password")
                .hasArg()
                .withDescription("password to log in to database.")
                .create(ImportArgument.PASSWORD.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("fileName")
                .hasArg()
                .withDescription("file path of the json import file.")
                .isRequired()
                .create(ImportArgument.FILE_NAME.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("port of the SQL server. If empty, it defaults to 1433.")
                .create(ImportArgument.PORT.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("instance")
                .hasArg()
                .withDescription("instance name of the SQL server.")
                .create(ImportArgument.INSTANCE.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("blockSize")
                .hasArg()
                .withDescription("number of json objects per thread. If empty, it defaults to 500.")
                .create(ImportArgument.BLOCK_SIZE.getOpt());
        options.addOption(o);

        o = OptionBuilder.withArgName("threadCount")
                .hasArg()
                .withDescription("number of threads to use. If empty, it defaults to number of processors available.")
                .create(ImportArgument.THREAD_COUNT.getOpt());
        options.addOption(o);

        return options;
    }
}