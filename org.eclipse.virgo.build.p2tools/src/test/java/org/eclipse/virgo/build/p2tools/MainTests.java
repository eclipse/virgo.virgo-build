package org.eclipse.virgo.build.p2tools;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.virgo.build.p2tools.Main;

public class MainTests {
    
    private static final String BInfo_E2E_LOCATION = "configuration" + File.separator + "org.eclipse.equinox.simpleconfigurator" + File.separator + "bundles.info";
    private static final String SRC_TEST_RESOURCES = "src/test/resources";
    private static final File testResources = new File(SRC_TEST_RESOURCES);
    private static final File testBInfo = new File(testResources, "bundles.info");
    private static final File e2eBInfo = new File(testResources, BInfo_E2E_LOCATION);
    
    @Before
    @After
    public void resetTestBInfo() {
        testBInfo.delete();
        e2eBInfo.delete();
        e2eBInfo.getParentFile().delete();
        e2eBInfo.getParentFile().getParentFile().delete();
    }
    
    @Test
    public void getTargetDirTest() {
        File fileMock = new File("test");
        File result = Main.getTargetDir(fileMock);
        String expected = "org.eclipse.equinox.simpleconfigurator";
        assertEquals("Wrong targetDir path. There's an error while creating the targetDir file.", expected, result.getName());
    }
    
    @Test
    public void getSourcePluginsTest() {
        File fileMock = new File("test");
        File result = Main.getSourcePlugins(fileMock);
        String expected = "plugins";
        assertEquals("Wrong sourceDir path. There's an error while creating the sourceDir file.", expected, result.getName());
    }
    
    @Test
    public void generateBInfoTest() {
        File testPlugins = new File(testResources, "plugins");
        String expected = "dummyFileA,1.0.0,plugins/dummyFileA_1.0.0.jar,4,true\n" +
        		"dummyFileB,1.0.0,plugins/dummyFileB_1.0.0.jar,4,true\n" +
        		"org.eclipse.equinox.simpleconfigurator,1.0.0,plugins/org.eclipse.equinox.simpleconfigurator_1.0.0.jar,1,true\n" +
        		"org.eclipse.osgi,3.7.1,plugins/org.eclipse.osgi_3.7.1.jar,-1,true\n";
        String result = Main.generateBInfo(testPlugins);
        assertEquals("Incorrectly generated bundles.info content", expected, result);
    }
    
    @Test
    public void writeBInfoTest() {
        String expected = "dummyFileA,1.0.0,plugins/dummyFileA_1.0.0.jar,4,true\n" +
                "dummyFileB,1.0.0,plugins/dummyFileB_1.0.0.jar,4,true\n" +
                "org.eclipse.equinox.simpleconfigurator,1.0.0,plugins/org.eclipse.equinox.simpleconfigurator_1.0.0.jar,1,true\n" +
                "org.eclipse.osgi,3.7.1,plugins/org.eclipse.osgi_3.7.1.jar,-1,true\n";
        try {
            Main.writeBInfo(testResources, expected);
            String actual = readFileAsString(testBInfo);
            assertEquals("Incorrect content was written in the bundles.info file.", expected, actual);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException while testing bundles.info file creation. " + e.getMessage());            
        }
    }
    
    @Test
    public void writeBInfoOverWriteTest() throws IOException {
        writeTestContentToBInfo("testContent");
        try {
            String bundlesInfoContent = "overwrittenContent";
            Main.writeBInfo(testResources, bundlesInfoContent);
            String resultContent = readFileAsString(testBInfo);
            assertEquals("The content of the bundles.info file isn't overwritten with every generation.", bundlesInfoContent, resultContent);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException while testing bundles.info file creation. " + e.getMessage());            
        }
    }
    
    @Test
    public void mainTest() throws IOException {
        String[] args = {SRC_TEST_RESOURCES};
        String expected = "dummyFileA,1.0.0,plugins/dummyFileA_1.0.0.jar,4,true\n" +
            "dummyFileB,1.0.0,plugins/dummyFileB_1.0.0.jar,4,true\n" +
            "org.eclipse.equinox.simpleconfigurator,1.0.0,plugins/org.eclipse.equinox.simpleconfigurator_1.0.0.jar,1,true\n" +
            "org.eclipse.osgi,3.7.1,plugins/org.eclipse.osgi_3.7.1.jar,-1,true\n";
        
        Main.main(args);
        String actual = readFileAsString(new File(testResources, BInfo_E2E_LOCATION));
        assertEquals("Incorrect content was written in the bundles.info file.", expected, actual);
    }
    
    @Test
    public void mainNegativeTest() throws IOException {
        String[] args_empty = {};
        String[] args_tooMuch = {SRC_TEST_RESOURCES, "dummyArg"};
        try {
            Main.main(args_empty);
            fail("When passing empty arguments to main, IllegalArgumentException is expected.");
        } catch (IllegalArgumentException iae) {}
        try {
            Main.main(args_tooMuch);
            fail("When passing more than one argument to main, IllegalArgumentException is expected.");
        } catch (IllegalArgumentException iae) {}
        
        assertTrue("Generation of bundles.info file must fail when illegal arguments are passed to main.", !e2eBInfo.exists());
    }

    private void writeTestContentToBInfo(String testData) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(testBInfo);
            writer.write(testData);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException while preparing testData. " + e.getMessage());
        } finally {
            writer.flush();
            writer.close();
        }
    }
    
    private static String readFileAsString(File filePath) throws java.io.IOException{
        BufferedInputStream f = null;
        try {
            byte[] buffer = new byte[(int) filePath.length()];
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
            return new String(buffer);
        } finally {
            f.close();
        }
    }

}
