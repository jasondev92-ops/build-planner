"""Curated factual recipe data; see docs/SOURCES.md. No network access."""
import json,re,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
recipes=[]
def key(n):return re.sub('[^a-z0-9]+','-',n.lower()).strip('-')
def add(name,group,con,mats,sail=0,boat='Any',source=None,previous=None,notes=''):
 r=dict(id=key(name+(' '+boat if boat!='Any' else '')),name=name,category=group,construction=con,sailing=sail,boat=boat,materials=mats,source='https://oldschool.runescape.wiki/w/'+(source or name.replace(' ','_')),checked='2026-10-22',notes=notes,previous=key(previous) if previous else None)
 recipes.append(r);return r
woods=['Wooden','Oak','Teak','Mahogany','Camphor','Ironwood','Rosewood']
metals=['Bronze','Iron','Steel','Mithril','Adamant','Rune','Dragon']
planks=['Plank']+[w+' plank' for w in woods[1:]]
logs=['Logs']+[w+' logs' for w in woods[1:]]
nails=['Bronze nails','Iron nails','Steel nails','Mithril nails','Adamantite nails','Rune nails','Dragon nails']
bars=['Bronze bar','Iron bar','Steel bar','Mithril bar','Adamantite bar','Runite bar','Dragon metal sheet']
for b,minimum in [('Raft',1),('Skiff',15),('Sloop',50)]:
 for i,w in enumerate(woods):
  m={logs[i]:10,'Rope':6,'Swamp tar':10} if b=='Raft' else {(('Large '+w.lower()) if b=='Sloop' else w)+' hull parts':16 if b=='Sloop' else 10,nails[i]:600 if b=='Sloop' else 300,'Swamp tar':25 if b=='Sloop' else 20}
  if i>=2:m['Lead bar' if i<=4 else 'Cupronickel bar']=5
  add(w+' hull','Sailing', [1,8,23,41,59,75,84][i],m,max(minimum,[1,20,31,48,67,81,93][i]),b,'Hull',notes='Boat-specific quantities. Hull parts are purchased/pre-made inputs; their crafting materials are not expanded.')
  multiplier={'Raft':1,'Skiff':2,'Sloop':3}[b]
  cloth='linen' if i<2 else 'canvas' if i<5 else 'cotton'
  add(w+' mast and '+cloth+' sails','Sailing',[1,11,26,45,60,77,85][i],{logs[i]:5*multiplier,nails[i]:20*multiplier,'Bolt of '+cloth:10 if b=='Sloop' else 5},max(minimum,[1,24,36,52,68,83,94][i]),b,'Mast_and_sails')
  n={'Raft':2,'Skiff':3,'Sloop':4}[b]
  add(metals[i]+' helm','Sailing',[1,14,30,47,59,81,86][i],{planks[i]:n,bars[i]:n*2},max(minimum,[1,17,38,55,72,87,96][i]),b,'Helm',notes='Sailing requirement includes access to the selected boat.')
  if b!='Raft':
   m={(('Large '+metals[i].lower()) if b=='Sloop' else metals[i])+' keel parts':16 if b=='Sloop' else 10}
   if i>=2:m['Lead bar' if i<=4 else 'Cupronickel bar']=5
   add(metals[i]+' keel','Sailing',[1,17,32,50,62,78,87][i],m,max(minimum,[1,22,39,54,66,85,97][i]),b,'Keel',notes='Keel parts are purchased/pre-made inputs; their crafting materials are not expanded.')
for i,w in enumerate(woods):
 m={planks[i]:8,nails[i]:32}
 if i>=2:m['Lead bar' if i<=4 else 'Cupronickel bar']=3
 add(('Basic' if i==0 else w)+' cargo hold','Sailing',[1,11,21,41,53,77,84][i],m,[1,18,29,46,60,80,89][i],source='Boat_facility')
 m={planks[i]:4,nails[i]:16,bars[i]:6,'Rope':1}
 if 2<=i<=4:m['Lead bar']=3
 if i==5:m.update({'Lead bar':4,'Cupronickel bar':4})
 if i==6:m.update({'Cupronickel bar':4,'Broken dragon hook':1})
 add(metals[i]+' salvaging hook','Sailing',[1,9,18,30,52,66,78][i],m,[15,21,27,44,59,74,86][i],source='Salvaging_hook',notes='Active facility: a Sailing boost used to build it must be maintained to operate it. Check crew requirements separately.')
 m={planks[i]:4,nails[i]:16,bars[i]:8}
 if i==6:m['Dragon cannon barrel']=1
 add(metals[i]+' cannon','Sailing',[21,28,39,50,61,76,84][i],m,[28,35,47,57,69,80,92][i],source='Dragon_cannon' if i==6 else 'Cannon_(facility)',notes=('Read the dragon cannon schematic first. ' if i==6 else '')+'Operation has separate Ranged and crew requirements; see source.')
