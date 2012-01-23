package org.eclipse.virgo.build.p2tools.instructions;

import java.io.File;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class P2InstructionGeneratorApplication implements IApplication {

    private File source;

    @Override
    public Object start(IApplicationContext context) throws Exception {
        return run((String[]) context.getArguments().get("application.args"));
    }
    
    public Object run(String[] args) throws Exception {
        processCommandLineArguments(args);
        InstructionGeneratorAction generator = new InstructionGeneratorAction();
        System.out.println("Generating instructions ..");
        generator.perform(this.source);
        System.out.println("Generation completed with success.");
        return IApplication.EXIT_OK;
    }
    
    private void processCommandLineArguments(String[] args) throws Exception {
        if (args == null || args.length <= 0 || args.length > 2) {
            System.err.println("This application requires a single argument '-source' with a valid file-system value. Incorrect arguments: " + args);
        }
        String arg = args[0];
        String parameter = args[1];
        if (arg.equalsIgnoreCase("-source")) {
            if (!new File(parameter).exists())
                throw new IllegalArgumentException("Source location " + parameter + " must be a valid file-system path.");
            source = new File(parameter);
        }
    }

    @Override
    public void stop() {
        // nothing to do 
    }

}
