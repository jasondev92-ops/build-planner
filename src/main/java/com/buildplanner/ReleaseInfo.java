package com.buildplanner;

import java.io.InputStream;
import java.util.Properties;

/** Bundled release metadata; never fetches anything from the network. */
final class ReleaseInfo
{
    private static final Properties VALUES = load();
    static String version() { return VALUES.getProperty("version", "Development build"); }
    static String feedbackUrl() { return VALUES.getProperty("feedbackUrl", "").strip(); }
    static boolean isNewFor(String lastSeen) { return !version().equals(lastSeen); }
    static String changes()
    {
        try (InputStream in = ReleaseInfo.class.getResourceAsStream("/com/buildplanner/changes.txt")) {
            if (in == null) return "Release notes unavailable.";
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot load release notes", e); }
    }
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