facilities=[
('Range (facility)',16,6,{'Steel bar':4,'Charcoal':2,'Tinderbox':1}),
('Keg',33,25,{'Oak plank':5,'Iron nails':20,'Barrel stand':1}),
('Innoculation station',40,37,{'Teak plank':8,'Steel nails':32,"Relicym\'s balm(4)":6}),
('Salvaging station',42,34,{'Teak plank':4,'Steel nails':16}),
('Chum station',56,45,{'Mahogany plank':10,'Mithril nails':40,'Steel bar':2,'Fishing bait':1000,'Knife':1}),
("Bosun's workbench",63,54,{'Camphor plank':6,'Adamantite nails':24,'Lead bar':5}),
('Advanced chum station',68,61,{'Camphor plank':10,'Adamantite nails':40,'Steel bar':2,'Fishing bait':1000,'Knife':1}),
('Chum spreader',82,74,{'Ironwood plank':10,'Rune nails':40,'Cupronickel bar':5,'Fishing bait':10000,'Narwhal horn knife':1}),
('Wind catcher',53,47,{'Teak plank':4,'Steel nails':16,'Steel bar':8,'Lead bar':4,'Air rune':10000,'Captured wind mote':1}),
('Crystal extractor',73,67,{'Ironwood plank':6,'Cupronickel bar':5,'Magic stone':2,'Heart of Ithell':1}),
('Gale catcher',79,70,{'Camphor plank':4,'Adamantite nails':16,'Adamantite bar':8,'Cupronickel bar':4,'Air rune':25000,'Captured wind mote':1,'Swift albatross feather':5}),
('Anchor (facility)',37,29,{'Steel bar':8,'Lead bar':6,'Rope':1}),
('Ballistic attractor',50,44,{'Mahogany plank':10,'Water rune':5000,'Law rune':1000,'Lead bar':5,'Steel bar':5}),
('Teleport focus (facility)',55,49,{'Mahogany plank':8,'Mithril nails':32,'Lead bar':4,'Magic stone':1}),
('Fathom stone',70,62,{'Camphor plank':10,'Adamantite nails':40,'Molten glass':4,'Cupronickel bar':2}),
('Greater teleport focus',75,69,{'Ironwood plank':8,'Rune nails':32,'Cupronickel bar':4,'Magic stone':2,'Bottled storm':1}),
('Fathom pearl',91,83,{'Rosewood plank':10,'Dragon nails':40,'Dragon metal sheet':2,'Echo pearl':1}),
('Rope trawling net',56,45,{'Rope':7,'Teak plank':4,'Steel bar':4,'Lead bar':2}),
('Linen trawling net',65,61,{'Linen yarn':6,'Rope':1,'Mahogany plank':4,'Mithril bar':4,'Lead bar':2}),
('Hemp trawling net',76,70,{'Hemp yarn':6,'Rope':1,'Camphor plank':4,'Adamantite bar':4,'Cupronickel bar':2,'Ray barbs':4}),
('Cotton trawling net',84,73,{'Cotton yarn':6,'Rope':1,'Ironwood plank':4,'Runite bar':4,'Cupronickel bar':2,'Ray barbs':8})]
for name,s,c,m in facilities:add(name,'Sailing',c,m,s,source='Trawling_net' if 'trawling net' in name else 'Boat_facility',notes='Build requirements only. Compatible hotspots, quests and operating/crew levels may add restrictions; see source.')
def house(n,c,m,src='Construction/Experience_table',prev=None,notes=''):
 return add(n,'House',c,m,source=src,previous=prev,notes=notes)
