package Database;

import DataAccessLayer.FileSystem.FileUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.Scanner;

public final class Database {

    private static final String PATH_TO_SQL_SCRIPT = System.getProperty("user.dir") + "/DocumentTermsDump.sql";

    private static final BasicDataSource dataSource = new BasicDataSource();

    public static Connection getConnection(Configuration config) throws SQLException {
        String dbtype=config.getString("DB_TYPE");
        switch (dbtype) {
            case "mysql":
                dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        }
        dataSource.setUrl("jdbc:"+dbtype+"://"+config.getString("DB_HOSTNAME")+"/"+config.getString("DB_NAME")+"?autoReconnect=true&useSSL=false&rewriteBatchedStatements=true");
        dataSource.setUsername(config.getString("DB_USER"));
        dataSource.setPassword(config.getString("DB_PASS"));

        return dataSource.getConnection();
    }

    private Database() {}

    public static boolean checkForTables(Configuration config) {
        String sqlCheck = "SHOW TABLES";
        try (Connection connection = Database.getConnection(config);
             PreparedStatement preparedStatement = connection.prepareStatement(sqlCheck);
             ResultSet res = preparedStatement.executeQuery()) {
                res.last();
                if (res.getRow() != 3) {
                    return false;
                }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean initDatabase(Configuration config) {
        try (Connection connection = Database.getConnection(config);
             Statement statement = connection.createStatement()) {
            Scanner s = new Scanner(FileUtil.getInputStreamFrom(PATH_TO_SQL_SCRIPT));
            s.useDelimiter("(;(\r)?\n)|(--\n)");
            while (s.hasNext())
            {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/"))
                {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0)
                {
                    statement.execute(line);
                }
            }
        } catch (SQLException | FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
