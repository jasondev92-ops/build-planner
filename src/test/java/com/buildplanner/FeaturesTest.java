package com.buildplanner;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class FeaturesTest
{
    private final Gson gson = new Gson();
    private final Catalogue catalogue = Catalogue.load(gson);

    @Test public void completionPersistsAndUndoRestoresRequirements()
    {
        Planner p = Planner.fresh(catalogue);
        p.add("oak-chair", 2, false);
        Planner.Entry e = p.current().entries.get(0);
        p.complete(e, true);
        assertTrue(p.materials().isEmpty());
        p = Planner.decode(catalogue, gson, p.encode(gson));
        assertTrue(p.current().entries.get(0).completed);
        p.complete(p.current().entries.get(0), false);
        assertEquals(4L, p.materials().get(0).required);
    }
    @Test public void addingCompletedRecipeCreatesActiveBuildAndUndoMerges()
    {
        Planner p = Planner.fresh(catalogue); p.add("oak-chair",2,false);
        Planner.Entry done = p.current().entries.get(0); p.complete(done,true);
        p.add("oak-chair",3,false);
        assertEquals(2,p.current().entries.size());
        assertEquals(6L,p.materials().get(0).required);
        p.complete(done,false);
        assertEquals(1,p.current().entries.size());
        assertEquals(10L,p.materials().get(0).required);
    }
    @Test public void mergeOverflowDoesNotLoseCompletionHistory()
    {
        Planner p = Planner.fresh(catalogue); p.add("oak-chair",1,false);
        Planner.Entry done = p.current().entries.get(0); p.complete(done,true);
        p.add("oak-chair",Planner.MAX_QUANTITY,false);
        try { p.complete(done,false); fail(); } catch (IllegalArgumentException expected) { }
        assertTrue(done.completed); assertEquals(2,p.current().entries.size());
    }
    @Test public void completedChainsAndOtherProjectsDoNotPolluteShoppingList()
    {
        Planner p = Planner.fresh(catalogue); p.add("ornate-jewellery-box",1,true);
        p.complete(p.current().entries.get(0),true);
        assertTrue(p.materials().isEmpty());
        assertTrue(p.shoppingList().contains("Completed:"));
        p.create("Second"); p.add("oak-chair",1,false);
        assertEquals(1,p.materials().size());
    }
    @Test public void oldSavesDefaultToUnfinished()
    {
        Planner p=Planner.fresh(catalogue);p.add("oak-chair",1,false);
        String old=p.encode(gson).replace(",\"completed\":false", "");
        Planner read=Planner.decode(catalogue,gson,old);
        assertFalse(read.current().entries.get(0).completed);
        assertEquals(2L,read.materials().get(0).required);
    }
    @Test public void levelsAreUnknownOfflineAndUpdateWithoutChangingSelection() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            PlannerView v=new PlannerView(new Gson(), catalogue,Planner.fresh(catalogue),()->{},s->{});
            v.searchFor("Oak chair");v.chooseRecipe("oak-chair");
            assertTrue(v.levelText().contains("?"));
            v.updateLevels(18,1);assertTrue(v.levelText().contains("✗"));
            v.updateLevels(19,1);assertTrue(v.levelText().contains("✓"));
            assertEquals("oak-chair",v.recipeList().getSelectedValue().id);
            v.selectShortcut(1);assertEquals(1,v.recipeList().getModel().getSize());
            v.updateLevels(18,1);assertEquals(0,v.recipeList().getModel().getSize());
            v.updateLevels(-1,-1);assertEquals(0,v.recipeList().getModel().getSize());
        });
    }
    @Test public void materialsFilterNeedsKnownCountsAndSupportsManualMode() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=Planner.fresh(catalogue);
            PlannerView v=new PlannerView(new Gson(), catalogue,p,()->{},s->{});
            v.searchFor("Oak chair");v.selectShortcut(2);
            assertEquals(0,v.recipeList().getModel().getSize());
            v.updateSupplies(new Supplies.Snapshot(Map.of(8778,2L),Map.of(),true,1,Map.of(),Set.of(),0,"Test"));
            assertEquals(2,v.recipeList().getModel().getSize());
            v.chooseRecipe("oak-chair"); assertEquals("oak-chair", v.recipeList().getSelectedValue().id);
            v.updateSupplies(Supplies.Snapshot.empty());
            assertEquals(0,v.recipeList().getModel().getSize());
            p.current().manualCounts=true;p.owned("Oak plank",2);
            v.updateSupplies(Supplies.Snapshot.empty());
            assertEquals(2,v.recipeList().getModel().getSize());
            v.chooseRecipe("oak-chair"); assertEquals("oak-chair", v.recipeList().getSelectedValue().id);
        });
    }
    @Test public void bankHighlightIsOptInWithRuneLiteConfigMetadata() throws Exception
    {
        assertFalse(new BuildPlannerConfig() {}.highlightBank());
        assertEquals("buildplanner",BuildPlannerConfig.class.getAnnotation(net.runelite.client.config.ConfigGroup.class).value());
        assertEquals("highlightBank",BuildPlannerConfig.class.getMethod("highlightBank")
            .getAnnotation(net.runelite.client.config.ConfigItem.class).keyName());
    }
    @Test public void bankHighlightsFollowActiveProjectAndExcludeCompletedBuilds()
    {
        Planner p=Planner.fresh(catalogue);Supplies mapping=new Supplies(gson);
        p.add("oak-chair",1,false);
        assertEquals(Set.of(8778),BuildPlannerPlugin.requiredItemIds(p,mapping));
        Planner.Project first=p.current();
        p.create("Other");p.add("cat-basket",1,false);
        Set<Integer> ids=BuildPlannerPlugin.requiredItemIds(p,mapping);
        assertTrue(ids.contains(960));assertTrue(ids.contains(1539));assertFalse(ids.contains(8778));
        p.select(first);p.complete(first.entries.get(0),true);
        assertTrue(BuildPlannerPlugin.requiredItemIds(p,mapping).isEmpty());
    }
    @Test public void overlayToggleAndPlaceholderAreRespected() throws Exception
    {
        BuildPlannerPlugin plugin=new BuildPlannerPlugin();
        java.lang.reflect.Field items=BuildPlannerPlugin.class.getDeclaredField("bankItems");
        items.setAccessible(true);items.set(plugin,Set.of(8778));
        boolean[] enabled={false};
        BuildBankOverlay overlay=new BuildBankOverlay(plugin,new BuildPlannerConfig() {
            @Override public boolean highlightBank() { return enabled[0]; }
        });
        java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(40,40,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g=image.createGraphics();
        net.runelite.api.widgets.WidgetItem oak=new net.runelite.api.widgets.WidgetItem(8778,2,new java.awt.Rectangle(2,2,30,30),null,null);
        overlay.renderItemOverlay(g,8778,oak);assertEquals(0,image.getRGB(2,2));
        enabled[0]=true;
        overlay.renderItemOverlay(g,960,oak);assertEquals(0,image.getRGB(2,2));
        overlay.renderItemOverlay(g,8778,new net.runelite.api.widgets.WidgetItem(8778,0,new java.awt.Rectangle(2,2,30,30),null,null));
        assertEquals(0,image.getRGB(2,2));
        overlay.renderItemOverlay(g,8778,oak);assertNotEquals(0,image.getRGB(2,2));
        assertEquals(0,image.getRGB(35,35));g.dispose();
    }

    @Test public void catalogueCorrectionsKeepToolsOutOfShoppingLists()
    {
        Recipe keg=catalogue.get("keg");
        assertFalse(keg.materials.containsKey("Barrel stand"));
        assertTrue(keg.tools.contains("Barrel stand"));
        assertFalse(catalogue.get("gale-catcher").materials.containsKey("Captured wind mote"));
        Recipe sail=catalogue.get("table-black-sail");
        assertEquals(Map.of("Black dye",10L),sail.materials);
        assertTrue(sail.notes.contains("existing sail"));
    }
    @Test public void coinRequirementsHaveFaceValueWithoutGeQuote()
    {
        Supplies.Row row=new Supplies(gson).calculate(
            java.util.List.of(new Planner.Material("Coins",1000,250)),true,Supplies.Snapshot.empty()).get(0);
        assertEquals(1L,row.unitPrice); assertEquals(750L,row.missing);
        assertEquals(java.math.BigInteger.valueOf(750),row.cost(row.missing));
    }
    @Test public void constructionFilterIncludesHouseAndOtherConstruction()
    {
        assertEquals(739,catalogue.search("","Construction","All boats").size());
        assertEquals(182,catalogue.search("","Sailing","All boats").size());
        assertEquals("0.1.7-beta",ReleaseInfo.version());
        assertEquals("https://github.com/jasondev92-ops/build-planner/issues/new/choose",ReleaseInfo.feedbackUrl());
    }
    @Test public void freshProfilesDoNotInheritAnotherProfilesSavedPlan()
    {
        Planner existing = Planner.fresh(catalogue);
        existing.rename("Private test project");
        existing.add("oak-chair", 3, false);
        existing.owned("Oak plank", 4);
        String saved = existing.encode(gson);
        for (String empty : new String[]{null, "", "   "}) {
            Planner fresh = Planner.decode(catalogue, gson, empty);
            assertEquals(1, fresh.state.projects.size());
            assertEquals("My builds", fresh.current().name);
            assertTrue(fresh.current().entries.isEmpty());
            assertTrue(fresh.current().owned.isEmpty());
            assertTrue(fresh.materials().isEmpty());
        }
        Planner restored = Planner.decode(catalogue, gson, saved);
        assertEquals("Private test project", restored.current().name);
        assertEquals(3, restored.current().entries.get(0).quantity);
        assertEquals(Long.valueOf(4), restored.current().owned.get("Oak plank"));
    }
}
