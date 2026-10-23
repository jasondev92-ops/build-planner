package com.buildplanner;

import java.util.*;
import java.util.function.Consumer;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ItemManager;

/** Client-thread adapter. Bank/inventory are copied together at the end of a game tick. */
final class LiveSupplies
{
    private final Client client;
    private final java.util.function.IntFunction<ItemComposition> definitions;
    private final java.util.function.IntToLongFunction priceLookup;
    private final java.util.function.BooleanSupplier wikiPrices;
    private final Supplies materials;
    private final Consumer<Supplies.Snapshot> publish;
    private long account, bankAt, priceAt;
    private boolean inventoryDirty=true, bankDirty, bankWasOpen, inventoryKnown;
    private Map<Integer,Long> bank=Map.of(), inventory=Map.of(), prices=Map.of();
    private Set<Integer> untradeable=Set.of();
    private String source="RuneLite";
    LiveSupplies(Client client, ItemManager manager, RuneLiteConfig config, Supplies materials,
        Consumer<Supplies.Snapshot> publish)
    { this(client, manager::getItemComposition, manager::getItemPrice, config::useWikiItemPrices, materials, publish); }
    LiveSupplies(Client client, java.util.function.IntFunction<ItemComposition> definitions,
        java.util.function.IntToLongFunction priceLookup, java.util.function.BooleanSupplier wikiPrices,
        Supplies materials, Consumer<Supplies.Snapshot> publish)
    { this.client=client; this.definitions=definitions; this.priceLookup=priceLookup; this.wikiPrices=wikiPrices; this.materials=materials; this.publish=publish; }

    void clear()
    {
        account=0; bankAt=0; bank=Map.of(); inventory=Map.of(); inventoryKnown=false;
        inventoryDirty=true; bankDirty=false; bankWasOpen=false;
        publish.accept(snapshot());
    }
    void stateChanged(GameState state)
    {
        if (state==GameState.LOGIN_SCREEN || state==GameState.LOGIN_SCREEN_AUTHENTICATOR
            || state==GameState.LOGGING_IN || state==GameState.HOPPING || state==GameState.CONNECTION_LOST) clear();
    }
    void containerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId()==InventoryID.BANK) bankDirty=true;
        if (event.getContainerId()==InventoryID.INV) inventoryDirty=true;
    }
    void tick()
    {
        if (client.getGameState()!=GameState.LOGGED_IN) return;
        long current=client.getAccountHash();
        if (current==0 || current==-1) return;
        if (current!=account) { clear(); account=current; }
        Widget widget=client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        boolean open=widget!=null && !widget.isHidden();
        boolean changed=false;
        // A visible bank is required to establish the first snapshot in this session.
        if ((open && (!bankWasOpen || bankAt==0)) || (bankDirty && bankAt>0)) {
            ItemContainer container=client.getItemContainer(InventoryID.BANK);
            if (container!=null) { bank=copy(container); bankAt=System.currentTimeMillis(); changed=true; }
        }
        if (inventoryDirty || !inventoryKnown || bankDirty) {
            ItemContainer container=client.getItemContainer(InventoryID.INV);
            if (container!=null) {
                Map<Integer,Long> next=copy(container);
                changed |= !inventoryKnown || !next.equals(inventory);
                inventory=next; inventoryKnown=true;
            }
        }
        inventoryDirty=false; bankDirty=false; bankWasOpen=open;
        String wanted=wikiPrices.getAsBoolean()?"Wiki market prices (RuneLite cache)":"GE guide prices (RuneLite cache)";
        if (System.currentTimeMillis()-priceAt>=60_000 || !wanted.equals(source)) {
            Map<Integer,Long> next=new HashMap<>(); Set<Integer> unavailable=new HashSet<>();
            for (int[] ids : materials.items.values()) for (int id : ids) {
                if (!definitions.apply(id).isGeTradeable()) unavailable.add(id);
                else {
                    long price=priceLookup.applyAsLong(id);
                    if (price>0) next.put(id,price);
                }
            }
            prices=next; untradeable=unavailable; source=wanted; priceAt=System.currentTimeMillis(); changed=true;
        }
        if (changed) publish.accept(snapshot());
    }
    private Supplies.Snapshot snapshot()
    { return new Supplies.Snapshot(bank,inventory,inventoryKnown,bankAt,prices,untradeable,priceAt,source); }

    private Map<Integer,Long> copy(ItemContainer container)
    {
        Map<Integer,Long> quantities=new HashMap<>();
        for (Item item : container.getItems()) {
            if (item==null || item.getId()<0 || item.getQuantity()<=0) continue;
            ItemComposition definition=definitions.apply(item.getId());
            if (definition.getPlaceholderTemplateId()!=-1) continue;
            int id=definition.getNote()!=-1?definition.getLinkedNoteId():item.getId();
            quantities.merge(id,(long)item.getQuantity(),Long::sum);
        }
        return quantities;
    }
}
