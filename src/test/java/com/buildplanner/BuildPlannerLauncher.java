package com.buildplanner;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Official external-plugin development entry point; never automates login. */
public final class BuildPlannerLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BuildPlannerPlugin.class);
        RuneLite.main(args);
    }
}
