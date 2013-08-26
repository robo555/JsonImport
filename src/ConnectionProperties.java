/**
 * Created with IntelliJ IDEA.
 * User: Robo
 *
 * Immutable object for storing connection parameters to a SQL Server database.
 * <p>
 * When this object is shared for concurrent use, no synchronization is necessary.
 */
final class ConnectionProperties {
    public final String server;
    public final String database;
    public final String instance;
    public final String user;
    public final String password;
    public final int port;

    /**
     * Creates a new immutable {@code ConnectionProperties} containing connection parameters to a SQL Server database.
     * @param server server name of the SQL server. Do not include instance name.
     * @param port  port of the SQL server. If empty, it defaults to 1433.
     * @param database name of the database.
     * @param instance instance name of the SQL server.
     * @param user username to log in to database.
     * @param password password to log in to database.
     */
    ConnectionProperties(String server, int port, String database, String instance, String user, String password) {
        //connection can still work if some of the values are blank, e.g. user/password
        //so default null values to empty string, and allow to continue
        this.server = (server == null) ? "" : server;
        this.database = (database == null) ? "" : database;
        this.instance = (instance == null) ? "" : instance;
        this.user = (user == null) ? "" : user;
        this.password = (password == null) ? "" : password;
        this.port = (port == 0) ? 1433 : port;
    }
}
