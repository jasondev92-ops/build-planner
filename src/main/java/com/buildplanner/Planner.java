package com.buildplanner;

import com.google.gson.Gson;
import java.util.*;

/** Pure planning logic, independently testable without the game client. All edits run on the Swing EDT. */
public final class Planner
{
    public static final int MAX_QUANTITY = 1_000_000;
    public static final long MAX_OWNED = 1_000_000_000_000L;

    public static final class Entry
    {
        public String recipeId;
        public int quantity;
        public boolean fromScratch;
        public boolean completed;
        Entry(String id, int count, boolean all) { recipeId = id; quantity = count; fromScratch = all; }
    }
    public static final class Project
    {
        public String id = UUID.randomUUID().toString();
        public String name;
        public boolean manualCounts;
        public List<Entry> entries = new ArrayList<>();
        public Map<String, Long> owned = new TreeMap<>();
        Project(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    public static final class State
    {
        public int version = 1;
        public String selected;
        public List<Project> projects = new ArrayList<>();
    }
    public static final class Material
    {
        public final String name;
        public final long required, owned, missing;
        Material(String name, long required, long owned)
        {
            this.name = name; this.required = required; this.owned = owned;
            this.missing = Math.max(0, required - owned);
        }
    }

    private final Catalogue catalogue;
    public final State state;
    public Planner(Catalogue catalogue, State state)
    {
        this.catalogue = catalogue; this.state = state;
        validate();
    }
    public static Planner fresh(Catalogue catalogue)
    {
        State s = new State(); Project p = new Project("My builds");
        s.projects.add(p); s.selected = p.id;
        return new Planner(catalogue, s);
    }
    public static Planner decode(Catalogue c, Gson g, String json)
    {
        if (json == null || json.isBlank()) return fresh(c);
        return new Planner(c, g.fromJson(json, State.class));
    }
    public String encode(Gson g) { return g.toJson(state); }
    private void validate()
    {
        if (state == null || state.version != 1 || state.projects == null || state.projects.isEmpty()
            || state.projects.size() > 100) throw new IllegalArgumentException("Unsupported or invalid saved planner");
        Set<String> ids = new HashSet<>();
        for (Project p : state.projects)
        {
            if (p == null || p.id == null || !ids.add(p.id) || p.entries == null || p.owned == null)
                throw new IllegalArgumentException("Invalid saved project");
            checkName(p.name);
            Set<String> entries = new HashSet<>();
            for (Entry e : p.entries)
            {
                if (e == null) throw new IllegalArgumentException("Invalid saved entry");
                catalogue.get(e.recipeId); checkQuantity(e.quantity);
                if (!entries.add(e.recipeId + ":" + e.fromScratch + ":" + e.completed)) throw new IllegalArgumentException("Duplicate saved entry");
            }
            for (Map.Entry<String, Long> m : p.owned.entrySet())
                if (m.getKey() == null || m.getKey().isBlank() || m.getValue() == null || m.getValue() < 0 || m.getValue() > MAX_OWNED)
                    throw new IllegalArgumentException("Invalid saved material count");
        }
        if (!ids.contains(state.selected)) state.selected = state.projects.get(0).id;
    }
    private static void checkQuantity(int n)
    {
        if (n < 1 || n > MAX_QUANTITY) throw new IllegalArgumentException("Quantity must be 1–1,000,000");
    }
    private static void checkName(String name)
    {
        if (name == null || name.isBlank() || name.length() > 60) throw new IllegalArgumentException("Use a project name of 1–60 characters");
    }
    public Project current()
    {
        return state.projects.stream().filter(p -> p.id.equals(state.selected)).findFirst().orElseThrow(IllegalStateException::new);
    }
    public void select(Project p)
    {
        if (!state.projects.contains(p)) throw new IllegalArgumentException("Unknown project");
        state.selected = p.id;
    }
    public Project create(String name)
    {
        checkName(name);
        if (state.projects.size() >= 100) throw new IllegalArgumentException("Project limit reached (100)");
        Project p = new Project(name.strip()); state.projects.add(p); state.selected = p.id; return p;
    }
    public void rename(String name) { checkName(name); current().name = name.strip(); }
    public void deleteCurrent()
    {
        state.projects.remove(current());
        if (state.projects.isEmpty()) state.projects.add(new Project("My builds"));
        state.selected = state.projects.get(0).id;
    }
    public void add(String id, int quantity, boolean fromScratch)
    {
        catalogue.get(id); checkQuantity(quantity);
        if (catalogue.get(id).previous == null) fromScratch = false;
        for (Entry e : current().entries)
            if (!e.completed && e.recipeId.equals(id) && e.fromScratch == fromScratch)
            {
                int total = Math.addExact(e.quantity, quantity); checkQuantity(total); e.quantity = total; return;
            }
        current().entries.add(new Entry(id, quantity, fromScratch));
    }
    public void quantity(Entry entry, int quantity) { checkQuantity(quantity); entry.quantity = quantity; }
    public void complete(Entry entry, boolean completed)
    {
        if (!current().entries.contains(entry)) throw new IllegalArgumentException("Unknown build");
        for (Entry other : current().entries)
            if (other != entry && other.completed == completed && other.recipeId.equals(entry.recipeId)
                && other.fromScratch == entry.fromScratch)
            {
                int total = Math.addExact(other.quantity, entry.quantity); checkQuantity(total);
                other.quantity = total; current().entries.remove(entry); return;
            }
        entry.completed = completed;
    }
    public void remove(Entry entry) { current().entries.remove(entry); }
    public void owned(String material, long quantity)
    {
        if (material == null || material.isBlank() || quantity < 0 || quantity > MAX_OWNED)
            throw new IllegalArgumentException("Owned must be 0–1,000,000,000,000");
        current().owned.put(material, quantity);
    }
    public Map<String, Long> requirements(String id, int count, boolean fromScratch)
    {
        checkQuantity(count);
        Map<String, Long> total = new TreeMap<>();
        for (Recipe r : fromScratch ? catalogue.chain(id) : Collections.singletonList(catalogue.get(id)))
            r.materials.forEach((name, n) -> total.merge(name, Math.multiplyExact(n, count), Math::addExact));
        return total;
    }
    public List<Material> materials()
    {
        Map<String, Long> total = new TreeMap<>();
        for (Entry e : current().entries)
            if (!e.completed) requirements(e.recipeId, e.quantity, e.fromScratch).forEach((name, n) -> total.merge(name, n, Math::addExact));
        List<Material> out = new ArrayList<>();
        total.forEach((name, n) -> out.add(new Material(name, n, current().owned.getOrDefault(name, 0L))));
        return out;
    }
    public String shoppingList()
    {
        StringBuilder s = new StringBuilder(current().name + " — Build Planner\nManual counts; not a bank scan.\n\n");
        for (Material m : materials())
            s.append(m.name).append(": need ").append(m.required).append(", have ").append(m.owned).append(", missing ").append(m.missing).append('\n');
        s.append("\nTools and unlock requirements: consult each recipe. Room costs and intermediate crafting are excluded.\n");
        for (Entry e : current().entries)
        {
            Recipe r = catalogue.get(e.recipeId);
            if (e.completed) { s.append("Completed: ").append(e.quantity).append(" × ").append(r.label()).append('\n'); continue; }
            s.append(e.quantity).append(" × ").append(r.label()).append(e.fromScratch ? " (from scratch)" : " (this step only)").append('\n');
            if (!e.fromScratch && r.previous != null) s.append("  Must already have: ").append(catalogue.get(r.previous).name).append('\n');
            s.append("  ").append(r.source).append('\n');
        }
        return s.toString();
    }
}
