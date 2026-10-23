package com.buildplanner;

import java.util.Map;

/** Bundled, factual build recipe. Existing furniture is a dependency, never a bank item. */
public final class Recipe
{
    public String id;
    public String name;
    public String category;
    public int construction;
    public int sailing;
    public String boat;
    public Map<String, Long> materials;
    public String previous;
    public String source;
    public String checked;
    public String notes;
    public String description;
    public String stats;
    public Map<String, Integer> capacities;
    public int iconItemId;
    public String tools;
    public String facility;
    public Map<String, Integer> otherSkills;
    public String iconStatus;
    public int sourceRow;

    public String label()
    {
        return name + ("Any".equals(boat) ? "" : " [" + boat + "]");
    }

    @Override public String toString() { return label(); }
}
