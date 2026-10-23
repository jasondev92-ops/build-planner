package com.buildplanner;

import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(name = "Build Planner", description = "Search Sailing and house recipes and plan materials", tags = {"construction", "sailing", "recipes", "materials", "planner"})
public final class BuildPlannerPlugin extends Plugin
{
    static final String GROUP = "buildplanner";
    static final String STATE = "projectsV1";
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager config;
    @Inject private Gson gson;
    @Inject private net.runelite.client.game.ItemManager itemManager;
    @Inject private net.runelite.api.Client client;
    @Inject private net.runelite.client.config.RuneLiteConfig runeLiteConfig;
    @Inject private net.runelite.client.ui.overlay.OverlayManager overlayManager;
    @Inject private BuildBankOverlay bankOverlay;
    private volatile java.util.Set<Integer> bankItems = java.util.Set.of();
    private int construction = -1, sailing = -1;
    private volatile LiveSupplies live;

    @com.google.inject.Provides
    BuildPlannerConfig provideConfig(ConfigManager manager) { return manager.getConfig(BuildPlannerConfig.class); }
    java.util.Set<Integer> bankItems() { return bankItems; }
    private void publishPlan(Planner planner)
    {
        bankItems = requiredItemIds(planner, materialMappings);
    }
    static java.util.Set<Integer> requiredItemIds(Planner planner, Supplies mappings)
    {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Planner.Material m : planner.materials())
            for (int id : mappings.items.getOrDefault(m.name, new int[0])) ids.add(id);
        return java.util.Set.copyOf(ids);
    }
    private PlannerView view;
    private Supplies.Snapshot snapshot = Supplies.Snapshot.empty();
    private NavigationButton navigation;
    private PluginPanel panel;
    private Catalogue catalogue;
    private Supplies materialMappings;

    @Override protected void startUp() throws Exception
    {
        catalogue = Catalogue.load(gson);
        materialMappings = new Supplies(gson);
        LiveSupplies[] holder = new LiveSupplies[1];
        holder[0] = new LiveSupplies(client, itemManager, runeLiteConfig, new Supplies(gson), next ->
            SwingUtilities.invokeLater(() -> {
                if (live != holder[0]) return;
                snapshot = next;
                if (view != null) view.updateSupplies(next);
            }));
        live = holder[0];
        overlayManager.add(bankOverlay);
        Runnable init = () -> {
            panel = new PluginPanel(false) { private static final long serialVersionUID = 1L; };
            panel.setLayout(new BorderLayout());
            try { mount(catalogue); }
            catch (RuntimeException failure) { showRecovery(catalogue); }
            navigation = NavigationButton.builder().tooltip("Build Planner").icon(icon()).priority(8).panel(panel).build();
            toolbar.addNavigation(navigation);
        };
        if (SwingUtilities.isEventDispatchThread()) init.run(); else SwingUtilities.invokeAndWait(init);
    }

    @Subscribe public void onProfileChanged(ProfileChanged event)
    {
        SwingUtilities.invokeLater(() -> {
            if (panel == null) return;
            try { mount(catalogue); }
            catch (RuntimeException failure) { showRecovery(catalogue); }
        });
    }

    private void mount(Catalogue catalogue)
    {
        Planner planner = Planner.decode(catalogue, gson, config.getConfiguration(GROUP, STATE));
        panel.removeAll();
        view = new PlannerView(gson, catalogue, planner,
            () -> { config.setConfiguration(GROUP, STATE, planner.encode(gson)); publishPlan(planner); }, LinkBrowser::browse,
            (recipe, ready) -> {
                net.runelite.client.util.AsyncBufferedImage image = itemManager.getImage(recipe.iconItemId);
                if (image == null) ready.accept(null);
                else image.onLoaded(() -> ready.accept(image));
            });
        publishPlan(planner);
        view.updateLevels(construction, sailing);
        view.updateSupplies(snapshot);
        panel.add(view, BorderLayout.CENTER);
        panel.revalidate(); panel.repaint();
    }

    private void showRecovery(Catalogue catalogue)
    {
        view = null; bankItems = java.util.Set.of();
        panel.removeAll();
        JTextArea message = new JTextArea("Your saved project data could not be loaded. It has not been overwritten. You can disable this plugin to preserve it, or back it up and start a fresh planner below.");
        message.setLineWrap(true); message.setWrapStyleWord(true); message.setEditable(false);
        panel.add(message, BorderLayout.CENTER);
        JButton reset = new JButton("Back up & reset");
        reset.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(panel, "Back up saved data and reset the planner?", "Recover planner", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            String raw = config.getConfiguration(GROUP, STATE);
            if (raw != null) config.setConfiguration(GROUP, "recoveryBackup" + System.currentTimeMillis(), raw);
            config.unsetConfiguration(GROUP, STATE); mount(catalogue);
        });
        panel.add(reset, BorderLayout.SOUTH);
    }

    @Override protected void shutDown() throws Exception
    {
        live = null; bankItems = java.util.Set.of();
        overlayManager.remove(bankOverlay);
        Runnable stop = () -> {
            view = null; snapshot = Supplies.Snapshot.empty(); construction = -1; sailing = -1;
            if (navigation != null) toolbar.removeNavigation(navigation);
            navigation = null; panel = null;
        };
        if (SwingUtilities.isEventDispatchThread()) stop.run(); else SwingUtilities.invokeAndWait(stop);
    }

    @Subscribe public void onItemContainerChanged(net.runelite.api.events.ItemContainerChanged event)
    { LiveSupplies current=live; if (current!=null) current.containerChanged(event); }
    @Subscribe public void onGameTick(net.runelite.api.events.GameTick event)
    {
        LiveSupplies current=live; if (current==null) return;
        current.tick();
        boolean loggedIn = client.getGameState() == net.runelite.api.GameState.LOGGED_IN;
        publishLevels(current, loggedIn ? client.getBoostedSkillLevel(net.runelite.api.Skill.CONSTRUCTION) : -1,
            loggedIn ? client.getBoostedSkillLevel(net.runelite.api.Skill.SAILING) : -1);
    }
    private void publishLevels(LiveSupplies source, int con, int sail)
    {
        SwingUtilities.invokeLater(() -> {
            if (live != source) return;
            construction = con; sailing = sail;
            if (view != null) view.updateLevels(con, sail);
        });
    }
    @Subscribe public void onGameStateChanged(net.runelite.api.events.GameStateChanged event)
    { LiveSupplies current=live; if (current!=null) {
        current.stateChanged(event.getGameState());
        if (event.getGameState()!=net.runelite.api.GameState.LOGGED_IN) publishLevels(current,-1,-1);
    } }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(208, 178, 112)); g.fillRoundRect(3, 2, 18, 20, 3, 3);
        g.setColor(new Color(43, 57, 58)); g.fillRect(6, 5, 12, 14);
        g.setColor(new Color(208, 178, 112));
        for (int y = 8; y < 18; y += 4) g.fillRect(8, y, 8, 1);
        g.dispose(); return image;
    }
}
