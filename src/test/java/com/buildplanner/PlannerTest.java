package com.buildplanner;

import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlannerTest
{
    private final Gson gson = new Gson();
    private final Catalogue c = Catalogue.load(gson);
    private Planner p() { return Planner.fresh(c); }
    @Test public void allBundledRecipesAreValid() { assertEquals(921, c.size()); }
    @Test public void sloopQuantitiesAreNotSkiffQuantities()
    {
        assertEquals(Long.valueOf(600), c.get("mahogany-hull-sloop").materials.get("Mithril nails"));
        assertEquals(Long.valueOf(300), c.get("mahogany-hull-skiff").materials.get("Mithril nails"));
        assertEquals(50, c.get("mahogany-hull-sloop").sailing);
        assertEquals(48, c.get("mahogany-hull-skiff").sailing);
    }
    @Test public void caseInsensitiveMultiwordMaterialSearch()
    {
        List<Recipe> found = c.search("MITHRIL nails", "Sailing", "Sloop");
        assertTrue(found.stream().anyMatch(r -> r.id.equals("mahogany-hull-sloop")));
        assertFalse(found.stream().anyMatch(r -> r.boat.equals("Skiff")));
        assertTrue(found.stream().anyMatch(r -> r.id.equals("mahogany-cargo-hold")));
    }
    @Test public void noResultsIsEmpty() { assertTrue(c.search("zzzzzz", "All", "All boats").isEmpty()); }
    @Test public void sumsSharedMaterialsAndRepeatedQuantities()
    {
        Planner p=p(); p.add("mahogany-cargo-hold",1,false); p.add("mithril-salvaging-hook",2,false);
        assertEquals(Long.valueOf(16), total(p).get("Mahogany plank"));
        assertEquals(Long.valueOf(64), total(p).get("Mithril nails"));
        p.add("mithril-salvaging-hook",1,false);
        assertEquals(2,p.current().entries.size()); assertEquals(Long.valueOf(20),total(p).get("Mahogany plank"));
    }
    @Test public void wardrobeFullChainAndUpgradeDiffer()
    {
        Planner p=p(); Map<String,Long> all=p.requirements("mahogany-magic-wardrobe",1,true);
        assertEquals(Long.valueOf(10),all.get("Oak plank")); assertEquals(Long.valueOf(10),all.get("Teak plank"));
        assertEquals(Long.valueOf(4),all.get("Mahogany plank"));
        Map<String,Long> one=p.requirements("mahogany-magic-wardrobe",1,false);
        assertEquals(1,one.size()); assertEquals(Long.valueOf(4),one.get("Mahogany plank"));
    }
    @Test public void allPoolStepsAreIncluded()
    {
        Map<String,Long> all=p().requirements("ornate-rejuvenation-pool",2,true);
        assertEquals(Long.valueOf(10),all.get("Bucket of water")); assertEquals(Long.valueOf(20),all.get("Stamina potion(4)"));
        assertEquals(Long.valueOf(2000),all.get("Blood rune")); assertEquals(11,all.size());
    }
    @Test public void materialsAreOwnedOnceAcrossAllTargets()
    {
        Planner p=p(); p.add("oak-larder",2,false); p.add("oak-chair",1,false); p.owned("Oak plank",17);
        Planner.Material m=p.materials().get(0); assertEquals(18,m.required); assertEquals(1,m.missing);
    }
    @Test public void overOwnedNeverGivesNegativeMissing()
    {
        Planner p=p(); p.add("oak-chair",1,false); p.owned("Oak plank",99); assertEquals(0,p.materials().get(0).missing);
    }
    @Test public void projectsHaveIndependentCounts()
    {
        Planner p=p(); Planner.Project first=p.current(); p.owned("Oak plank",20); p.create("Boat");
        assertTrue(p.current().owned.isEmpty()); p.select(first); assertEquals(Long.valueOf(20),p.current().owned.get("Oak plank"));
    }
    @Test public void roundTripPreservesAllProjectsAndModes()
    {
        Planner p=p(); p.add("mahogany-magic-wardrobe",2,true); p.owned("Oak plank",7); p.create("Sloop"); p.add("mithril-helm-sloop",1,false);
        Planner q=Planner.decode(c,gson,p.encode(gson)); assertEquals(p.encode(gson),q.encode(gson));
        assertEquals("Sloop",q.current().name);
    }
    @Test public void deletionAlwaysLeavesUsableProject() { Planner p=p(); p.deleteCurrent(); assertNotNull(p.current()); assertTrue(p.current().entries.isEmpty()); }
    @Test(expected=IllegalArgumentException.class) public void rejectZeroQuantity() { p().add("oak-chair",0,false); }
    @Test(expected=IllegalArgumentException.class) public void rejectNegativeOwned() { p().owned("Oak plank",-1); }
    @Test(expected=IllegalArgumentException.class) public void rejectUnknownRecipe() { p().add("not-real",1,false); }
    @Test(expected=IllegalArgumentException.class) public void rejectFutureSchema() { Planner.decode(c,gson,"{\"version\":2}"); }
    @Test(expected=RuntimeException.class) public void rejectCorruptJson() { Planner.decode(c,gson,"{broken"); }
    @Test public void rejectedMergedQuantityDoesNotChangeState()
    {
        Planner p=p(); p.add("oak-chair",Planner.MAX_QUANTITY,false);
        try { p.add("oak-chair",1,false); fail(); } catch(IllegalArgumentException expected) { }
        assertEquals(Planner.MAX_QUANTITY,p.current().entries.get(0).quantity);
    }
    @Test public void bigTotalsDoNotOverflowIntegers()
    {
        Planner p=p(); p.add("gale-catcher",Planner.MAX_QUANTITY,false); assertEquals(Long.valueOf(25_000_000_000L),total(p).get("Air rune"));
    }
    @Test public void copyIncludesExistingFurnitureRequirement()
    {
        Planner p=p(); p.add("mahogany-magic-wardrobe",1,false);
        assertTrue(p.shoppingList().contains("Must already have: Carved teak magic wardrobe"));
        assertTrue(p.shoppingList().contains("not a bank scan"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsCycle()
    {
        Recipe r=gson.fromJson(gson.toJson(c.get("oak-chair")),Recipe.class);r.previous=r.id;new Catalogue(new Recipe[]{r});
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingDependency()
    {
        Recipe r=gson.fromJson(gson.toJson(c.get("oak-chair")),Recipe.class);r.previous="unknown";new Catalogue(new Recipe[]{r});
    }
    @Test public void actualSwingControlsSearchAndAddChain() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=p(); AtomicInteger saves=new AtomicInteger();
            PlannerView view=new PlannerView(new Gson(), c,p,saves::incrementAndGet,s -> {});
            view.searchFor("mahogany magic wardrobe"); view.chooseRecipe("mahogany-magic-wardrobe");
            assertTrue(view.detailText().contains("Already built: Carved teak magic wardrobe"));
            view.includeEarlierSteps(true); assertTrue(view.detailText().contains("10 × Oak plank"));
            view.addSelected(); assertEquals(1,saves.get()); assertEquals(Long.valueOf(10),total(p).get("Oak plank"));
            view.searchFor("zzzzzz"); assertTrue(view.detailText().contains("No matching recipe"));
        });
    }
    @Test public void clearingSearchBatchesResultsAndReusesRenderer() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            PlannerView view = new PlannerView(new Gson(), c, p(), () -> {}, s -> {});
            view.searchFor("zzzzzz");
            javax.swing.JList<Recipe> list = view.recipeList();
            AtomicInteger additions = new AtomicInteger();
            list.getModel().addListDataListener(new javax.swing.event.ListDataListener() {
                public void intervalAdded(javax.swing.event.ListDataEvent e) { additions.incrementAndGet(); }
                public void intervalRemoved(javax.swing.event.ListDataEvent e) {}
                public void contentsChanged(javax.swing.event.ListDataEvent e) {}
            });
            view.searchFor("");
            assertEquals(c.size(), list.getModel().getSize());
            assertEquals(1, additions.get());
            assertTrue(list.getFixedCellHeight() > 0);
            assertTrue(list.getFixedCellWidth() > 0);
            java.awt.Component first = list.getCellRenderer().getListCellRendererComponent(list, c.get("oak-chair"), 0, false, false);
            java.awt.Component second = list.getCellRenderer().getListCellRendererComponent(list, c.get("mahogany-magic-wardrobe"), 1, true, false);
            assertSame(first, second);
            assertTrue(((javax.swing.JTextArea) second).getText().contains("Mahogany"));
            String query = "mahogany magic wardrobe";
            for (int n = query.length(); n >= 0; n--) view.searchFor(query.substring(0, n));
            assertEquals(c.size(), list.getModel().getSize());
            assertNotNull(list.getSelectedValue());
        });
    }
    @Test public void cargoDetailsFollowShipAndLinkToExactItem() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            java.util.List<String> links = new java.util.ArrayList<>();
            PlannerView view = new PlannerView(new Gson(), c, p(), () -> {}, links::add);
            view.searchFor("mahogany cargo hold");
            assertTrue(view.detailText().contains("Raft: 60 slots"));
            assertTrue(view.detailText().contains("Skiff: 90 slots"));
            assertTrue(view.detailText().contains("Sloop: 120 slots"));
            assertFalse(view.earlierStepsVisible());
            view.selectBoat("Skiff");
            assertTrue(view.detailText().contains("Skiff: 90 slots"));
            assertFalse(view.detailText().contains("Sloop: 120 slots"));
            view.openSelectedWiki();
            assertEquals("https://oldschool.runescape.wiki/w/Mahogany_cargo_hold", links.get(0));
            assertEquals(32216, c.get("mahogany-cargo-hold").iconItemId);
            view.searchFor("mahogany magic wardrobe");
            assertTrue(view.earlierStepsVisible());
            view.searchFor("zzzzzz");
            assertFalse(view.earlierStepsVisible());
        });
    }
    @Test public void picturesFitPanelWithoutDistortion()
    {
        java.awt.image.BufferedImage tiny = new java.awt.image.BufferedImage(36,32,2);
        java.awt.image.BufferedImage fitted = PlannerView.fitPicture(tiny);
        assertEquals(72, fitted.getWidth()); assertEquals(64, fitted.getHeight());
        fitted = PlannerView.fitPicture(new java.awt.image.BufferedImage(600,200,2));
        assertEquals(96, fitted.getWidth()); assertEquals(32, fitted.getHeight());
    }
    @Test public void everyRecipeHasDescriptionSpecificPageAndExplicitImageStatus()
    {
        for (Recipe r : c.search("", "All", "All boats")) {
            assertNotNull(r.id, r.description);
            assertFalse(r.id, r.description.isBlank());
            assertTrue(r.id, r.iconItemId > 0 || "Image not yet verified".equals(r.iconStatus));
            assertFalse(r.id, r.source.endsWith("/Boat_facility"));
            assertFalse(r.id, r.source.endsWith("/Construction/Experience_table"));
        }
        assertNotEquals(c.get("oak-wardrobe").iconItemId, c.get("oak-magic-wardrobe").iconItemId);
        assertTrue(c.get("salvaging-station").source.endsWith("/Salvaging_station_(facility)"));
        assertTrue(c.get("greater-teleport-focus").source.endsWith("/Greater_teleportation_focus_(facility)"));
        assertEquals(65, c.get("hemp-trawling-net").construction);
    }
    @Test public void delayedPictureCannotReplaceNewSelection() throws Exception
    {
        java.util.Map<String, java.util.function.Consumer<java.awt.image.BufferedImage>> ready = new java.util.HashMap<>();
        PlannerView[] holder = new PlannerView[1];
        SwingUtilities.invokeAndWait(() -> {
            PlannerView view = new PlannerView(new Gson(), c, p(), () -> {}, s -> {}, (r, callback) -> ready.put(r.id, callback));
            holder[0] = view;
            view.searchFor("mahogany cargo hold");
            view.searchFor("oak cargo hold");
            ready.get("oak-cargo-hold").accept(new java.awt.image.BufferedImage(36,32,2));
            ready.get("mahogany-cargo-hold").accept(new java.awt.image.BufferedImage(36,32,2));
        });
        SwingUtilities.invokeAndWait(() -> assertEquals("Oak cargo hold — game icon", holder[0].pictureTooltip()));
    }
    private Map<String,Long> total(Planner p)
    {
        Map<String,Long> total=new TreeMap<>();for(Planner.Material m:p.materials())total.put(m.name,m.required);return total;
    }
}
