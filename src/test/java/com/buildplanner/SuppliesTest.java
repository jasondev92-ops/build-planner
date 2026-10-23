package com.buildplanner;

import com.google.gson.Gson;
import java.util.*;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuppliesTest
{
    private final Supplies supplies=new Supplies(new Gson());
    private Supplies.Snapshot snapshot(Map<Integer,Long> bank, Map<Integer,Long> inv, Map<Integer,Long> prices)
    { return new Supplies.Snapshot(bank,inv,true,1000,prices,Set.of(),2000,"Test prices"); }
    @Test public void mappingsCoverEveryMaterialAndDisambiguateNames()
    {
        for (Recipe r:Catalogue.load(new Gson()).search("","All","All boats"))
            for (String material:r.materials.keySet()) assertNotNull(material,supplies.items.get(material));
        assertArrayEquals(new int[]{8788},supplies.items.get("Magic stone"));
        assertArrayEquals(new int[]{8784},supplies.items.get("Gold leaf"));
        assertArrayEquals(new int[]{1539},supplies.items.get("Steel nails"));
        assertArrayEquals(new int[]{1712},supplies.items.get("Amulet of glory(4)"));
    }
    @Test public void countsAndCostUseOnlyMissingMaterials()
    {
        Supplies.Row row=supplies.calculate(List.of(new Planner.Material("Mahogany plank",20,999)),false,
            snapshot(Map.of(8782,10L),Map.of(8782,2L),Map.of(8782,2000L))).get(0);
        assertEquals(12,row.owned); assertEquals(8,row.missing);
        assertEquals(BigInteger.valueOf(16000),row.cost(row.missing));
        assertTrue(Supplies.summary(List.of(row)).contains("40,000 gp"));
        assertTrue(Supplies.summary(List.of(row)).contains("16,000 gp"));
    }
    @Test public void unknownBankDoesNotPretendMissingCountsAreKnown()
    {
        Supplies.Row row=supplies.calculate(List.of(new Planner.Material("Oak plank",8,0)),false,Supplies.Snapshot.empty()).get(0);
        assertFalse(row.countsKnown);
        assertTrue(Supplies.summary(List.of(row)).contains("refresh bank/inventory"));
    }
    @Test public void manualCountsRemainIndependentOfBank()
    {
        Supplies.Row row=supplies.calculate(List.of(new Planner.Material("Oak plank",8,3)),true,
            snapshot(Map.of(8778,100L),Map.of(),Map.of(8778,500L))).get(0);
        assertEquals(3,row.owned);assertEquals(5,row.missing);assertTrue(row.countsKnown);
    }
    @Test public void fixedMetalNailsAreReservedBeforeGenericNails()
    {
        List<Supplies.Row> rows=supplies.calculate(List.of(new Planner.Material("Steel nails",8,0),
            new Planner.Material(Supplies.ANY_NAILS,10,0)),false,
            snapshot(Map.of(1539,6L,4819,4L),Map.of(1539,4L),Map.of(1539,30L,4819,5L)));
        assertEquals(0,rows.get(0).missing);assertEquals(6,rows.get(1).owned);assertEquals(4,rows.get(1).missing);
        assertEquals(5,rows.get(1).unitPrice);assertTrue(rows.get(1).priceNote.contains("bronze nails"));
    }
    @Test public void untradeableOrMissingPricesAreNotCountedAsFree()
    {
        Supplies.Snapshot s=new Supplies.Snapshot(Map.of(),Map.of(),true,1,Map.of(),Set.of(22710),1,"Test");
        List<Supplies.Row> rows=supplies.calculate(List.of(new Planner.Material("Curator's medallion",1,0),
            new Planner.Material("Oak plank",8,0)),false,s);
        assertEquals("Not sold on GE",rows.get(0).priceNote);assertEquals("Price unavailable",rows.get(1).priceNote);
        assertTrue(Supplies.summary(rows).contains("2 missing material(s) unpriced"));
        rows=supplies.calculate(List.of(new Planner.Material("Curator's medallion",1,1)),true,s);
        assertTrue(Supplies.summary(rows).contains("Estimated cost to finish: 0 gp"));
    }
    @Test public void largeCostDoesNotOverflowLong()
    {
        Supplies.Row row=supplies.calculate(List.of(new Planner.Material("Oak plank",10_000_000_000L,0)),true,
            snapshot(Map.of(),Map.of(),Map.of(8778,2_000_000_000L))).get(0);
        assertEquals(new BigInteger("20000000000000000000"),row.cost(row.required));
    }
    @Test public void oldSavesAndManualModeRoundTrip()
    {
        Gson gson=new Gson();Catalogue c=Catalogue.load(gson);Planner p=Planner.fresh(c);
        p.owned("Oak plank",5);p.current().manualCounts=true;
        Planner read=Planner.decode(c,gson,p.encode(gson));assertTrue(read.current().manualCounts);
        assertEquals(Long.valueOf(5),read.current().owned.get("Oak plank"));
        String old=p.encode(gson).replace("\"manualCounts\":true,","");
        read=Planner.decode(c,gson,old);assertFalse(read.current().manualCounts);
        assertEquals(Long.valueOf(5),read.current().owned.get("Oak plank"));
    }
    @Test public void copiedListMatchesAutomaticCounts() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Planner p=Planner.fresh(Catalogue.load(new Gson()));p.add("oak-larder",1,false);
            PlannerView view=new PlannerView(new Gson(), Catalogue.load(new Gson()),p,()->{},u->{});
            view.updateSupplies(snapshot(Map.of(8778,5L),Map.of(8778,1L),Map.of(8778,500L)));
            String copied=view.shoppingListText();assertTrue(copied.contains("Missing 2"));assertTrue(copied.contains("1,000 gp"));
            assertFalse(copied.contains("Manual counts; not a bank scan"));
        });
    }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String,Object[],Object> answer)
    {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args)->{
            Object result=answer.apply(method.getName(),args);
            if (result!=null) return result;
            if (method.getReturnType()==boolean.class) return false;
            if (method.getReturnType()==int.class) return 0;
            if (method.getReturnType()==long.class) return 0L;
            return null;
        });
    }
    private static ItemContainer container(Item... items)
    { return proxy(ItemContainer.class,(n,a)->n.equals("getItems")?items:null); }
    @Test public void clientEventsNormalizeNotesIgnorePlaceholdersAndIsolateAccounts()
    {
        long[] account={10};boolean[] open={false};
        ItemContainer[] bank={container(new Item(8778,10))}, inv={container(new Item(8779,2),new Item(90000,99),new Item(-1,0))};
        Widget widget=proxy(Widget.class,(n,a)->n.equals("isHidden")?!open[0]:null);
        Client client=proxy(Client.class,(n,a)->{
            if (n.equals("getGameState"))return GameState.LOGGED_IN;
            if (n.equals("getAccountHash"))return account[0];
            if (n.equals("getWidget"))return widget;
            if (n.equals("getItemContainer"))return a[0].equals(InventoryID.BANK)?bank[0]:inv[0];
            return null;
        });
        java.util.function.IntFunction<ItemComposition> defs=id->proxy(ItemComposition.class,(n,a)->{
            if(n.equals("getPlaceholderTemplateId"))return id==90000?14401:-1;
            if(n.equals("getNote"))return id==8779?799:-1;
            if(n.equals("getLinkedNoteId"))return 8778;
            if(n.equals("isGeTradeable"))return true;
            return null;
        });
        List<Supplies.Snapshot> received=new ArrayList<>();
        LiveSupplies live=new LiveSupplies(client,defs,id->500L,()->false,supplies,received::add);
        live.tick();Supplies.Snapshot last=received.get(received.size()-1);
        assertEquals(0,last.bankAt);assertEquals(Map.of(8778,2L),last.inventory);
        open[0]=true;live.tick();last=received.get(received.size()-1);assertEquals(Long.valueOf(10),last.bank.get(8778));
        // Both container changes are combined into one published end-of-tick snapshot.
        int previous=received.size(); bank[0]=container(new Item(8778,7));inv[0]=container(new Item(8779,5));
        live.containerChanged(new ItemContainerChanged(InventoryID.BANK,bank[0]));
        live.containerChanged(new ItemContainerChanged(InventoryID.INV,inv[0]));
        assertEquals(previous,received.size());live.tick();assertEquals(previous+1,received.size());
        last=received.get(received.size()-1);assertEquals(12,last.bank.get(8778)+last.inventory.get(8778));
        open[0]=false;account[0]=11;inv[0]=container();live.tick();last=received.get(received.size()-1);
        assertEquals(0,last.bankAt);assertTrue(last.bank.isEmpty());assertTrue(last.inventory.isEmpty());
        live.stateChanged(GameState.LOGIN_SCREEN);last=received.get(received.size()-1);assertFalse(last.inventoryKnown);
    }
}
