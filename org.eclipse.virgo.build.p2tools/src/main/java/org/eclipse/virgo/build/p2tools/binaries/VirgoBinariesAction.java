package org.eclipse.virgo.build.p2tools.binaries;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Publishes a single binary artifact. Has to be executed for each new binary artifact that needs publishing
 * <strong>Concurrent Semantics</strong><br />
 * Thread-safe.
 */
public class VirgoBinariesAction extends AbstractPublisherAction {

    private static final String NATIVE_TOUCHPOINT = "org.eclipse.equinox.p2.native";
    private static final Version DEFAULT_VERSION = Version.createOSGi(1, 0, 0);

    private final File sourceLocation;

    private final Object monitor = new Object();

    private final Map<String, String> args;

    public VirgoBinariesAction(File sourceLocation, Map<String, String> args) {
        this.sourceLocation = sourceLocation;
        this.args = args;
    }
    
    public VirgoBinariesAction(File sourceLocation) {
        this(sourceLocation, null);
    }

    /**
     * Executes the action, resulting in a published artifact and metadata for it. 
     * If publishing of several binaries is required the action has to be executed several times for each binary/source location.
     * 
     * @param publisherInfo - initialized {@link IPublisherInfo} with repositories to be used by this
     *        {@link IPublisherAction}
     * @param results - {@link IPublisherResult} that will be passed on the next publishing stages
     * @param monitor - {@link IProgressMonitor} used for monitoring the progress of this action, can be <b>null</b>
     * @return - the {@link IStatus} containing the result of the operation
     */
    @Override
    public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
        if (this.sourceLocation == null) {
            throw new IllegalStateException(Messages.exception_noBundlesOrLocations);
        }
        synchronized (this.monitor) {
            setPublisherInfo(publisherInfo);
            try {
            	File binary = getFirstBinaryFileFrom(this.sourceLocation);
                if (binary != null) {
                    publishBinaryIU(binary, publisherInfo, results, monitor);
                }
            } catch (OperationCanceledException e) {
                return Status.CANCEL_STATUS;
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Takes the first binary file from the source location and publishes metadata for it.
     * In case several binaries need to be published there has to be the same number of sources
     * @param source
     * @return
     */
    private File getFirstBinaryFileFrom(File source) {
        File[] binaries = source.listFiles();
        if (binaries == null || binaries.length == 0) {
            return null;
        }
        return binaries[0];
	}

    private void publishBinaryIU(File binary, IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
        InstallableUnitDescription iuDescription = createBinaryIUDescriptionShell(binary.getName());
        addArtifactToIUDescription(binary, publisherInfo, iuDescription);
        setTouchpointInstructionsToIUDescription(iuDescription);
        results.addIU(MetadataFactory.createInstallableUnit(iuDescription), IPublisherResult.ROOT);
    }

    private void setTouchpointInstructionsToIUDescription(InstallableUnitDescription iuDescription) {
        String chmodTouchpointData = getCHMODConfiguration();
        Map<String, String> touchpointData = new HashMap<String, String>();
        touchpointData.put("install", "unzip(source:@artifact, target:${installFolder}/);" + chmodTouchpointData);
        touchpointData.put("uninstall", "cleanupzip(source:@artifact, target:${installFolder}/);");
        iuDescription.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
    }

    private String getCHMODConfiguration() {
        if (args != null && !args.isEmpty()) {
            StringBuilder chmodTouchpointData = new StringBuilder();
            for (String location : args.keySet()) {
                String[] targetDetails = location.split("@");
                String targetFile = targetDetails[0];
                String targetDir = targetDetails[1];
                String permission = args.get(location);
                chmodTouchpointData.append("chmod(targetDir:${installFolder}" + targetDir + ",targetFile:" + targetFile + ",permissions:" + permission + ");");
            }
            return chmodTouchpointData.toString();
        }
        return "";
    }

    private void addArtifactToIUDescription(File binary, IPublisherInfo publisherInfo, InstallableUnitDescription iuDescription) {
        List<IArtifactKey> binaryArtifacts = new ArrayList<IArtifactKey>();

        IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, binary.getName(), DEFAULT_VERSION);
        IArtifactDescriptor binaryDescriptor = PublisherHelper.createArtifactDescriptor(publisherInfo, key, binary);
        publishArtifact(binaryDescriptor, binary, publisherInfo);
        binaryArtifacts.add(key);

        iuDescription.setArtifacts(binaryArtifacts.toArray(new IArtifactKey[binaryArtifacts.size()]));
    }

    private InstallableUnitDescription createBinaryIUDescriptionShell(String iuId) {
        InstallableUnitDescription iuDescription = new MetadataFactory.InstallableUnitDescription();
        iuDescription.setId(iuId);
        iuDescription.setVersion(DEFAULT_VERSION);

        ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<IProvidedCapability>();
        IProvidedCapability p2IUCapability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iuDescription.getId(),
            DEFAULT_VERSION);
        providedCapabilities.add(p2IUCapability);
        iuDescription.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
        iuDescription.setTouchpointType(MetadataFactory.createTouchpointType(NATIVE_TOUCHPOINT, DEFAULT_VERSION));
        return iuDescription;
    }
}
