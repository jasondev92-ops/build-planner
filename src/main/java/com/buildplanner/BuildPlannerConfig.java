package com.buildplanner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("buildplanner")
public interface BuildPlannerConfig extends Config
{
    @ConfigItem(keyName = "highlightBank", name = "Automatic bank highlighting",
        description = "Highlight bank materials for unfinished builds in the selected project.", position = 0)
    default boolean highlightBank() { return false; }
}
