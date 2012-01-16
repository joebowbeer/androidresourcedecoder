package com.joebowbeer.resourcedecoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MainTest {

    public static final String USES_PERMISSION_INTERNET =
            "uses-permission[android:name=android.permission.INTERNET]";

    public static final String USES_PERMISSION_VIBRATE =
            "uses-permission[android:name=android.permission.VIBRATE]";

    public MainTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class Main.
     */
    @Test
    public void testMain() throws IOException {
        File tmpFile = File.createTempFile("resources", ".arsc");
        tmpFile.deleteOnExit();
        File orig = getResourceFile("resources.arsc");
        Files.copy(orig.toPath(), tmpFile.toPath(), REPLACE_EXISTING);
        assertTrue(bytesEquals(orig, tmpFile));
        // assume checked is initially "true"
        // assume background is initially "#FF113377"
        String[] args = {
            "-file", tmpFile.getAbsolutePath(),
            "-dump",
            "-R", "R.bool.checked=false",
            "-R", "R.color.background=#FF000000"};
        Main.main(args);
        assertFalse(bytesEquals(orig, tmpFile));
        args[4] = "R.bool.checked=true";
        args[6] = "R.color.background=#ff113377";
        Main.main(args);
        assertTrue(bytesEquals(orig, tmpFile));
    }

    /**
     * Test that manifest is unchanged if given -X pattern is specified
     * but matching element is missing from AndroidManifest.xml.
     */
    private void testNotRemove(String pattern) throws IOException {
        File tmpFile = File.createTempFile("AndroidManifest", ".xml");
        tmpFile.deleteOnExit();
        File orig = getResourceFile("AndroidManifest.xml");
        Files.copy(orig.toPath(), tmpFile.toPath(), REPLACE_EXISTING);
        Main.main(new String[] {
            "-file", tmpFile.getAbsolutePath(),
            "-X", pattern
        });
        assertTrue(bytesEquals(orig, tmpFile));
    }

    /**
     * Test that manifest is changed if given -X pattern are specified
     * and matching element is present in AndroidManifest.xml, and
     * not changed again after the element is removed.
     */
    private void testRemove(String pattern) throws IOException {
        File tmpFile = File.createTempFile("AndroidManifest", ".xml");
        tmpFile.deleteOnExit();
        File orig = getResourceFile("AndroidManifest.xml");
        Files.copy(orig.toPath(), tmpFile.toPath(), REPLACE_EXISTING);
        String[] args = {
            "-file", tmpFile.getAbsolutePath(),
            "-X", pattern
        };
        Main.main(args);
        assertFalse(bytesEquals(orig, tmpFile));
        File tmpFile2 = File.createTempFile("AndroidManifest", ".xml");
        tmpFile2.deleteOnExit();
        Files.copy(tmpFile.toPath(), tmpFile2.toPath(), REPLACE_EXISTING);
        args[1] = tmpFile2.getAbsolutePath();
        Main.main(args);
        assertTrue(bytesEquals(tmpFile, tmpFile2));
        Main.main(new String[] {
            "-file", tmpFile2.getAbsolutePath(), "-dump"
        });
    }

    @Test
    public void testRemoveInternetPermission() throws IOException {
        testRemove(USES_PERMISSION_INTERNET);
    }

    @Test
    public void testNotRemoveVibratePermission() throws IOException {
        testNotRemove(USES_PERMISSION_VIBRATE);
    }

    private static File getResourceFile(String name) {
        return new File(ClassLoader.getSystemResource(name).getPath());
    }

    private static boolean bytesEquals(File expected, File actual) throws IOException {
        byte[] expectedBytes = Files.readAllBytes(expected.toPath());
        byte[] actualBytes = Files.readAllBytes(actual.toPath());
        return Arrays.equals(expectedBytes, actualBytes);
    }
}
