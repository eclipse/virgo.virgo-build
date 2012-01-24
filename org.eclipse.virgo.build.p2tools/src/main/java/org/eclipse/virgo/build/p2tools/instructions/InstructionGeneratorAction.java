package org.eclipse.virgo.build.p2tools.instructions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This action generates autostart instructions for all feature.xmls contained in a feature directory structure. 
 * is means having a common directory that contains many feature directories with feature.xmls inside.
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 * Thread-safe.
 */
public class InstructionGeneratorAction {
    
    private static final String EOL = "\n";
    
    public void perform(File featuresLocation) {
        FeatureParser parser = new FeatureParser();
        for (File featureDir : featuresLocation.listFiles()) {
            if (featureDir.isDirectory()) {
                File featureXml = new File(featureDir, "feature.xml");
                try {
                    ArrayList<String> autoStartPlugins = parser.parse(featureXml);
                    generateInstructions(autoStartPlugins, featureDir);
                } catch (IOException e) {
                    System.err.println("Failed to generate autostart instructions for feature: " + featureXml.getAbsolutePath() + " Error was: " + e);
                }
            }
        }
    }
    
    /**
     * Generates p2.inf with autostart instructions for all passed plugin Ids and writes it in the passed feature directory
     * @param autoStartPlugins - the list of plugins to autostart
     * @param featureDir - the directory of the source feature
     * @throws IOException
     */
    private void generateInstructions(ArrayList<String> autoStartPlugins, File featureDir) throws IOException {
        if (autoStartPlugins.isEmpty()) {
            return;
        }
        File p2Inf = new File(featureDir, "p2.inf");
        FileWriter writer = new FileWriter(p2Inf);
        int counter = 0;
        for (String autoStartPlugin : autoStartPlugins) {
            writer.write("requires." + counter + ".name = tooling" + autoStartPlugin + EOL
                + "requires." + counter + ".namespace = org.eclipse.equinox.p2.iu" + EOL
                + "requires." + counter + ".optional = true" + EOL);
            counter++;
        }
        writer.write(EOL);
        counter = 0;
        for (String autoStartPlugin : autoStartPlugins) {
            writer.write("units." + counter + ".id = tooling" + autoStartPlugin + EOL
                + "units." + counter + ".version = 1.0.0" + EOL
                + "units." + counter + ".requires.0.name = " + autoStartPlugin + EOL
                + "units." + counter + ".requires.0.namespace = osgi.bundle" + EOL
                + "units." + counter + ".requires.1.name = bundle" + EOL
                + "units." + counter + ".requires.1.namespace = org.eclipse.equinox.p2.eclipse.type" + EOL
                + "units." + counter + ".requires.1.greedy = false" + EOL
                + "units." + counter + ".requires.1.range = 0.0.0" + EOL
                + "units." + counter + ".hostRequirements.0.name = " + autoStartPlugin + EOL
                + "units." + counter + ".hostRequirements.0.namespace = osgi.bundle" + EOL
                + "units." + counter + ".hostRequirements.1.name = bundle" + EOL
                + "units." + counter + ".hostRequirements.1.namespace = org.eclipse.equinox.p2.eclipse.type" + EOL
                + "units." + counter + ".hostRequirements.1.greedy = false" + EOL
                + "units." + counter + ".hostRequirements.1.range = 0.0.0" + EOL
                + "units." + counter + ".properties.0.name = org.eclipse.equinox.p2.type.fragment" + EOL
                + "units." + counter + ".properties.0.value = true" + EOL
                + "units." + counter + ".provides.0.name = tooling" + autoStartPlugin + EOL
                + "units." + counter + ".provides.0.namespace = org.eclipse.equinox.p2.iu" + EOL
                + "units." + counter + ".provides.1.name = tooling" + EOL
                + "units." + counter + ".provides.1.namespace = org.eclipse.equinox.p2.flavor" + EOL
                + "units." + counter + ".provides.1.version = 1.0.0" + EOL
                + "units." + counter + ".touchpoint.id = org.eclipse.equinox.p2.osgi" + EOL
                + "units." + counter + ".touchpoint.version = 1.0.0" + EOL
                + "units." + counter + ".instructions.install = installBundle(bundle:${artifact})" + EOL
                + "units." + counter + ".instructions.uninstall = uninstallBundle(bundle:${artifact})" + EOL
                + "units." + counter + ".instructions.configure = markStarted(started: true);" + EOL
                + "units." + counter + ".instructions.unconfigure = markStarted(started: false);" + EOL + EOL);
            counter++;
        }
        writer.flush();
        writer.close();
    }

}
