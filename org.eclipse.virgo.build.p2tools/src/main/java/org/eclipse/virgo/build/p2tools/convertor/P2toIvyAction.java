package org.eclipse.virgo.build.p2tools.convertor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This action generates build.xml file that when executed uploads mirrored p2 artifacts to build.eclipse.org's Ivy repositories.
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 * not thread-safe.
 */
public class P2toIvyAction {
    
    private static final String DEFAULT_ORG = "org.eclipse.virgo.mirrored";
	private static final String EOL = "\n";
    private static final String TAB = "    ";
    
    public void perform(File pluginsLocation, File targetIvyRepo, String offsetToVirgoBuild, File buildVersionsFile) throws IOException {
    	Set<String> processed = new HashSet<String>();
        for (File bundle : pluginsLocation.listFiles()) {
            //skip source files'
        	if (bundle.getName().contains(".source_") || !bundle.getName().endsWith(".jar")) {
            	continue;
            }
        	String nameVersion = bundle.getName().substring(0, bundle.getName().lastIndexOf("."));
			String[] pair = nameVersion.split("_");
            String bundleName = pair[0];
            String bundleVersion = pair[1];
            if (!processed.contains(nameVersion)) {
            	File bundleDir = createDirectory(targetIvyRepo, nameVersion);
            	File targetDir = createDirectory(bundleDir, "target");
            	File artifactsDir = createDirectory(targetDir, "artifacts");
            	File targetBundle = new File(artifactsDir, bundleName + ".jar");
            	copy(bundle, targetBundle);
            	String sourceBundleFileName = bundleName + ".source_" + bundleVersion + ".jar";
				File sourceBundle = new File(bundle.getParentFile(), sourceBundleFileName);
				boolean hasSource = false;
				if (sourceBundle.exists()) {
					File targetSourceBundle = new File(artifactsDir, bundleName + ".source.jar");
					copy(sourceBundle, targetSourceBundle);
					hasSource = true;
				}
            	generateDependenlessIvyXmlForP2Artifact(new File(bundleDir, "ivy.xml"), bundleName, bundleVersion, hasSource);
            	generateModuleBuildXml(new File(bundleDir, "build.xml"), nameVersion, offsetToVirgoBuild);
            	generateModuleBuildProperties(new File(bundleDir, "build.properties"), bundleName, bundleVersion);
                processed.add(nameVersion);
            }
        }
        generateOverallBuildXml(processed, targetIvyRepo, offsetToVirgoBuild);
        System.out.println("Successfully generated Ivy metadata for mirrored artifacts.");
        buildVersionsFile.delete();
        generateBuildVersionsFile(buildVersionsFile, processed);
        System.out.println("Successfully generated build.versions for mirrored artifacts.");
    }
    
