package com.group12.ciserver.database;

import com.group12.ciserver.model.BuildInfo;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.sql.*;
import java.util.ArrayList;

@Component
public class DatabaseWrapper {
    private Connection connection;
    private boolean isConnected = false;
    public DatabaseWrapper()
    {
        this("ci-server.db");
    }

    /**
     * Create a database wrapper given a path to a database. The database is created if the path does not exist.
     * Also, a connection to the database is established.
     * @param dbPath path to the database file
     */
    public DatabaseWrapper(String dbPath) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createBuildTableIfNotExists();
            isConnected = true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Close the connection to the database. Required to move or delete the database file.
     */
    public void closeConnection() {
        try {
            connection.close();
            isConnected = false;
        } catch(java.sql.SQLException e) {
            System.err.println("Closing database connection failed: " + e.getMessage());
        }

    }

    private void createBuildTableIfNotExists() {
        String existCheck = "SELECT count(name) FROM sqlite_master WHERE type='table' AND name='builds'";
        String sql = """
                CREATE TABLE builds (
                        uid INTEGER PRIMARY KEY,
                        commit_hash TEXT,
                        content TEXT,
                        timestamp TEXT
                );
                """;

        try {
            Statement statement1 = connection.createStatement();
            ResultSet rs = statement1.executeQuery(existCheck);
            if (rs.next())
            {
                int exists = rs.getInt(1);
                if (exists == 0)
                {
                    System.err.println("Added builds table as it did not exist");
                    Statement statement2 = connection.createStatement();
                    statement2.execute(sql);
                }
            }

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Add a build to the database.
     * @param b BuildInfo object to be stored in the database. The object's uid property is ignored.
     * @return unique identifier assigned by the database to the build if successful, otherwise -1
     */
    public long addBuild(BuildInfo b) {

        String sql = "insert into builds(commit_hash,content,timestamp) values(?,?,?)";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, b.getCommitId());
            pstmt.setString(2, b.getContent());
            pstmt.setString(3, offsetDateTimeToString(b.getTimestamp()));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 1) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next())
                {
                    return keys.getLong(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }

    /**
     * Remove a build from the database.
     * @param uid unique identifier of requested build
     * @return true if the database operation was successful, otherwise false
     */
    public boolean removeBuild(long uid) {
        String sql = "delete from builds where uid = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setLong(1, uid);

            int affectedRows = pstmt.executeUpdate();

            return affectedRows == 1;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Edit the content associated with a build.
     * @param uid unique identifier of requested build
     * @param content content string to replace previous content
     * @return true if the database operation was successful, otherwise false
     */
    public boolean editBuildContent(long uid, String content) {
        String sql = "update builds set content = ? where uid = ?";

        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, content);
            pstmt.setLong(2, uid);

            int affectedRows = pstmt.executeUpdate();

            return affectedRows == 1;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Get the information stored about a build.
     * @param uid unique identifier of requested build
     * @return BuildInfo object if build is found, else null
     */
    public BuildInfo getBuildInfo(long uid) {
        String sql = "select * from builds where uid = ?";

        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setLong(1, uid);
            ResultSet rs = pstmt.executeQuery();

            if(rs.next())
            {
                String commit_hash = rs.getString(2);
                String content = rs.getString(3);
                String tsString = rs.getString(4);
                OffsetDateTime ts = stringToOffsetDateTime(tsString);
                return new BuildInfo(uid, commit_hash, content, ts);
            }

            return null;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Get a list of all builds stored in the database.
     * @return ArrayList list of BuildInfo objects
     */
    public ArrayList<BuildInfo> getAllBuilds() {
        String sql = "select * from builds";
        ArrayList<BuildInfo> builds = new ArrayList<>();
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next())
            {
                long uid = rs.getLong(1);
                String commit_hash = rs.getString(2);
                String content = rs.getString(3);
                String tsString = rs.getString(4);
                OffsetDateTime ts = stringToOffsetDateTime(tsString);
                builds.add(new BuildInfo(uid, commit_hash, content, ts));
            }

            return builds;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static OffsetDateTime stringToOffsetDateTime(String s) {
        DateTimeFormatter format = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        TemporalAccessor t;
        try {
            t = format.parse(s);
        } catch (DateTimeParseException e) {
            System.err.println("Failed parsing timestamp: " + e.getMessage());
            return null;
        }
        return OffsetDateTime.from(t);
    }

    private static String offsetDateTimeToString(OffsetDateTime ts) {
        DateTimeFormatter format = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return format.format(ts);
    }
}
