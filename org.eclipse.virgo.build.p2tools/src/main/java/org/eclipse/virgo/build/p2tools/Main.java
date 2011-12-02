package org.eclipse.virgo.build.p2tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * This class is used to generate a valid bundles.info file on top of a directory with bundles
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 * Not thread safe.
 */

public class Main {

    protected static final String SIMPLECONFIGURATOR_BSN = "org.eclipse.equinox.simpleconfigurator";

    private static final String EQUINOX_BSN = "org.eclipse.osgi";

    private static final String BSN_VERSION_SEPARATOR = "_";

    private static final String SC_CONFIG_DIR = "configuration" + File.separator + SIMPLECONFIGURATOR_BSN;

    private static final String BINFO_FILENAME = "bundles.info";

    public static void main(String[] args) throws IOException {
        if (args != null && args.length == 1) {
            String baseDirLocation = args[0];
            File baseDir = new File(baseDirLocation);
            String bInfo = generateBInfo(getSourcePlugins(baseDir));
            writeBInfo(getTargetDir(baseDir), bInfo);
        } else {
            throw new IllegalArgumentException("Required argument for build-kernel's location is missing or wrong.");
        }
    }

    static File getSourcePlugins(File baseDir) {
        return new File(baseDir, "plugins");
    }

    static File getTargetDir(File baseDir) {
        return new File(baseDir, SC_CONFIG_DIR);
    }
    
    static String generateBInfo(File sourceFile) {
        StringBuilder bundlesInfoContent = new StringBuilder();
        if (sourceFile.isDirectory()) {
            for (File file : sourceFile.listFiles()) {
                if (file.getName().endsWith(".jar") && file.getName().contains(BSN_VERSION_SEPARATOR)) {
                    String[] bsnVersionPair = file.getName().split(BSN_VERSION_SEPARATOR);
                    String name = bsnVersionPair[0];
                    String version = bsnVersionPair[1];
                    version = version.substring(0, version.indexOf(".jar"));
                    int startLevel = 4;
                    if (name.equals(SIMPLECONFIGURATOR_BSN)) {
                        startLevel = 1;
                    }
                    if (name.equals(EQUINOX_BSN)) {
                        startLevel = -1;
                    }
                    bundlesInfoContent = bundlesInfoContent.append(name).append(",").append(version).append(",").append("plugins/" + file.getName()).append(",").append(startLevel).append(",").append("true\n");
                }
            }
        }  
        return bundlesInfoContent.toString();
    }

    static void writeBInfo(File targetDir, String bundlesInfoContent) throws IOException {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Could not create the directory structure for path " + targetDir.getAbsolutePath());
        }
        File bundlesInfo = new File(targetDir, BINFO_FILENAME);
        if (!bundlesInfo.exists() && !bundlesInfo.createNewFile()) {
            throw new IOException("Could not create the bundle info file at " + bundlesInfo.getAbsolutePath());
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(bundlesInfo);
            writer.write(bundlesInfoContent);
        } catch (IOException e) {
            throw e;
        } finally {
            writer.flush();
            writer.close();
        }
    }

}
