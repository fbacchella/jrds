package jrds;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FolderSaver {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    protected static String tmpFolder = null;

    @BeforeClass
    static public void saveTempFolder() throws Exception {
        tmpFolder = System.getProperty("java.io.tmpdir");
    }

    /**
     * Needed because it plays with tmpdir, TemporaryFolder get lost
     */
    @After
    public void restoreFolder() {
        System.setProperty("java.io.tmpdir", tmpFolder);
    }


}
