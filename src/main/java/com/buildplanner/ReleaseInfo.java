package com.buildplanner;

import java.io.InputStream;
import java.util.Properties;

/** Bundled release metadata; never fetches anything from the network. */
final class ReleaseInfo
{
    private static final Properties VALUES = load();
    static String version() { return VALUES.getProperty("version", "Development build"); }
    static String feedbackUrl() { return VALUES.getProperty("feedbackUrl", "").strip(); }
    private static Properties load()
    {
        Properties values = new Properties();
        try (InputStream in = ReleaseInfo.class.getResourceAsStream("/com/buildplanner/release.properties"))
        {
            if (in != null) values.load(in);
        }
        catch (java.io.IOException e) { throw new IllegalStateException("Cannot load release metadata", e); }
        return values;
    }
    private ReleaseInfo() { }
}
