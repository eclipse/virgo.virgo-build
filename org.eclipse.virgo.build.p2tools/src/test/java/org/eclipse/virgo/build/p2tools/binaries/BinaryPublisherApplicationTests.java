package org.eclipse.virgo.build.p2tools.binaries;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.junit.Test;


public class BinaryPublisherApplicationTests {
    
    private static final String SRC_TEST_RESOURCES = "src/test/resources";
    private static final File testResources = new File(SRC_TEST_RESOURCES);
    
    @Test
    public void getTargetDirTest() {
        BinaryPublisherApplication appToTest = new BinaryPublisherApplication();
        Map<String, String> actual = null;
        Map<String, String> expected = new HashMap<String, String>();
        
        expected.put("script.sh@/", "755");
        expected.put("props.properties@/config", "600");
        String testDataFine = "script.sh@/#755,props.properties@/config#600";
        actual = appToTest.parseChmodValues(testDataFine);
        assertTrue("Incorrect parsing of test data.", expected.equals(actual));
        
        expected.clear();
        String testDataWrongPath = "script@#755";
        actual = appToTest.parseChmodValues(testDataWrongPath);
        assertTrue("The wrong test data wasn't ignored as expected.", expected.equals(actual));
        
        expected.clear();
        String reversedSeparators = "props#path@600";
        actual = appToTest.parseChmodValues(reversedSeparators);
        assertTrue("The wrong test data wasn't ignored as expected.", expected.equals(actual));
        
        expected.clear();
        String reversedSeparatorsWrongPath = "props#@600";
        actual = appToTest.parseChmodValues(reversedSeparatorsWrongPath);
        assertTrue("The wrong test data wasn't ignored as expected.", expected.equals(actual));
        
        expected.clear();
        expected.put("file@\\location", "permission");
        String windowsSlashPath = "file@\\location#permission";
        actual = appToTest.parseChmodValues(windowsSlashPath);
        assertTrue("Incorrect parsing of test data.", expected.equals(actual));
        
        expected.clear();
        expected.put("file@\\\\location", "permission");
        String doubleWindowsSlashPath = "file@\\\\location#permission";
        actual = appToTest.parseChmodValues(doubleWindowsSlashPath);
        assertTrue("Incorrect parsing of test data.", expected.equals(actual));
    }
    
    @Test
    public void createActionsSourceOnlyTest() throws URISyntaxException {
        BinaryPublisherApplication appToTest = new BinaryPublisherApplication();
        appToTest.processParameter("-source", testResources.getAbsolutePath(), new PublisherInfo());
        IPublisherAction[] actions = appToTest.createActions();
        assertTrue("Expected one but no actions were generated.", actions != null);
        assertEquals(1, actions.length);
        IPublisherAction virgoBinaryAction = actions[0];
        IPublisherResult actual = new PublisherResult();
        virgoBinaryAction.perform(new PublisherInfo(), actual, new NullProgressMonitor());
        
        IInstallableUnit iu = actual.getIU("scripts", Version.create("1.0.0"), "root");
        String installInstructions = "unzip(source:@artifact, target:${installFolder}/);";
        String uninstallInstructions = "cleanupzip(source:@artifact, target:${installFolder}/);";
        validateIU(iu, installInstructions, uninstallInstructions);
        
    }

    private void validateIU(IInstallableUnit iu, String installInstructions, String uninstallInstructions) {
        assertTrue("IU generation failed.", iu != null);
        assertEquals("The touchpoint type of the generated iu does not match.", "org.eclipse.equinox.p2.native", iu.getTouchpointType().getId());
        assertEquals("Wrong touchpoint version", "1.0.0", iu.getTouchpointType().getVersion().toString());
        assertTrue(iu.getTouchpointData().size() == 1);
        for (ITouchpointData data : iu.getTouchpointData()) {
            Map<String, ITouchpointInstruction> instructions = data.getInstructions();
            assertEquals("Wrong install instructions.", installInstructions, instructions.get("install").getBody());
            assertEquals("Wrong uninstall instructions", uninstallInstructions, instructions.get("uninstall").getBody());
        }
    }
    
    @Test
    public void createActionsSourceAndPermissionsTest() throws URISyntaxException {
        BinaryPublisherApplication appToTest = new BinaryPublisherApplication();
        PublisherInfo info = new PublisherInfo();
        appToTest.processParameter("-source", testResources.getAbsolutePath(), info);
        appToTest.processParameter("-chmod", "file@path#permission,file@\\wronglocation\\#600,file#location@755", info);
        IPublisherAction[] actions = appToTest.createActions();
        
        assertTrue("Expected one but no actions were generated.", actions != null);
        assertEquals(1, actions.length);
        IPublisherAction virgoBinaryAction = actions[0];
        IPublisherResult actual = new PublisherResult();
        virgoBinaryAction.perform(new PublisherInfo(), actual, new NullProgressMonitor());
        
        IInstallableUnit iu = actual.getIU("scripts", Version.create("1.0.0"), "root");
        String installInstructions = "unzip(source:@artifact, target:${installFolder}/);chmod(targetDir:${installFolder}\\wronglocation\\,targetFile:file,permissions:600);chmod(targetDir:${installFolder}path,targetFile:file,permissions:permission);";
        String uninstallInstructions = "cleanupzip(source:@artifact, target:${installFolder}/);";
        validateIU(iu, installInstructions, uninstallInstructions);
    }
}
