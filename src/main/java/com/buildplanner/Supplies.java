package com.buildplanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.*;

/** Immutable client snapshots; all quantity/cost calculations can run without RuneLite. */
public final class Supplies
{
    public static final String ANY_NAILS = "Nails (any metal)";
    public final Map<String, int[]> items;
    public Supplies(Gson gson)
    {
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
            Supplies.class.getResourceAsStream("/com/buildplanner/material-items.json")), StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            Map<String,int[]> data = new TreeMap<>();
            json.entrySet().forEach(e -> data.put(e.getKey(), gson.fromJson(e.getValue(), int[].class)));
            items = Collections.unmodifiableMap(data);
        } catch (java.io.IOException e) { throw new IllegalStateException("Material mapping unavailable", e); }
    }
    public static final class Snapshot
    {
        public final Map<Integer,Long> bank, inventory, prices;
        public final Set<Integer> untradeable;
        public final boolean inventoryKnown;
        public final long bankAt, priceAt;
        public final String priceSource;
        public Snapshot(Map<Integer,Long> bank, Map<Integer,Long> inventory, boolean inventoryKnown,
            long bankAt, Map<Integer,Long> prices, Set<Integer> untradeable, long priceAt, String priceSource)
        {
            this.bank = Map.copyOf(bank); this.inventory = Map.copyOf(inventory); this.inventoryKnown = inventoryKnown;
            this.bankAt = bankAt; this.prices = Map.copyOf(prices); this.untradeable = Set.copyOf(untradeable);
            this.priceAt = priceAt; this.priceSource = priceSource;
        }
        public static Snapshot empty() { return new Snapshot(Map.of(), Map.of(), false, 0, Map.of(), Set.of(), 0, "RuneLite"); }
        public boolean countsKnown() { return bankAt > 0 && inventoryKnown; }
    }
    public static final class Row
    {
        public final String name, priceNote;
        public final long required, bank, inventory, owned, missing, unitPrice;
        public final boolean countsKnown;
        Row(String name, long required, long bank, long inventory, long owned, boolean known, long price, String note)
        {
            this.name=name; this.required=required; this.bank=bank; this.inventory=inventory; this.owned=owned;
            missing=Math.max(0,required-owned); countsKnown=known; unitPrice=price; priceNote=note;
        }
        public BigInteger cost(long quantity) { return BigInteger.valueOf(quantity).multiply(BigInteger.valueOf(unitPrice)); }
    }
    public List<Row> calculate(List<Planner.Material> needs, boolean manual, Snapshot snapshot)
    {
        Map<Integer,Long> remainingBank = new HashMap<>(snapshot.bank), remainingInv = new HashMap<>(snapshot.inventory);
        // Specific nail requirements reserve stock before the interchangeable nail row uses the leftovers.
        for (Planner.Material m : needs) {
            int[] ids=items.get(m.name);
            if (ids == null || ids.length != 1) continue;
            long take=Math.min(m.required,remainingBank.getOrDefault(ids[0],0L));
            remainingBank.put(ids[0],remainingBank.getOrDefault(ids[0],0L)-take);
            long rest=m.required-take;
            remainingInv.put(ids[0],Math.max(0,remainingInv.getOrDefault(ids[0],0L)-rest));
        }
        List<Row> rows = new ArrayList<>();
        for (Planner.Material m : needs) {
            int[] ids = items.get(m.name);
            long bank=0,inv=0,price=0; int priceId=0;
            boolean allUntradeable=ids!=null;
            if (ids != null) for (int id : ids) {
                bank += (ids.length>1 ? remainingBank : snapshot.bank).getOrDefault(id,0L);
                inv += (ids.length>1 ? remainingInv : snapshot.inventory).getOrDefault(id,0L);
                long candidate=id == 995 ? 1L : snapshot.prices.getOrDefault(id,0L);
                if (candidate>0 && (price==0 || candidate<price)) { price=candidate; priceId=id; }
                allUntradeable &= snapshot.untradeable.contains(id);
            }
            String note = price>0 ? "" : allUntradeable ? "Not sold on GE" : "Price unavailable";
            if (price>0 && ids.length>1) {
                final int selected=priceId;
                String metal=items.entrySet().stream().filter(e -> e.getValue().length==1 && e.getValue()[0]==selected)
                    .map(Map.Entry::getKey).findFirst().orElse("Black nails");
                note="Estimate uses " + metal.toLowerCase(Locale.ROOT);
            }
            rows.add(new Row(m.name,m.required,bank,inv,manual?m.owned:bank+inv,
                manual || (ids!=null && snapshot.countsKnown()),price,note));
        }
        return rows;
    }
    public static String gp(BigInteger n) { return String.format(Locale.US,"%,d",n) + " gp"; }
    public static String summary(List<Row> rows)
    {
        BigInteger full=BigInteger.ZERO,remaining=BigInteger.ZERO;
        int missingFullPrices=0,missingRemainingPrices=0; boolean countsKnown=true;
        for (Row r : rows) {
            if (r.unitPrice>0) full=full.add(r.cost(r.required)); else missingFullPrices++;
            countsKnown &= r.countsKnown;
            if (r.missing>0) {
                if (r.unitPrice>0) remaining=remaining.add(r.cost(r.missing)); else missingRemainingPrices++;
            }
        }
        return (missingFullPrices==0?"Full material estimate: ":"Priced materials subtotal: ")+gp(full)
            +(missingFullPrices>0?"\n"+missingFullPrices+" material(s) without GE prices":"")
            +"\n"+(!countsKnown?"Cost to finish: refresh bank/inventory":
                (missingRemainingPrices==0?"Estimated cost to finish: ":"Priced missing subtotal: ")+gp(remaining)
                +(missingRemainingPrices>0?"\n"+missingRemainingPrices+" missing material(s) unpriced":""));
    }
}
