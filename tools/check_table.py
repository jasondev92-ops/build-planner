"""Independent checks against the supplied table, including all upgrade totals."""
import collections,json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
recipes=json.loads((root/'src/main/resources/com/buildplanner/recipes.json').read_text())
rows=json.loads((root/'tools/construction-table.json').read_text())
audit=json.loads((root/'tools/table-audit.json').read_text())
byid={r['id']:r for r in recipes}; byrow={r['sourceRow']:r for r in recipes}
assert len(rows)==len(audit)==960
assert len(byid)==len(byrow)==len(recipes)==921
assert {r['row'] for r in rows}=={r['row'] for r in audit}
assert sum(not r['materials'] for r in rows)==6
assert sum('(Total)' in r['name'] for r in rows)==33

def expanded(r,seen=()):
    assert r['id'] not in seen,'Cyclic upgrade'
    result=collections.Counter({('Steel nails' if k=='Nails (any metal)' else k):v for k,v in r['materials'].items()})
    if r['previous']:result.update(expanded(byid[r['previous']],(*seen,r['id'])))
    return result

for row in rows:
    if '(Total)' not in row['name']:continue
    name=row['name'].replace(' (Total)','')
    matching=[x for x in rows if x['name'] in [name,name+' (Upgrade)',name+' (Upgrade - materials)'] and x['row'] in byrow]
    assert len(matching)==1,name
    expected=collections.Counter()
    for material in row['materials']:
        count,name=material.split(' × ',1);expected[name]+=int(count.replace(',',''))
    assert expanded(byrow[matching[0]['row']])==expected,row['name']
mapping=json.loads((root/'src/main/resources/com/buildplanner/material-items.json').read_text())
materials={m for r in recipes for m in r['materials']}
assert all(mapping.get(m) for m in materials)
assert len(materials)==326
print('960 source rows accounted for; all 33 upgrade totals match; all 326 material names mapped.')
