package com.github.empyrosx.sonarqube.ce;

import org.sonar.core.platform.EditionProvider;

import java.util.Optional;

public class BranchEditionProvider implements EditionProvider {

    @Override
    public Optional<Edition> get() {
        return Optional.of(Edition.DEVELOPER);
    }

}