# Independent furniture verified from the Construction experience table.
simple='''Oak chair|19|2 Oak plank
Oak chest|79|5 Oak plank;2 Steel bar
Oak clock|25|2 Oak plank;1 Clockwork
Oak dining table|22|4 Oak plank
Oak display|38|2 Oak plank
Oak door|74|10 Oak plank
Oak drawers|27|2 Oak plank
Oak kitchen table|32|3 Oak plank
Oak ladder|68|5 Oak plank
Oak larder|33|8 Oak plank
Oak lectern|40|1 Oak plank
Oak lever|68|5 Oak plank
Oak trapdoor|68|5 Oak plank
Oak trophy case|36|6 Oak plank
Oak wall decoration|16|2 Oak plank
Oak wardrobe|39|3 Oak plank
4-poster|53|3 Mahogany plank;2 Bolt of cloth
Mahogany armchair|50|2 Mahogany plank
Mahogany bench|52|4 Mahogany plank
Mahogany bookcase|40|3 Mahogany plank
Mahogany chest|87|5 Mahogany plank;1 Gold leaf
Mahogany demon lectern|67|2 Mahogany plank;1 Gold leaf
Mahogany display (fishing trophy)|76|2 Mahogany plank
Mahogany display (head trophy)|58|2 Mahogany plank
Mahogany dresser|64|2 Mahogany plank;1 Molten glass
Mahogany eagle lectern|67|2 Mahogany plank;1 Gold leaf
Mahogany feeder|59|4 Mahogany plank;1 Gold leaf;1 Bucket of milk
Mahogany ladder|88|5 Mahogany plank
Mahogany lever|88|5 Mahogany plank
Mahogany outfit stand|74|4 Mahogany plank;1 Gold leaf
Mahogany portal|65|3 Mahogany plank
Mahogany prize chest|54|4 Mahogany plank;1 Gold leaf
Mahogany scratching post|59|2 Mahogany plank;1 Limestone brick;1 Rope
Mahogany table|52|6 Mahogany plank
Mahogany telescope|84|2 Mahogany plank;1 Molten glass
Mahogany throne|74|5 Mahogany plank;3 Marble block
Mahogany trapdoor|88|5 Mahogany plank
Mahogany trophy case|78|6 Mahogany plank;2 Gold leaf
Mahogany wardrobe|75|3 Mahogany plank
Hoop and stick|30|2 Oak plank
Imp statue|15|5 Limestone brick;5 Soft clay
Incense burner (Oak)|61|4 Oak plank;2 Steel bar
Incense burner (Mahogany)|65|4 Mahogany plank;2 Steel bar
Incense burner (Marble)|69|2 Marble block;2 Steel bar
Infernal chart|83|4 Bolt of cloth
Organ|69|4 Mahogany plank;6 Steel bar
Ornamental globe|50|3 Teak plank
Ornate banner stand|66|2 Marble block;1 Gold leaf
Ornate league statue|68|6 Marble block;1 Gold leaf
Gilded bench|61|4 Mahogany plank;4 Gold leaf
Gilded clock|85|2 Mahogany plank;1 Clockwork;1 Gold leaf
Gilded decoration|56|3 Mahogany plank;2 Gold leaf
Gilded display|78|2 Mahogany plank;2 Gold leaf
Gilded dresser|74|2 Mahogany plank;2 Molten glass;1 Gold leaf'''
for row in simple.splitlines():
 n,c,m=row.split('|');house(n,int(c),{s.split(' ',1)[1]:int(s.split(' ',1)[0]) for s in m.split(';')})
# Storage upgrades are dependencies, not purchasable ingredients.
for suffix,levels,qty in [('armour case',[46,64,82],3),('fancy dress box',[44,62,80],2),('treasure chest',[48,66,84],2),('cape rack',[54,63,72],4)]:
 prev=None
 for w,c in zip(['Oak','Teak','Mahogany'],levels):
  n=w+' '+suffix;house(n,c,{w+' plank':qty},'Costume_room',prev);prev=n
for w,c in zip(['Oak','Teak','Mahogany'],[50,68,86]):house(w+' toy box',c,{w+' plank':2},'Costume_room')
prev=None
for n,c,m in [('Oak magic wardrobe',42,{'Oak plank':4}),('Carved oak magic wardrobe',51,{'Oak plank':6}),('Teak magic wardrobe',60,{'Teak plank':4}),('Carved teak magic wardrobe',69,{'Teak plank':6}),('Mahogany magic wardrobe',78,{'Mahogany plank':4}),('Gilded magic wardrobe',87,{'Mahogany plank':4,'Gold leaf':1}),('Marble magic wardrobe',96,{'Marble block':1})]:
 house(n,c,m,'Costume_room',prev);prev=n
prev='Mahogany cape rack'
for n,c,m in [('Gilded cape rack',81,{'Mahogany plank':4,'Gold leaf':1}),('Marble cape rack',90,{'Marble block':1}),('Magical cape rack',99,{'Magic stone':1})]:
 house(n,c,m,'Costume_room',prev);prev=n
prev=None
for n,c,m in [('Restoration pool',65,{'Limestone brick':5,'Bucket of water':5,'Soul rune':1000,'Body rune':1000}),('Revitalisation pool',70,{'Stamina potion(4)':10}),('Rejuvenation pool',80,{'Prayer potion(4)':10}),('Fancy rejuvenation pool',85,{'Super restore(4)':10,'Marble block':2}),('Ornate rejuvenation pool',90,{'Anti-venom(4)':10,'Gold leaf':5,'Blood rune':1000})]:
 house(n,c,m,'Pool_space',prev);prev=n
