package com.datamaster.deploy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbInitApplication {

    public static void main(String[] args) throws Exception {
        Config config = Config.from(args);
        Class.forName("org.postgresql.Driver");

        try (Connection admin = connect(config.host, config.port, config.adminDatabase, config.adminUser, config.adminPassword)) {
            admin.setAutoCommit(true);
            ensureRole(admin, config.appUser, config.appPassword);
            ensureDatabase(admin, config.appDatabase, config.appUser);
            ensureDatabase(admin, config.dolphinSchedulerDatabase, config.appUser);
        }

        executeSqlFile(config, config.appDatabase, config.appSql);
        executeSqlFile(config, config.dolphinSchedulerDatabase, config.dolphinSchedulerSql);
    }

    private static Connection connect(String host, int port, String database, String user, String password) throws SQLException {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url, user, password);
    }

    private static void ensureRole(Connection connection, String user, String password) throws SQLException {
        if (exists(connection, "select 1 from pg_roles where rolname = '" + escapeLiteral(user) + "'")) {
            execute(connection, "alter role " + quoteIdentifier(user) + " with login password '" + escapeLiteral(password) + "'");
            return;
        }
        execute(connection, "create role " + quoteIdentifier(user) + " with login password '" + escapeLiteral(password) + "'");
    }

    private static void ensureDatabase(Connection connection, String database, String owner) throws SQLException {
        if (!exists(connection, "select 1 from pg_database where datname = '" + escapeLiteral(database) + "'")) {
            execute(connection, "create database " + quoteIdentifier(database) + " owner " + quoteIdentifier(owner) + " encoding 'UTF8'");
        }
        execute(connection, "alter database " + quoteIdentifier(database) + " owner to " + quoteIdentifier(owner));
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }

    private static void executeSqlFile(Config config, String database, Path sqlFile) throws Exception {
        if (sqlFile == null) {
            return;
        }
        if (!Files.exists(sqlFile)) {
            throw new IllegalArgumentException("SQL file does not exist: " + sqlFile);
        }

        String content = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        List<String> statements = SqlStatements.split(content);
        System.out.println("Initializing database " + database + " with " + sqlFile + " (" + statements.size() + " statements)");

        try (Connection connection = connect(config.host, config.port, database, config.appUser, config.appPassword)) {
            connection.setAutoCommit(true);
            int success = 0;
            for (String statement : statements) {
                String normalized = statement.trim();
                if (normalized.isEmpty()) {
                    continue;
                }
                try {
                    execute(connection, normalized);
                    success++;
                } catch (SQLException ex) {
                    throw new SQLException("Failed SQL in " + sqlFile + ": " + preview(normalized), ex);
                }
            }
            System.out.println("Initialized database " + database + ": " + success + " statements executed");
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String quoteIdentifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String escapeLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String preview(String sql) {
        String oneLine = sql.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() <= 240) {
            return oneLine;
        }
        return oneLine.substring(0, 240) + "...";
    }

    private static final class Config {
        private String host = "127.0.0.1";
        private int port = 5432;
        private String adminDatabase = "postgres";
        private String adminUser = "postgres";
        private String adminPassword = "postgres";
        private String appDatabase = "datamaster";
        private String dolphinSchedulerDatabase = "dolphinscheduler";
        private String appUser = "datamaster";
        private String appPassword = "datamaster";
        private Path appSql;
        private Path dolphinSchedulerSql;

        private static Config from(String[] args) {
            Map<String, String> values = parseArgs(args);
            Config config = new Config();
            config.host = values.getOrDefault("host", config.host);
            config.port = Integer.parseInt(values.getOrDefault("port", String.valueOf(config.port)));
            config.adminDatabase = values.getOrDefault("admin-db", config.adminDatabase);
            config.adminUser = values.getOrDefault("admin-user", config.adminUser);
            config.adminPassword = values.getOrDefault("admin-password", config.adminPassword);
            config.appDatabase = values.getOrDefault("app-db", config.appDatabase);
            config.dolphinSchedulerDatabase = values.getOrDefault("ds-db", config.dolphinSchedulerDatabase);
            config.appUser = values.getOrDefault("app-user", config.appUser);
            config.appPassword = values.getOrDefault("app-password", config.appPassword);
            config.appSql = pathOrNull(values.get("app-sql"));
            config.dolphinSchedulerSql = pathOrNull(values.get("ds-sql"));
            return config;
        }

        private static Map<String, String> parseArgs(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String arg : args) {
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsageAndExit();
                }
                if (!arg.startsWith("--") || !arg.contains("=")) {
                    throw new IllegalArgumentException("Invalid argument: " + arg + ". Use --name=value");
                }
                int index = arg.indexOf('=');
                values.put(arg.substring(2, index), arg.substring(index + 1));
            }
            return values;
        }

        private static Path pathOrNull(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Paths.get(value);
        }

        private static void printUsageAndExit() {
            System.out.println("Usage: java -jar datamaster-db-init.jar "
                    + "--host=127.0.0.1 --port=5432 --admin-user=postgres --admin-password=postgres "
                    + "--app-db=datamaster --ds-db=dolphinscheduler --app-user=datamaster --app-password=datamaster "
                    + "--app-sql=/data/datamaster/init-sql/datamaster.sql --ds-sql=/data/datamaster/init-sql/dolphinscheduler.sql");
            System.exit(0);
        }
    }

    private static final class SqlStatements {
        private static List<String> split(String sql) throws IOException {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean singleQuote = false;
            boolean doubleQuote = false;
            boolean lineComment = false;
            boolean blockComment = false;
            String dollarQuote = null;

            for (int i = 0; i < sql.length(); i++) {
                char ch = sql.charAt(i);
                char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

                if (lineComment) {
                    current.append(ch);
                    if (ch == '\n') {
                        lineComment = false;
                    }
                    continue;
                }

                if (blockComment) {
                    current.append(ch);
                    if (ch == '*' && next == '/') {
                        current.append(next);
                        i++;
                        blockComment = false;
                    }
                    continue;
                }

                if (dollarQuote != null) {
                    if (sql.startsWith(dollarQuote, i)) {
                        current.append(dollarQuote);
                        i += dollarQuote.length() - 1;
                        dollarQuote = null;
                    } else {
                        current.append(ch);
                    }
                    continue;
                }

                if (!singleQuote && !doubleQuote) {
                    if (ch == '-' && next == '-') {
                        current.append(ch).append(next);
                        i++;
                        lineComment = true;
                        continue;
                    }
                    if (ch == '/' && next == '*') {
                        current.append(ch).append(next);
                        i++;
                        blockComment = true;
                        continue;
                    }
                    String tag = dollarQuoteTag(sql, i);
                    if (tag != null) {
                        current.append(tag);
                        i += tag.length() - 1;
                        dollarQuote = tag;
                        continue;
                    }
                }

                if (!doubleQuote && ch == '\'') {
                    current.append(ch);
                    if (singleQuote && next == '\'') {
                        current.append(next);
                        i++;
                    } else {
                        singleQuote = !singleQuote;
                    }
                    continue;
                }

                if (!singleQuote && ch == '"') {
                    current.append(ch);
                    doubleQuote = !doubleQuote;
                    continue;
                }

                if (!singleQuote && !doubleQuote && ch == ';') {
                    addStatement(result, current);
                    current.setLength(0);
                    continue;
                }

                current.append(ch);
            }

            addStatement(result, current);
            return result;
        }

        private static String dollarQuoteTag(String sql, int start) {
            if (sql.charAt(start) != '$') {
                return null;
            }
            int end = sql.indexOf('$', start + 1);
            if (end < 0) {
                return null;
            }
            String tag = sql.substring(start, end + 1);
            for (int i = 1; i < tag.length() - 1; i++) {
                char ch = tag.charAt(i);
                if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
                    return null;
                }
            }
            return tag;
        }

        private static void addStatement(List<String> result, StringBuilder builder) {
            String statement = builder.toString().trim();
            if (!statement.isEmpty()) {
                result.add(statement);
            }
        }
    }
}
