package com.buildplanner;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class Catalogue
{
    private final Map<String, Recipe> byId = new LinkedHashMap<>();

    public Catalogue(Recipe[] recipes)
    {
        if (recipes == null || recipes.length == 0) throw new IllegalArgumentException("Empty catalogue");
        for (Recipe r : recipes)
        {
            if (r == null || r.id == null || r.name == null || r.name.isBlank()
                || !Arrays.asList("House", "Construction", "Sailing").contains(r.category)
                || !Arrays.asList("Any", "Raft", "Skiff", "Sloop").contains(r.boat)
                || r.construction < 1 || r.construction > 99 || r.sailing < 0 || r.sailing > 99
                || r.materials == null || r.materials.isEmpty() || r.checked == null
                || r.source == null || !r.source.startsWith("https://oldschool.runescape.wiki/w/"))
                throw new IllegalArgumentException("Invalid recipe");
            for (Map.Entry<String, Long> m : r.materials.entrySet())
                if (m.getKey() == null || m.getKey().isBlank() || m.getValue() == null || m.getValue() <= 0)
                    throw new IllegalArgumentException("Invalid materials: " + r.id);
            if (byId.putIfAbsent(r.id, r) != null) throw new IllegalArgumentException("Duplicate recipe: " + r.id);
        }
        for (Recipe r : recipes) chain(r.id); // Detect missing dependencies and cycles before displaying anything.
    }

    public static Catalogue load(Gson gson)
    {
        InputStream in = Catalogue.class.getResourceAsStream("/com/buildplanner/recipes.json");
        if (in == null) throw new IllegalStateException("Bundled recipes missing");
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            return new Catalogue(gson.fromJson(reader, Recipe[].class));
        }
        catch (java.io.IOException e) { throw new IllegalStateException("Cannot read recipes", e); }
    }

    public Recipe get(String id)
    {
        Recipe r = byId.get(id);
        if (r == null) throw new IllegalArgumentException("Unknown recipe: " + id);
        return r;
    }

    public List<Recipe> chain(String id)
    {
        List<Recipe> out = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        while (id != null)
        {
            if (!visited.add(id)) throw new IllegalArgumentException("Cyclic prerequisite: " + id);
            Recipe r = get(id);
            out.add(r);
            id = r.previous;
        }
        Collections.reverse(out);
        return out;
    }

    public List<Recipe> search(String query, String category, String boat)
    {
        String[] words = query.strip().toLowerCase(Locale.ROOT).split("\\s+");
        return byId.values().stream()
            .filter(r -> "All".equals(category) || r.category.equals(category) || ("Construction".equals(category) && "House".equals(r.category)))
            .filter(r -> "All boats".equals(boat) || r.boat.equals("Any") || r.boat.equals(boat))
            .filter(r -> {
                String hay = (r.label() + " " + r.category + " " + String.join(" ", r.materials.keySet())).toLowerCase(Locale.ROOT);
                return Arrays.stream(words).allMatch(hay::contains);
            })
            .sorted(Comparator.comparing(Recipe::label)).collect(Collectors.toList());
    }

    public int size() { return byId.size(); }
}
