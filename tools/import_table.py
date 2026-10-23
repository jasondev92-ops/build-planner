"""Import the supplied OSRS Wiki Construction experience table facts.
All material-bearing rows are represented. Totals are accounted for by chains.
No game images or item IDs are guessed from fuzzy name matches.
"""
import json,re,collections,copy
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def key(n):return re.sub('[^a-z0-9]+','-',n.lower()).strip('-')
def norm(n):return re.sub('[^a-z0-9]','',n.lower())
def clean(n):return re.sub(r' \(Upgrade(?: - materials)?\)$','',n)
def expand(base):
 rows=json.loads((ROOT/'tools/construction-table.json').read_text())
 definitions=json.loads((ROOT/'tools/item-name-definitions.json').read_text())
 icons=json.loads((ROOT/'tools/icon-overrides.json').read_text())
 aliases={'keg':'Keg (facility)','innoculation-station':'Innoculation station','salvaging-station':'Salvaging station (facility)','quest-list':'Quest list (Construction)','anvil-amenity':'Anvil|Anvil (amenity) (Built|Anvil)'}
 base_index={norm(aliases.get(b['id'],b['name']+(' ('+b['boat']+')' if b['boat']!='Any' else ''))):b for b in base}
 name_counts=collections.Counter(clean(x['name']) for x in rows if '(Total)' not in x['name'] and x['materials'])
 result=[]; audit=[]; corrections=[]; deps={}; by_name={}; used=set()
 for row in rows:
  name=clean(row['name'])
  if not row['materials']:
   audit.append({'row':row['row'],'name':row['name'],'status':'Excluded: activity without a material recipe'});continue
  if '(Total)' in name:
   audit.append({'row':row['row'],'name':row['name'],'status':'Total: represented by earlier-upgrade chain'});continue
  original=base_index.get(norm(name));r=copy.deepcopy(original) if original else {}
  ingredients={}
  for value in row['materials']:
   amount,material=value.split(' × ',1);ingredients[material]=ingredients.get(material,0)+int(amount.replace(',',''))
  dependency=None
  sail_base=ingredients.pop('Plain sail',None)
  if ' (Upgrade' in row['name']:
   dependency=next(iter(ingredients));assert ingredients.pop(dependency)==1
  elif any('display' in m.lower() for m in ingredients) and ('mounted' in name.lower() or name.startswith('Mounted ')):
   dependency=next(m for m in ingredients if 'display' in m.lower());assert ingredients.pop(dependency)==1
  boat=re.search(r' \((Raft|Skiff|Sloop)\)$',name)
  label=name[:boat.start()] if boat else name
  if name_counts[name]>1:
   variant=(dependency+'; '+', '.join(ingredients)) if dependency else next((m for m in ingredients if 'blueprint' in m.lower()),None) or ' + '.join(ingredients)
   label+=' — '+variant
  ident=r.get('id','table-'+key(name)+(('-'+key((dependency or '')+'-'+'-'.join(ingredients))) if name_counts[name]>1 else ''))
  assert ident not in used,ident
  used.add(ident)
  # POH generic nails can use any metal; preserve the already reviewed mappings for old entries.
  if original and 'Nails (any metal)' in original['materials'] and 'Steel nails' in ingredients:
   ingredients['Nails (any metal)']=ingredients.pop('Steel nails')
  icon=r.get('iconItemId',0);symbol=r.get('iconSymbol','')
  if not icon:
   matches=definitions.get(norm(label),[]) or definitions.get(norm(name),[])
   poh=[x for x in matches if x[1].startswith('POH_') and 'FLATPACK' not in x[1]]
   chosen=poh[0] if len(poh)==1 else matches[0] if len(matches)==1 else None
   if name in icons:chosen=icons[name]
   if chosen:icon,symbol=chosen
  sailing='Sailing' in row['skills'] or 'Boat schematics'==row['facilities'] or 'Shipwright' in row['facilities'] or '(amenity)' in name
  category=r.get('category', 'Sailing' if sailing else 'House' if symbol.startswith('POH_') else 'Construction')
  notes=r.get('notes','')
  if not original:
   notes='Materials are for one listed build or crafting action. Crafted components are not expanded automatically. Check the source for output quantities, quests, room and hotspot restrictions.'
  if sail_base:notes+=' Requires an existing sail of any colour; the plain sail is a cosmetic state, not a bank item.'
  if original:
   old=(r['construction'],r['sailing'],r['materials'])
   new=(int(row['con'][0]),row['skills'].get('Sailing',0),ingredients)
   if old!=new:corrections.append({'id':ident,'before':old,'after':new,'row':row['row']})
  r.update(id=ident,name=r.get('name',label),category=category,boat=boat.group(1) if boat else 'Any',construction=int(row['con'][0]),sailing=row['skills'].get('Sailing',0),materials=ingredients,previous=None,checked='2026-10-23',notes=notes,iconItemId=icon,iconSymbol=symbol,sourceRow=row['row'],tools=row['tools'],facility=row['facilities'],otherSkills={k:v for k,v in row['skills'].items() if k!='Sailing'},iconStatus='Mapped to named game definition' if icon else 'Image not yet verified')
  if not original:
   r['source']=row['url'];r['description']='Build or make '+label+'.'+(' Requires '+row['facilities']+'.' if row['facilities'] else ' See the linked item page for its uses and placement requirements.')
  result.append(r);by_name.setdefault(norm(name),[]).append(r)
  if dependency:deps[ident]=dependency
  audit.append({'row':row['row'],'name':row['name'],'status':'Included','id':ident})
 for r in result:
  if r['id'] in deps:
   candidates=by_name.get(norm(deps[r['id']]),[])
   assert len(candidates)==1,(r['id'],deps[r['id']],len(candidates))
   r['previous']=candidates[0]['id']
 # All old saved IDs must remain resolvable.
 assert set(b['id'] for b in base).issubset(used),set(b['id'] for b in base)-used
 (ROOT/'tools/table-audit.json').write_text(json.dumps(audit,indent=2)+'\n')
 (ROOT/'tools/table-corrections.json').write_text(json.dumps(corrections,indent=2)+'\n')
 return result
