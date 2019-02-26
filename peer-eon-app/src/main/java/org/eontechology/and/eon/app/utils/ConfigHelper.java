package org.eontechology.and.eon.app.utils;

import java.io.File;
import java.io.IOException;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Spring config helper.
 */
public abstract class ConfigHelper {

    /**
     * Converts content of last existing file in files to string array
     *
     * @throws IOException
     */
    public static String[] getStringsFromLastExists(String delimiter, File... files) throws IOException {
        String[] ret = new String[0];
        for (File f : files) {
            if (!f.exists()) {
                continue;
            }

            String str = new String(FileCopyUtils.copyToByteArray(f), "UTF-8");
            ret = StringUtils.tokenizeToStringArray(str, delimiter);
        }
        return ret;
    }

    public static Boolean verifyNetwork(String network) {
        if (network == null || network.length() == 0) {
            System.out.println("ERROR! EON_NETWORK environment variable not specified (dev/test3/main)");
            System.exit(1);
        }
        return true;
    }
}
