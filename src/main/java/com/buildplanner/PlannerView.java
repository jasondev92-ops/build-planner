package com.buildplanner;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The same Swing panel is used by RuneLite and the offline visual checks. */
public final class PlannerView extends JPanel
{
    private final Catalogue catalogue;
    private final Planner planner;
    private final Runnable persist;
    private static final long serialVersionUID = 1L;
    private final JComboBox<Planner.Project> projects = new JComboBox<>();
    private final JTextField search = new JTextField();
    private final JComboBox<String> category = new JComboBox<>(new String[]{"All", "Sailing", "Construction"});
    private final JComboBox<String> boat = new JComboBox<>(new String[]{"All boats", "Raft", "Skiff", "Sloop"});
    private final DefaultListModel<Recipe> results = new DefaultListModel<>();
    private final JList<Recipe> list = new JList<>(results);
    private final JLabel count = new JLabel();
    private final JTextArea detail = area("");
    private final JTextArea recipeTitle = area("");
    private final JLabel picture = new JLabel("", SwingConstants.CENTER);
    private final BiConsumer<Recipe, Consumer<BufferedImage>> loadPicture;
    private int pictureRequest;
    private String picturedRecipe;
    private final JScrollPane detailScroll;

    private final JCheckBox scratch = new JCheckBox("Include earlier upgrades");
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, Planner.MAX_QUANTITY, 1));
    private final JPanel plan = stack();
    private final JPanel materials = stack();
    private final JLabel status = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton add;
    private final JButton wiki;
    private final Supplies supplyCalculator;
    private Supplies.Snapshot supplies = Supplies.Snapshot.empty();
    private final JLabel levels = new JLabel();
    private final JComboBox<String> shortcut = new JComboBox<>(new String[]{"All recipes", "Within my levels", "Have materials"});
    private int construction = -1, sailing = -1;
    private boolean materialsDirty;
    private boolean refreshing;
    private boolean updatingResults;

    public PlannerView(com.google.gson.Gson gson, Catalogue catalogue, Planner planner, Runnable persist, Consumer<String> openLink)
    {
        this(gson, catalogue, planner, persist, openLink, (recipe, ready) -> {});
    }

    public PlannerView(com.google.gson.Gson gson, Catalogue catalogue, Planner planner, Runnable persist, Consumer<String> openLink,
        BiConsumer<Recipe, Consumer<BufferedImage>> loadPicture)
    {
        this.catalogue = catalogue; this.planner = planner; this.persist = persist;
        this.loadPicture = loadPicture;
        this.supplyCalculator = new Supplies(gson);
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        setPreferredSize(new Dimension(225, 720));
        JPanel header = stack();
        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel("BUILD PLANNER");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        JLabel version = new JLabel("v" + ReleaseInfo.version(), SwingConstants.RIGHT);
        version.setFont(version.getFont().deriveFont(Font.PLAIN, 10f));
        version.setToolTipText("Build Planner version " + ReleaseInfo.version());
        titleRow.add(title, BorderLayout.CENTER);
        titleRow.add(version, BorderLayout.EAST);
        header.add(titleRow);
        header.add(area("Recipes & shopping lists\nConstruction & Sailing"));
        projects.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); header.add(projects);
        header.add(row(button("New", this::newProject), button("Rename", this::renameProject), button("Delete", this::deleteProject)));
        add(header, BorderLayout.NORTH);
        projects.addActionListener(e -> {
            if (!refreshing && projects.getSelectedItem() != null) {
                planner.select((Planner.Project) projects.getSelectedItem()); changed();
            }
        });

        JPanel find = new JPanel(new BorderLayout(0, 5));
        JPanel filters = stack();
        filters.add(new JLabel("Search name or material"));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); filters.add(search);
        filters.add(row(category, boat));
        shortcut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        shortcut.setToolTipText("Checks current boosted levels or supplies for one recipe step; other unlocks still apply.");
        filters.add(shortcut); filters.add(count); find.add(filters, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 5));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Fixed geometry avoids measuring every recipe whenever the search changes.
        // Reuse one renderer rather than constructing a text component per row.
        list.setFixedCellHeight(54);
        list.setFixedCellWidth(185);
        JTextArea cell = new JTextArea();
        cell.setEditable(false); cell.setLineWrap(true); cell.setWrapStyleWord(true);
        cell.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        cell.setOpaque(true); cell.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        list.setCellRenderer((items, recipe, index, selected, focus) -> {
            cell.setText(recipe.label());
            cell.setBackground(selected ? new Color(64, 92, 94) : items.getBackground());
            cell.setForeground(selected ? Color.WHITE : items.getForeground());
            return cell;
        });
        JScrollPane resultScroll = scroll(list); resultScroll.setPreferredSize(new Dimension(195, 150));
        center.add(resultScroll, BorderLayout.NORTH);
        detail.setName("recipeDetail");
        JPanel details = stack();
        recipeTitle.setFont(recipeTitle.getFont().deriveFont(Font.BOLD, 14f));
        details.add(recipeTitle);
        JPanel imageRow = new JPanel(new BorderLayout());
        imageRow.setOpaque(false); imageRow.add(picture, BorderLayout.CENTER);
        imageRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        imageRow.setPreferredSize(new Dimension(180, 72));
        picture.setName("recipePicture"); details.add(imageRow); details.add(levels); details.add(detail);
        detailScroll = scroll(details);
        center.add(detailScroll, BorderLayout.CENTER); find.add(center, BorderLayout.CENTER);
        JPanel actions = stack();
        scratch.setFont(scratch.getFont().deriveFont(11f)); scratch.addActionListener(e -> showRecipe());
        scratch.setToolTipText("Add the materials for every earlier upgrade, starting from the first build.");
        actions.add(scratch); actions.add(row(new JLabel("Builds / actions"), quantity));
        quantity.addChangeListener(e -> showRecipe());
        add = button("Add to plan", this::addRecipe);
        wiki = button("Wiki source", () -> { if (list.getSelectedValue() != null) openLink.accept(list.getSelectedValue().source); });
        actions.add(row(add, wiki)); find.add(actions, BorderLayout.SOUTH);
        list.addListSelectionListener(e -> { if (!updatingResults && !e.getValueIsAdjusting()) { scratch.setSelected(false); showRecipe(); } });
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        shortcut.addActionListener(e -> filter());
        category.addActionListener(e -> filter()); boat.addActionListener(e -> filter());
        tabs.addTab("Find", find); tabs.addTab("Plan", scroll(plan)); tabs.addTab("List", scroll(materials));
        JPanel help = stack();
        help.add(area("Build Planner " + ReleaseInfo.version()));
        if (!ReleaseInfo.feedbackUrl().isEmpty())
            help.add(button("Suggestions & bug reports", () -> openLink.accept(ReleaseInfo.feedbackUrl())));
        help.add(area("CATALOGUE COVERAGE\n921 recipe/build-action entries from the supplied Construction experience table, including 182 Sailing entries. All 960 source rows accounted for: 921 recipes, 33 upgrade totals represented by chains, and six activities without material recipes. Snapshot checked 23 October 2026.\n\n649 named game-icon mappings; 272 entries show Image not yet verified. Some entries are alternate methods or cosmetics. Quantities mean build/crafting actions, not guaranteed output-item counts. Crafted components are not expanded automatically. This covers the supplied table, not every recipe in the game.\n\nAutomatic counts use your bank and inventory after opening the bank this session. Manual mode retains counts set aside per project. Ship cargo, pouches, and other special storage are not scanned. Counts are not reservations across projects. Prices are cached estimates, not guaranteed offers. Projects are saved in your RuneLite configuration profile and may follow its sync settings. Development and Plugin Hub installations using the same profile share those saved projects. A profile without saved planner data starts with an empty project.\n\nBring a hammer and saw for ordinary builds. Check the linked source for room, quest, hotspot and operating requirements. Level marks check your current boosted levels; a question mark means unavailable. Crystal saw and other tool bonuses, quests and other skill requirements are not checked. Island docking requirements are listed in the item notes.\n\nEach planned line represents a separate build. Avoid selecting both a full upgrade chain and its lower steps unless you intend to build both. Generic house nails are listed separately from fixed-metal Sailing nails. Bring extras for bending.\n\nMark complete removes a whole planned line from remaining materials; Undo restores it. Manual owned counts are not deducted automatically. Enable Automatic bank highlighting through the gear beside Build Planner in RuneLite settings. Highlighting follows unfinished builds in the selected project.\n\nThis plugin performs no gameplay actions."));
        tabs.addTab("Info", scroll(help)); add(tabs, BorderLayout.CENTER);
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex()==2 && materialsDirty) refreshMaterials(); });
        status.setFont(status.getFont().deriveFont(10f)); add(status, BorderLayout.SOUTH);
        refreshProjects(); filter(); refreshPlan(); refreshMaterials();
    }

    private static JPanel stack() { return new Column(); }
    private static final class Column extends JPanel implements Scrollable
    {
        private static final long serialVersionUID = 1L;
        Column() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
        @Override protected void addImpl(Component c, Object constraints, int index)
        {
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            super.addImpl(c, constraints, index);
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(16, r.height - 16); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private static JPanel row(Component... components)
    {
        JPanel p = new JPanel(new GridLayout(1, components.length, 3, 0));
        for (Component c : components) p.add(c);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); return p;
    }
    private static JScrollPane scroll(Component c)
    {
        JScrollPane s = new JScrollPane(c); s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.getVerticalScrollBar().setUnitIncrement(16); return s;
    }
    private static JTextArea area(String s)
    {
        JTextArea a = new WrappedText(s); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setOpaque(false); a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        a.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2)); return a;
    }
    private static final class WrappedText extends JTextArea
    {
        private static final long serialVersionUID = 1L;
        WrappedText(String text) { super(text); }
        @Override public Dimension getPreferredSize()
        {
            int width = getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() - 6 : 195;
            setSize(Math.max(80, width), Short.MAX_VALUE);
            Dimension size = super.getPreferredSize();
            return new Dimension(width, size.height);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
    private JButton button(String name, Runnable action)
    {
        JButton b = new JButton(name); b.setMargin(new Insets(4, 3, 4, 3)); b.setFont(b.getFont().deriveFont(11f));
        b.addActionListener(e -> {
            try { action.run(); }
            catch (RuntimeException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Build Planner", JOptionPane.ERROR_MESSAGE); }
        }); return b;
    }
    private void refreshProjects()
    {
        refreshing = true; projects.removeAllItems();
        for (Planner.Project p : planner.state.projects) projects.addItem(p);
        projects.setSelectedItem(planner.current()); refreshing = false;
    }
    private void newProject()
    {
        String name = JOptionPane.showInputDialog(this, "Project name (up to 60 characters)");
        if (name != null) { planner.create(name); refreshProjects(); changed(); }
    }
    private void renameProject()
    {
        String name = JOptionPane.showInputDialog(this, "Project name", planner.current().name);
        if (name != null) { planner.rename(name); refreshProjects(); changed(); }
    }
    private void deleteProject()
    {
        if (JOptionPane.showConfirmDialog(this, "Delete this project and its manual counts?", "Delete project", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
        { planner.deleteCurrent(); refreshProjects(); changed(); }
    }
    private void filter()
    {
        String selected = list.getSelectedValue() == null ? null : list.getSelectedValue().id;
        List<Recipe> found = catalogue.search(search.getText(), (String) category.getSelectedItem(), (String) boat.getSelectedItem());
        if (shortcut.getSelectedIndex() == 1)
            found.removeIf(r -> construction < r.construction || (r.sailing > 0 && sailing < r.sailing));
        if (shortcut.getSelectedIndex() == 2) found.removeIf(r -> !hasMaterials(r));
        updatingResults = true;
        try
        {
            results.clear();
            results.addAll(found);
            int index = 0;
            for (int i = 0; i < results.size(); i++) if (results.get(i).id.equals(selected)) index = i;
            if (!results.isEmpty()) list.setSelectedIndex(index);
            scratch.setSelected(false);
        }
        finally { updatingResults = false; }
        count.setText(found.size() + " / " + catalogue.size() + " recipes");
        showRecipe();
    }
    private void showRecipe()
    {
        Recipe r = list.getSelectedValue();
        if (add == null) return;
        add.setEnabled(r != null); wiki.setEnabled(r != null);
        scratch.setEnabled(r != null && r.previous != null);
        scratch.setVisible(r != null && r.previous != null);
        recipeTitle.setText(r == null ? "" : r.label());
        updatePicture(r);
        showLevels(r);
        if (r == null) { detail.setText("No matching recipe. Try a shorter name or check Info for coverage."); return; }
        StringBuilder text = new StringBuilder();
        if (r.description != null) text.append(r.description).append("\n\n");
        if (r.capacities != null) {
            text.append("STORAGE CAPACITY\n");
            String ship = (String) boat.getSelectedItem();
            if (r.capacities.containsKey(ship)) text.append(ship).append(": ").append(r.capacities.get(ship)).append(" slots\n");
            else r.capacities.forEach((name, slots) -> text.append(name).append(": ").append(slots).append(" slots\n"));
            text.append('\n');
        }
        if (r.stats != null) text.append("STATS & EFFECTS\n").append(r.stats).append("\n\n");

        if (r.previous != null)
        {
            if (scratch.isSelected()) text.append("Includes all earlier build steps.\n\n");
            else text.append("Already built: ").append(catalogue.get(r.previous).name).append("\nOnly this upgrade's materials below.\n\n");
        }
        text.append("MATERIALS\n");
        planner.requirements(r.id, (Integer) quantity.getValue(), scratch.isSelected()).forEach((name, n) -> text.append(n).append(" × ").append(name).append('\n'));
        if (r.tools != null && !r.tools.isBlank()) text.append("\nTOOLS / UNLOCKS (not consumed)\n").append(r.tools).append('\n');
        if (r.facility != null && !r.facility.isBlank()) text.append("\nFACILITY\n").append(r.facility).append('\n');
        if (r.otherSkills != null && !r.otherSkills.isEmpty()) {
            text.append("\nOTHER LEVELS (not automatically checked)\n");
            r.otherSkills.forEach((skill, level) -> text.append(skill).append(" ").append(level).append('\n'));
        }
        if (r.notes != null && !r.notes.isEmpty()) text.append('\n').append(r.notes).append('\n');
        text.append("\nChecked ").append(r.checked).append(" · Wiki source below");
        detail.setText(text.toString()); detail.setCaretPosition(0);
    }
    private boolean hasMaterials(Recipe recipe)
    {
        java.util.List<Planner.Material> needs = new java.util.ArrayList<>();
        recipe.materials.forEach((name, amount) -> needs.add(new Planner.Material(name, amount,
            planner.current().owned.getOrDefault(name, 0L))));
        return supplyCalculator.calculate(needs, planner.current().manualCounts, supplies).stream()
            .allMatch(m -> m.countsKnown && m.missing == 0);
    }
    public void updateLevels(int con, int sail)
    {
        if (construction == con && sailing == sail) return;
        construction = con; sailing = sail;
        if (shortcut.getSelectedIndex() == 1) filter(); else showLevels(list.getSelectedValue());
    }
    private static String levelMark(int actual, int required)
    {
        if (actual < 0) return "<font color='#aaaaaa'>? </font>";
        return actual >= required ? "<font color='#4da57f'>✓ </font>" : "<font color='#ee7777'>✗ </font>";
    }
    private void showLevels(Recipe r)
    {
        levels.setText(r == null ? "" : "<html>" + levelMark(construction, r.construction) + "Construction " + r.construction
            + (r.sailing > 0 ? "<br>" + levelMark(sailing, r.sailing) + "Sailing " + r.sailing : "") + "</html>");
        levels.setToolTipText(construction < 0 ? "Log in to check your levels." :
            "Current boosted levels: Construction " + construction + ", Sailing " + sailing + ". Other unlocks and tool bonuses are not checked.");
    }
    private void updatePicture(Recipe r)
    {
        String id = r == null ? null : r.id;
        if (java.util.Objects.equals(id, picturedRecipe)) return;
        picturedRecipe = id;
        SwingUtilities.invokeLater(() -> detailScroll.getVerticalScrollBar().setValue(0));
        int request = ++pictureRequest;
        picture.setIcon(null);
        picture.setToolTipText(null);
        picture.setText(r != null && r.iconItemId > 0 ? "Loading image…" : "Image not yet verified");
        picture.getParent().setVisible(r != null);
        if (r == null || r.iconItemId <= 0) return;
        loadPicture.accept(r, image -> SwingUtilities.invokeLater(() -> {
            if (request != pictureRequest) return;
            if (image == null) { picture.setText("Image unavailable"); return; }
            picture.setText("");
            picture.setIcon(new ImageIcon(fitPicture(image)));
            picture.setToolTipText(r.name + " — game icon");
        }));
    }
    static BufferedImage fitPicture(BufferedImage image)
    {
        double scale = Math.min(2.0, Math.min(96.0 / image.getWidth(), 64.0 / image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, width, height, null); g.dispose(); return result;
    }
    private void addRecipe()
    {
        Recipe r = list.getSelectedValue(); if (r == null) return;
        try { quantity.commitEdit(); } catch (java.text.ParseException e) { throw new IllegalArgumentException("Enter a whole quantity"); }
        planner.add(r.id, (Integer) quantity.getValue(), scratch.isSelected()); changed(); status.setText("Added to " + planner.current().name);
    }
    private void changed()
    {
        try { persist.run(); status.setText("Saved"); }
        catch (RuntimeException e) { status.setText("SAVE FAILED — keep this panel open"); throw e; }
        refreshPlan(); refreshMaterials();
        if (shortcut.getSelectedIndex()==2) filter();
    }
    private void refreshPlan()
    {
        plan.removeAll();
        plan.add(area("Each line is a separate build. Mark complete finishes the entire quantity; Undo restores it. Full chains include earlier steps."));
        if (planner.current().entries.isEmpty()) plan.add(area("Your project is empty. Find a recipe, choose a quantity, then add it here."));
        for (Planner.Entry e : planner.current().entries)
        {
            Recipe r = catalogue.get(e.recipeId); JPanel card = stack();
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2), BorderFactory.createEtchedBorder()));
            card.add(area((e.completed ? "✓ COMPLETED\n" : "") + r.label() + "\n" + (e.fromScratch ? "From scratch" : "This step only") + (r.previous != null && !e.fromScratch ? "\nRequires existing " + catalogue.get(r.previous).name : "")));
            card.add(row(new JLabel("Qty: " + e.quantity), button("Change", () -> {
                String v = JOptionPane.showInputDialog(this, "New quantity", e.quantity);
                if (v != null) { planner.quantity(e, Integer.parseInt(v.strip())); changed(); }
            }), button("Remove", () -> { planner.remove(e); changed(); })));
            card.add(button(e.completed ? "Undo completion" : "Mark complete", () -> { planner.complete(e, !e.completed); changed(); }));
            plan.add(card);
        }
        plan.add(Box.createVerticalGlue()); plan.revalidate(); plan.repaint();
    }
    public void updateSupplies(Supplies.Snapshot next)
    {
        supplies = next;
        materialsDirty = true;
        // Live item changes never rebuild search controls or do work while typing in Find.
        if (tabs.getSelectedIndex()==2) refreshMaterials();
        if (shortcut.getSelectedIndex()==2) filter();
    }
    private String countStatus()
    {
        if (planner.current().manualCounts) return "Manual counts · supplies set aside for this project";
        String bank = supplies.bankAt==0 ? "Open your bank to refresh counts" : "Bank last seen " + time(supplies.bankAt);
        return bank + (supplies.inventoryKnown ? "\nInventory updates automatically" : "\nInventory unavailable until logged in");
    }
    private static String time(long millis)
    { return java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); }
    private String materialText(Supplies.Row m)
    {
        boolean manual=planner.current().manualCounts;
        String text=m.name+"\nNeed "+m.required;
        if (manual) text+="  ·  Have "+m.owned;
        else text+="\nBank "+(supplies.bankAt==0?"?":m.bank)+"  ·  Inventory "+(supplies.inventoryKnown?m.inventory:"?");
        text+="\n"+(m.countsKnown?(m.missing==0?"Ready":"Missing "+m.missing):"Missing: refresh bank/inventory");
        if (Supplies.ANY_NAILS.equals(m.name) && !manual) text+="\nCounts exclude nails reserved for fixed-metal recipes.";
        if (m.unitPrice>0) {
            text+="\nEach: "+Supplies.gp(java.math.BigInteger.valueOf(m.unitPrice));
            if (m.countsKnown) text+="\nMissing cost: "+Supplies.gp(m.cost(m.missing));
        }
        if (!m.priceNote.isEmpty()) text+="\n"+m.priceNote;
        return text;
    }
    String shoppingListText()
    {
        List<Supplies.Row> rows=supplyCalculator.calculate(planner.materials(),planner.current().manualCounts,supplies);
        StringBuilder out=new StringBuilder(planner.current().name+" — Build Planner\n"+countStatus()+"\n\n");
        for (Supplies.Row r:rows) out.append(materialText(r)).append("\n\n");
        out.append(Supplies.summary(rows)).append("\n").append(supplies.priceSource).append("; estimates only.\n");
        String original=planner.shoppingList();
        out.append(original.substring(original.indexOf("\nTools and unlock")));
        return out.toString();
    }
    private void refreshMaterials()
    {
        materialsDirty=false;
        materials.removeAll();
        materials.add(area("COMBINED MATERIALS"));
        JComboBox<String> mode=new JComboBox<>(new String[]{"Automatic: bank + inventory", "Manual: set aside"});
        mode.setFont(mode.getFont().deriveFont(11f));
        mode.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
        mode.setSelectedIndex(planner.current().manualCounts?1:0);
        mode.addActionListener(e -> { planner.current().manualCounts=mode.getSelectedIndex()==1; changed(); });
        materials.add(mode);
        materials.add(area(countStatus()));
        materials.add(button("Copy shopping list", () -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(shoppingListText()), null);
            status.setText("Shopping list copied");
        }));
        List<Supplies.Row> rows=supplyCalculator.calculate(planner.materials(),planner.current().manualCounts,supplies);
        if (rows.isEmpty()) materials.add(area("Add recipes to see your shopping list."));
        else {
            JTextArea total=area(Supplies.summary(rows));
            total.setFont(total.getFont().deriveFont(Font.BOLD)); materials.add(total);
            materials.add(area(supplies.priceSource+"\n"+(supplies.priceAt>0?"Cache checked "+time(supplies.priceAt):"Prices load after login")+"\nEstimates only; fees and extra tools excluded."));
        }
        for (Supplies.Row m : rows)
        {
            JPanel card = stack(); card.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
            JTextArea label = area(materialText(m));
            if (m.countsKnown && m.missing == 0) label.setForeground(new Color(77, 165, 127)); card.add(label);
            if (planner.current().manualCounts) card.add(row(button("Set have", () -> {
                String v = JOptionPane.showInputDialog(this, "How many have you set aside?", m.owned);
                if (v != null) { planner.owned(m.name, Long.parseLong(v.strip())); changed(); }
            }), button("Have all", () -> { planner.owned(m.name, m.required); changed(); })));
            materials.add(card);
        }
        materials.add(Box.createVerticalGlue()); materials.revalidate(); materials.repaint();
    }
    // Package-private hooks for real-component UI regression tests.
    void selectShortcut(int index) { shortcut.setSelectedIndex(index); }
    String levelText() { return levels.getText(); }
    String pictureTooltip() { return picture.getToolTipText(); }
    boolean earlierStepsVisible() { return scratch.isVisible(); }
    void selectBoat(String name) { boat.setSelectedItem(name); }
    void openSelectedWiki() { wiki.doClick(); }
    JList<Recipe> recipeList() { return list; }
    void searchFor(String text) { search.setText(text); }
    void selectTab(int tab) { tabs.setSelectedIndex(tab); }
    String detailText() { return detail.getText(); }
    void chooseRecipe(String id) { for (int i=0; i<results.size(); i++) if(results.get(i).id.equals(id)) list.setSelectedIndex(i); }
    void includeEarlierSteps(boolean enabled) { scratch.setSelected(enabled); showRecipe(); }
    void addSelected() { addRecipe(); }
}
