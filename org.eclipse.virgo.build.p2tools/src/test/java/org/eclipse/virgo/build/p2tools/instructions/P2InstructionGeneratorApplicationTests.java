package org.eclipse.virgo.build.p2tools.instructions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;


public class P2InstructionGeneratorApplicationTests {
    
    private static final String MANY_AUTO_START_EXPECTED_P2_INF = "manyAutoStart/expected_p2.inf";
    private static final String ONE_TO_START_EXPECTED_P2_INF = "oneAutoStart/expected_p2.inf";
    private static final String MANY_AUTO_START_P2_INF = "manyAutoStart/p2.inf";
    private static final String ONE_AUTO_START_P2_INF = "oneAutoStart/p2.inf";
    private static final String NOTHING_TO_START_P2_INF = "nothingToStart/p2.inf";
    private static final String IGNORED_P2_INF = "ignored/p2.inf";
    private static final String SRC_TEST_RESOURCES = "src/test/resources/features";
    private static final File testResources = new File(SRC_TEST_RESOURCES);
    
    @Test
    public void InstructionGenerationTest() throws IOException {
        P2InstructionGeneratorApplication appToTest = new P2InstructionGeneratorApplication();
        String[] testArgs = {"-source", testResources.getAbsolutePath()};
        try {
            appToTest.run(testArgs);
        } catch (Exception e) {
            fail("The application failed to run with: " + e);
        }
        
        File ignoredInf = new File(testResources, IGNORED_P2_INF);
        assertFalse("Unexpectedly generated p2.inf at " + SRC_TEST_RESOURCES + "/" + IGNORED_P2_INF, ignoredInf.exists());
        
        File nothingToStart = new File(testResources, NOTHING_TO_START_P2_INF);
        assertFalse("Unexpectedly generated p2.inf at " + SRC_TEST_RESOURCES + "/" + NOTHING_TO_START_P2_INF, nothingToStart.exists());
        
        File oneToStart = new File(testResources, ONE_AUTO_START_P2_INF);
        assertTrue("Failed to create generated p2.inf at " + SRC_TEST_RESOURCES + "/" + ONE_AUTO_START_P2_INF, oneToStart.exists());
        File expectedOneToStart = new File(testResources, ONE_TO_START_EXPECTED_P2_INF);
        assertTrue("Incorrectly generated instructions. The file " + oneToStart.getAbsolutePath() + " does not match the expected content from " + expectedOneToStart.getAbsoluteFile(), areFilesEqual(expectedOneToStart, oneToStart));
        
        File manyToStart = new File(testResources, MANY_AUTO_START_P2_INF);
        assertTrue("Failed to create generated p2.inf at " + SRC_TEST_RESOURCES + "/" + MANY_AUTO_START_P2_INF, manyToStart.exists());
        File expectedManyToStart = new File(testResources, MANY_AUTO_START_EXPECTED_P2_INF);
        assertTrue("Incorrectly generated instructions. The file " + oneToStart.getAbsolutePath() + " does not match the expected content from " + expectedManyToStart.getAbsoluteFile(), areFilesEqual(expectedManyToStart, manyToStart));
        
        oneToStart.delete();
        manyToStart.delete();
    }
    
    private boolean areFilesEqual(File file1, File file2) throws IOException {
        BufferedReader bfr1 = new BufferedReader(new FileReader(file1));
        BufferedReader bfr2 = new BufferedReader(new FileReader(file2));

        String z,y;
        String firstFile = "";
        String secondFile = "";
        while ((z = bfr1.readLine()) != null)
          firstFile += z;

        while ((y = bfr2.readLine()) != null)
          secondFile += y;
        
        return firstFile.equals(secondFile);
    }
}