    public void generateBuildVersionsFile(File buildVersions, Set<String> processed) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	for (String nameVersionPair : processed) {
    		builder.append(nameVersionPair.replaceAll("_", "=") + EOL);
    	}
    	writeToFile(buildVersions, builder.toString());
    }

	private void generateModuleBuildProperties(File buildProperties, String bundleName, String bundleVersion) throws IOException {
		String moduleBuildProperties = 
				"organisation=" + DEFAULT_ORG + EOL
				+ "module=" + bundleName + EOL
				+ "revision=" + bundleVersion + EOL
				+ "release.type=release" + EOL
				+ "ivy.cache.dir=${basedir}/../ivy-cache" + EOL
				+ "integration.repo.dir=${basedir}/../integration-repo" + EOL;
		writeToFile(buildProperties, moduleBuildProperties);		
	}

	private void generateModuleBuildXml(File buildXml, String nameVersion, String offsetToVirgoBuild) throws IOException {
		String moduleBuildXml = 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOL
				+ "<project name=\""+ nameVersion +"\">" + EOL
				+ TAB + "<property file=\"${basedir}/build.properties\"/>" + EOL
				+ TAB + "<import file=\"${basedir}/../" + offsetToVirgoBuild + "/standard/default.xml\"/>" + EOL
				+ "</project>";
		writeToFile(buildXml, moduleBuildXml);
	}

	private void generateOverallBuildXml(Set<String> processed, File targetIvyRepo, String offsetToVirgoBuild) throws IOException {
		File buildXml = new File(targetIvyRepo, "build.xml");
		String build_prefix = 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOL
				+ "<project name=\"build-virgo-build\" xmlns:ivy=\"antlib:org.apache.ivy.ant\">" + EOL
				+ TAB + "<path id=\"bundles\">" + EOL;
		String pathElements = "";
		for (String bundleDir : processed) {
			pathElements += TAB + TAB + "<pathelement location=\""+ bundleDir +"\"/>" + EOL;
		}
		String build_suffix = 
				TAB + "</path>" + EOL
				+ TAB + "<import file=\"${basedir}/"+ offsetToVirgoBuild +"/multi-bundle/default.xml\"/>" + EOL
				+ "</project>";
		writeToFile(buildXml, build_prefix + pathElements + build_suffix);
		
	}

	private void writeToFile(File buildXml, String stringToWrite) throws IOException {
		FileWriter writer = null;
        try {
            writer = new FileWriter(buildXml);
            writer.write(stringToWrite);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
	}

	private File createDirectory(File parentDir, String dirToCreate) throws IOException {
		File createdDir = new File(parentDir, dirToCreate);
		if(!createdDir.mkdir()) {
			throw new IOException("Failed to create directory " + createdDir.getAbsolutePath());
		}
		return createdDir;
	}
    
    /**
     * Generates ivy.xml without respect to any dependencies of the currently processed artifact
     * @param ivyXml - the location where to generate the ivy.xml file
     * @param moduleName - the name of the currently processed module
     * @param version - the version of the currently processed module
     * @throws IOException
     */
    private void generateDependenlessIvyXmlForP2Artifact(File ivyXml, String moduleName, String version, boolean hasSource) throws IOException {
        String ivy = 
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOL
            + "<?xml-stylesheet type=\"text/xsl\" href=\"http://ivyrep.jayasoft.org/ivy-doc.xsl\"?>" + EOL
            + "<ivy-module xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://incubator.apache.org/ivy/schemas/ivy.xsd\" version=\"1.3\">" + EOL
            + TAB + "<info organisation=\"" + DEFAULT_ORG + "\" module=\""+ moduleName +"\" revision=\""+ version +"\"/>" + EOL 
            + TAB + "<configurations>" + EOL
            + TAB + TAB + "<conf name=\"compile\" visibility=\"public\" description=\"Compile dependencies\"/>" + EOL
            + TAB + TAB + "<conf name=\"optional\" visibility=\"public\" extends=\"compile\" description=\"Optional dependencies\"/>" + EOL
            + TAB + TAB + "<conf name=\"provided\" visibility=\"public\" description=\"Provided dependencies\"/>" + EOL
            + TAB + TAB + "<conf name=\"runtime\" visibility=\"public\" extends=\"compile\" description=\"Runtime dependencies\"/>" + EOL 
            + TAB + "</configurations>" + EOL
            + TAB + "<publications>" + EOL 
            + TAB + TAB + "<artifact name=\""+ moduleName + "\"/>" + EOL;
        if (hasSource) {
            ivy += TAB + TAB + "<artifact name=\""+ moduleName + ".source\" type=\"src\" ext=\"jar\"/>" + EOL;
        }
        ivy += TAB + "</publications>" + EOL 
            + "</ivy-module>";
        FileWriter writer = null;
        try {
            writer = new FileWriter(ivyXml);
            writer.write(ivy);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }
    
    private final int BUFFER_SIZE = 4096;
    
    private int copy(InputStream in, OutputStream out) throws IOException {
        try {
            int byteCount = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {}
            try {
                out.close();
            } catch (IOException ex) {}
        }
    }

    private int copy(File in, File out) throws IOException {
        return copy(new BufferedInputStream(new FileInputStream(in)), new BufferedOutputStream(new FileOutputStream(out)));
    }

}
