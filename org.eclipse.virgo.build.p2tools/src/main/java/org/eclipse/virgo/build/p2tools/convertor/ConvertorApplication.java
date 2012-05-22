package org.eclipse.virgo.build.p2tools.convertor;

import java.io.File;
import java.util.Arrays;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * 
 * This is the IApplication implementation for publishing binary artifacts. This
 * application published only one artifact per source location. For each new
 * artifact and source location that needs publishing the application has to be
 * executed again. In case the source location is empty the application
 * publishes empty p2 repository at the repository location.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Not thread safe.
 */
public class ConvertorApplication implements IApplication {
    
	private File source;
	private File destination;
	private String offsetToVirgoBuild;
	private File buildVersions;
	
	@Override
	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args"));
	}

	private Object run(String[] args) throws Exception {
		processCommandLineArguments(args);
		P2toIvyAction generator = new P2toIvyAction();
        System.out.println("Generating Ivy metadata and publish instructions..");
        generator.perform(this.source, this.destination, this.offsetToVirgoBuild, this.buildVersions);
        System.out.println("Generation completed with success.");
        return IApplication.EXIT_OK;
	}
	
    private void processCommandLineArguments(String[] args) throws Exception {
        if (args == null || args.length <= 0 || args.length > 8) {
        	System.err.println("This application requires a two arguments '-source.plugins.dir', '-destination', '-offset.to.virgo.build' and '-build.versions.location' with a valid file-system value. Incorrect arguments: " + Arrays.toString(args));
            throw new IllegalArgumentException("This application requires a two arguments '-source.plugins.dir', '-destination', '-offset.to.virgo.build' and '-build.versions.location' with a valid file-system value. Incorrect arguments: " + Arrays.toString(args));
        }
        for (int i=0; i<8; i+=2) {
        	String arg = args[i];
        	String parameter = args[i+1];
        	if (arg.equalsIgnoreCase("-source.plugins.dir") || arg.equalsIgnoreCase("-destination") || arg.equalsIgnoreCase("-offset.to.virgo.build") || arg.equalsIgnoreCase("-build.versions.location")) {
                if (!arg.equalsIgnoreCase("-offset.to.virgo.build") && !arg.equalsIgnoreCase("-build.versions.location") && !new File(parameter).exists()) {
                	System.err.println("The "+ arg +" value " + parameter + " must be a valid file-system path.");
                    throw new IllegalArgumentException("The "+ arg +" value " + parameter + " must be a valid file-system path.");
                }
                if (arg.equalsIgnoreCase("-source.plugins.dir")) {
                	this.source = new File(parameter);
                }
                if (arg.equalsIgnoreCase("-destination")) {
                	this.destination = new File(parameter);
                }
                if (arg.equalsIgnoreCase("-offset.to.virgo.build")) {
                	this.offsetToVirgoBuild = parameter;
                }
                if (arg.equalsIgnoreCase("-build.versions.location")) {
                	this.buildVersions = new File(parameter);
                }
            }
        }
    }        

	@Override
	public void stop() {
		//nothing to do
	}

}
