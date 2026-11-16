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
        assertEquals("0.1.8-beta",ReleaseInfo.version());
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
    @Test public void roomFilterIncludesSharedFurnitureAndExcludesOtherActivities()
    {
        assertEquals(24,catalogue.rooms().size());
        assertEquals(Set.of("Parlour"),Set.copyOf(catalogue.get("oak-chair").rooms));
        assertEquals(Set.of("Parlour","Quest hall","Study"),Set.copyOf(catalogue.get("table-oak-bookcase").rooms));
        assertEquals(Set.of("oak-chair","table-oak-armchair"),catalogue.search("oak chair","All","All boats","Parlour").stream().map(r -> r.id).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2,catalogue.search("oak chair","Construction","All boats","Parlour").size());
        assertTrue(catalogue.search("","Sailing","All boats","Kitchen").isEmpty());
        assertFalse(catalogue.get("table-oak-chair-mahogany-homes").rooms.contains("Parlour"));
        assertEquals(921,catalogue.search("","All","All boats","All rooms").size());
        assertFalse(catalogue.get("table-boundary-stones").rooms.contains("Superior garden"));
        assertTrue(catalogue.get("table-marble-wall").rooms.containsAll(java.util.List.of("Formal garden","Superior garden")));
        assertFalse(catalogue.get("table-brown-rug").rooms.contains("League hall"));
        assertTrue(catalogue.get("table-rug").rooms.contains("League hall"));
    }
    @Test public void roomDropdownCombinesWithSearchAndResetsForSailing() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            PlannerView v=new PlannerView(gson,catalogue,Planner.fresh(catalogue),()->{},s->{});
            v.selectRoom("Parlour");v.searchFor("Oak chair");
            assertEquals(2,v.recipeList().getModel().getSize());
            v.selectRoom("Kitchen");assertEquals(0,v.recipeList().getModel().getSize());
            v.searchFor("");v.selectCategory("Sailing");
            assertFalse(v.roomFilterEnabled());assertEquals(182,v.recipeList().getModel().getSize());
            v.selectCategory("Construction");assertTrue(v.roomFilterEnabled());
            assertEquals(739,v.recipeList().getModel().getSize());
        });
    }
    private static String namedText(java.awt.Component component, String name)
    {
        if (name.equals(component.getName())) return componentText(component);
        if (component instanceof java.awt.Container)
            for (java.awt.Component child:((java.awt.Container)component).getComponents()) {
                String text=namedText(child,name);if(text!=null)return text;
            }
        return null;
    }
    private static String componentText(java.awt.Component c)
    {
        if(c instanceof javax.swing.JTextArea)return ((javax.swing.JTextArea)c).getText();
        if(c instanceof javax.swing.JLabel)return ((javax.swing.JLabel)c).getText();
        if(c instanceof java.awt.Container) {
            java.awt.Container container=(java.awt.Container)c;
            if(container.getLayout() instanceof java.awt.BorderLayout) {
                java.awt.BorderLayout layout=(java.awt.BorderLayout)container.getLayout();
                java.awt.Component amount=layout.getLayoutComponent(java.awt.BorderLayout.EAST);
                java.awt.Component material=layout.getLayoutComponent(java.awt.BorderLayout.CENTER);
                if(amount instanceof javax.swing.JLabel && material instanceof javax.swing.JTextArea)
                    return componentText(amount)+" × "+componentText(material);
            }
            StringBuilder text=new StringBuilder();
            for(java.awt.Component child:container.getComponents())text.append(componentText(child)).append('\n');
            return text.toString();
        }
        return "";
    }
    private static java.awt.Component named(java.awt.Component c,String name)
    {
        if(name.equals(c.getName()))return c;
        if(c instanceof java.awt.Container)for(java.awt.Component child:((java.awt.Container)c).getComponents()) {
            java.awt.Component found=named(child,name);if(found!=null)return found;
        }
        return null;
    }
    @Test public void collapsedCardsStayCollapsedAcrossRefreshAndRoomFiltering() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=Planner.fresh(catalogue);p.add("oak-chair",2,false);p.add("gilded-altar",1,false);
            PlannerView v=new PlannerView(gson,catalogue,p,()->{},s->{});
            assertTrue(named(v,"planBody:oak-chair").isVisible());
            ((javax.swing.JButton)named(v,"planToggle:oak-chair")).doClick();
            assertFalse(named(v,"planBody:oak-chair").isVisible());
            assertTrue(named(v,"planBody:gilded-altar").isVisible());
            v.searchFor("Oak chair");v.chooseRecipe("oak-chair");v.addSelected();
            assertFalse(named(v,"planBody:oak-chair").isVisible());
            assertTrue(namedText(v,"planMaterials:oak-chair").contains("6 × Oak plank"));
            int all=p.materials().size();
            v.selectPlanRoom("Chapel");assertNull(named(v,"planBody:oak-chair"));
            assertNotNull(named(v,"planBody:gilded-altar"));assertEquals(all,p.materials().size());
            v.selectPlanRoom("All rooms");assertFalse(named(v,"planBody:oak-chair").isVisible());
            ((javax.swing.JButton)named(v,"planToggle:oak-chair")).doClick();
            assertTrue(named(v,"planBody:oak-chair").isVisible());
            assertTrue(componentText(v).contains("Parlour"));
            assertTrue(componentText(v).contains("Chapel"));
        });
    }
    @Test public void planCardsShowSeparateQuantityAndChainMaterials() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=Planner.fresh(catalogue);p.add("oak-chair",3,false);p.add("ornate-jewellery-box",2,true);
            PlannerView v=new PlannerView(gson,catalogue,p,()->{},s->{});
            String chair=namedText(v,"planMaterials:oak-chair");
            assertTrue(chair.contains("Total for 3 builds / actions"));
            assertTrue(chair.contains("6 × Oak plank"));assertFalse(chair.contains("Gold leaf"));
            String chain=namedText(v,"planMaterials:ornate-jewellery-box");
            p.requirements("ornate-jewellery-box",2,true).forEach((name,n)->assertTrue(chain.contains(n+" × "+name)));
            assertTrue(chain.contains("6 × Games necklace(8)"));
        });
    }
    @Test public void planCardRefreshesAfterAddingAndCompleting() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=Planner.fresh(catalogue);p.add("oak-chair",1,false);
            PlannerView v=new PlannerView(gson,catalogue,p,()->{},s->{});
            v.searchFor("Oak chair");v.chooseRecipe("oak-chair");v.addSelected();
            assertTrue(namedText(v,"planMaterials:oak-chair").contains("4 × Oak plank"));
            clickButton(v,"Mark complete");
            assertTrue(namedText(v,"planMaterials:oak-chair").contains("Materials (completed):"));
            assertTrue(p.materials().isEmpty());
            clickButton(v,"Undo completion");
            assertTrue(namedText(v,"planMaterials:oak-chair").contains("Materials needed:"));
            assertEquals(4L,p.materials().get(0).required);
        });
    }
    private static boolean clickButton(java.awt.Component component,String label)
    {
        if(component instanceof javax.swing.JButton && label.equals(((javax.swing.JButton)component).getText())) {
            ((javax.swing.JButton)component).doClick();return true;
        }
        if(component instanceof java.awt.Container)
            for(java.awt.Component child:((java.awt.Container)component).getComponents())if(clickButton(child,label))return true;
        return false;
    }

}
