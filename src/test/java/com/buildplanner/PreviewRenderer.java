package com.buildplanner;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Renders real Swing components, not an HTML mockup. No game or account needed. */
public final class PreviewRenderer
{
    static void layout(Component c)
    {
        if(c instanceof Container) { ((Container)c).doLayout(); for(Component child:((Container)c).getComponents())layout(child); }
    }
    public static void main(String[] args) throws Exception
    {
        PlannerView[] panel = new PlannerView[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                for(String key:new String[]{"Panel.background","Viewport.background","TextArea.background","List.background","TextField.background","ComboBox.background","TabbedPane.background","Button.background","CheckBox.background","Spinner.background"}) UIManager.put(key,new Color(42,44,47));
                for(String key:new String[]{"Label.foreground","TextArea.foreground","List.foreground","TextField.foreground","ComboBox.foreground","TabbedPane.foreground","Button.foreground","CheckBox.foreground","Spinner.foreground"})UIManager.put(key,new Color(223,223,223));
                UIManager.put("TabbedPane.selected",new Color(61,77,77));
                Catalogue c=Catalogue.load(new Gson());Planner p=Planner.fresh(c);p.rename("Sloop upgrades");
                p.add("mahogany-cargo-hold",1,false);p.add("mithril-salvaging-hook",2,false);p.add("mithril-helm-sloop",1,false);
                p.owned("Mahogany plank",12);p.owned("Mithril nails",64);
                String recipeId = args.length > 1 ? args[1] : "mahogany-cargo-hold";
                BufferedImage suppliedImage = args.length > 2 ? ImageIO.read(new File(args[2])) : null;
                p.add("oak-chair",1,false);p.complete(p.current().entries.get(p.current().entries.size()-1),true);
                PlannerView v=new PlannerView(new Gson(), c,p,()->{},s->{}, (recipe, ready) -> {
                    if (recipe.id.equals(recipeId) && suppliedImage != null) ready.accept(suppliedImage);
                });
                v.updateLevels(53,58);
                panel[0]=v;
                v.setSize(242,800);v.searchFor(c.get(recipeId).name);v.chooseRecipe(recipeId);
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                PlannerView v=panel[0];
                File dir=new File(args.length>0?args[0]:"build/previews");dir.mkdirs();
                for(int tab=0;tab<4;tab++) {
                    v.selectTab(tab);for(int i=0;i<4;i++)layout(v);
                    BufferedImage img=new BufferedImage(242,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();v.printAll(g);g.dispose();ImageIO.write(img,"png",new File(dir,"panel-"+tab+".png"));
                }
                // Explicit sample data for layout inspection, not fetched/live prices.
                v.updateSupplies(new Supplies.Snapshot(java.util.Map.of(8782,10L,4822,64L,32889,9L),
                    java.util.Map.of(8782,2L),true,System.currentTimeMillis(),
                    java.util.Map.of(8782,2000L,4822,50L,32889,1000L,2359,800L,954,60L),
                    java.util.Set.of(),System.currentTimeMillis(),"Example prices (not live)"));
                v.selectTab(2);for(int i=0;i<4;i++)layout(v);
                BufferedImage example=new BufferedImage(242,800,BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=example.createGraphics();v.printAll(graphics);graphics.dispose();
                ImageIO.write(example,"png",new File(dir,"materials-example.png"));
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
    }
}
