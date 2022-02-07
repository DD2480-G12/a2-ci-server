package com.group12.ciserver;

import com.group12.ciserver.database.DatabaseWrapper;
import com.group12.ciserver.model.BuildInfo;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import java.io.File;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
class DatabaseWrapperTests {

    // use separate db for testing
    private static String test_database = "Testdatabase.db";
    private static DatabaseWrapper db;

    @BeforeAll
    static void setUp() {
        // Create test database
        db = new DatabaseWrapper(test_database);
    }

    @AfterAll
    static void tearDown() {
        // Close connection & remove test database
        db.closeConnection();
        boolean result = new File(test_database).delete();
    }

    @Test
    void testAddBuild() {
        BuildInfo b = new BuildInfo(0, "commithash", "content", OffsetDateTime.now());
        long uid = db.addBuild(b);

        assertTrue(db.getBuildInfo(uid) != null);
    }

    @Test
    void testRemoveBuild() {
        BuildInfo b = new BuildInfo(0, "commithash", "content", OffsetDateTime.now());
        long uid = db.addBuild(b);

        assertTrue(db.removeBuild(uid));
        assertTrue(db.getBuildInfo(uid) == null);
    }

    @Test
    void testEditBuildContent() {
        BuildInfo b = new BuildInfo(0, "commithash", "content", OffsetDateTime.now());
        long uid = db.addBuild(b);

        String newcontent = "new content";

        assertTrue(db.editBuildContent(uid, newcontent));
        assertEquals(db.getBuildInfo(uid).getContent(), newcontent);
    }

}
