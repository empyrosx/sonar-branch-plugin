package sonarqube;

import sonarqube.server.BranchFeatureExtensionImpl;
import sonarqube.server.BranchSupportDelegateImpl;
import org.sonar.api.Plugin;

/**
 * Implements branch plugin.
 */
public class BranchPlugin implements Plugin {

    public void define(Context context) {
        context.addExtensions(BranchFeatureExtensionImpl.class, BranchSupportDelegateImpl.class);
    }
}
