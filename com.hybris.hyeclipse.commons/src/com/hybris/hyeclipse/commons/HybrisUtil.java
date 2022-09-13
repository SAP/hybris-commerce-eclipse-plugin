package com.hybris.hyeclipse.commons;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

public class HybrisUtil {

    private HybrisUtil() {
    }

    public static boolean isPlatformModuleRoot(@NonNull File file) {
        return new File(file, Constants.EXTENSIONS_XML).isFile();
    }

    public static boolean isHybrisModuleRoot(@NonNull final File file) {
        return new File(file, Constants.EXTENSION_INFO_XML).isFile();
    }

    public static boolean isAcceleratorAddOnModuleRoot(@NonNull final File file) {
        return new File(file, Constants.ACCELERATOR_ADDON_DIRECTORY).isDirectory();
    }
}
