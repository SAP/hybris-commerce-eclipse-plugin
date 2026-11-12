package com.hybris.yps.hyeclipse.wizards;

import java.io.File;

public record ImportJobData(boolean removeExistingProjects, boolean fixClasspath, boolean removeHybrisBuilder,
		boolean createWorkingSet, boolean useMultiThread, boolean skipJarScanning, File platformDir) {
}