prev=None
for n,c,m in [('Basic jewellery box',81,{'Bolt of cloth':1,'Steel bar':1,'Games necklace(8)':3,'Ring of dueling(8)':3}),('Fancy jewellery box',86,{'Gold leaf':1,'Skills necklace(4)':5,'Combat bracelet(4)':5}),('Ornate jewellery box',91,{'Gold leaf':2,'Amulet of glory(4)':8,'Ring of wealth (5)':8})]:
 house(n,c,m,'Jewellery_box_space',prev);prev=n
prev=None
for n,c,m in [('Marble portal nexus',72,{'Marble block':4}),('Gilded portal nexus',82,{'Marble block':4,'Gold leaf':2}),('Crystalline portal nexus',92,{'Magic stone':2,'Gold leaf':2})]:
 house(n,c,m,'Portal_nexus',prev,notes='Nexus frame only; room cost and teleport attunement runes are not included.');prev=n
for n,c,m in [('Oak altar',45,{'Oak plank':4}),('Teak altar',50,{'Teak plank':4}),('Cloth altar',56,{'Teak plank':4,'Bolt of cloth':2}),('Mahogany altar',60,{'Mahogany plank':4,'Bolt of cloth':2}),('Limestone altar',64,{'Mahogany plank':6,'Bolt of cloth':2,'Limestone brick':2}),('Marble altar',70,{'Marble block':2,'Bolt of cloth':2}),('Gilded altar',75,{'Marble block':2,'Bolt of cloth':2,'Gold leaf':4})]:house(n,c,m,'Altar_space')
for n,c,m,p in [('Wooden workbench',17,{'Plank':5,'Nails (any metal)':5},None),('Oak workbench',32,{'Oak plank':5},None),('Steel framed workbench',46,{'Oak plank':6,'Steel bar':4},None),('Bench with vice',62,{'Oak plank':2,'Steel bar':1},'Steel framed workbench'),('Bench with lathe',77,{'Oak plank':2,'Steel bar':1},'Bench with vice')]:house(n,c,m,'Workbench',p,notes='Nail quantities are minimums; bending can consume extras.' if n=='Wooden workbench' else '')
house('Crude wooden chair',1,{'Plank':2,'Nails (any metal)':2},'Construction',notes='Nail quantities are minimums; bending can consume extras.')
house('Quest list',80,{'Papyrus':10,'Gold leaf':1},'Achievement_gallery')
house("Mounted xeric's talisman",72,{'Mahogany plank':1,'Gold leaf':1,"Xeric's talisman (inert)":1,'Lizardman fang':5000},'Portal_nexus')
house('Mounted digsite pendant',82,{'Mahogany plank':1,'Gold leaf':1,"Curator's medallion":1},'Portal_nexus',notes="Requires obtaining the Curator's medallion; see source for unlocks.")
# Verified per-item cargo pages, including icon IDs, 22 October 2026.
capacities = [[20,30,40],[30,45,60],[45,60,90],[60,90,120],[80,120,160],[105,150,210],[120,180,240]]
for i, name in enumerate(['Basic','Oak','Teak','Mahogany','Camphor','Ironwood','Rosewood']):
 r = next(r for r in recipes if r['name'] == name+' cargo hold')
 r['source'] = 'https://oldschool.runescape.wiki/w/'+name+'_cargo_hold'
 r['description'] = 'Stores permitted supplies and cargo aboard your boat. Capacity depends on the ship type.'
 r['capacities'] = dict(zip(['Raft','Skiff','Sloop'],capacities[i]))
 r['iconItemId'] = 32213+i
 if name == 'Rosewood': r['notes'] = 'Requires reading the rosewood cargo hold schematic before building.'

# Reviewed descriptions, exact item page targets and game icon identifiers.
details=json.loads((root/'tools/item-details.json').read_text())
for r in recipes:
 r.update(details[r['id']])
recipes.extend(json.loads((root/'tools/additional-recipes.json').read_text()))
from import_table import expand
recipes=expand(recipes)
ids=[r['id'] for r in recipes];assert len(ids)==len(set(ids))
for r in recipes:
 assert r['materials'] and all(v>0 for v in r['materials'].values())
 assert r['previous'] is None or r['previous'] in ids
out=root/'src/main/resources/com/buildplanner/recipes.json'
out.write_text(json.dumps(recipes,indent=2)+'\n')
print(len(recipes),'recipes;',sum(r['category']=='Sailing' for r in recipes),'Sailing;',sum(r['category']=='House' for r in recipes),'House')
