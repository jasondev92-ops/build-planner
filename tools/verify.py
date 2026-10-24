#!/usr/bin/env python3
"""Compile and test with Java 17 JRE + pinned ECJ, without a locally installed JDK.
Normal development should use the Gradle wrapper. This does not launch RuneLite.
"""
import hashlib,json,os,pathlib,subprocess,urllib.request,zipfile,shutil,sys
root=pathlib.Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'tools/check_table.py')],check=True)
build=root/'build'; cache=build/'verification-deps'; cache.mkdir(parents=True,exist_ok=True)
manifest=json.loads((root/'tools/verification-deps.json').read_text())
for item in manifest:
    path=cache/item['file']
    if not path.exists():
        print('Downloading',item['file'],flush=True)
        with urllib.request.urlopen(item['url'],timeout=60) as response:path.write_bytes(response.read())
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if digest!=item['sha256']:raise SystemExit('Checksum mismatch: '+str(path))
classpath=os.pathsep.join(str(cache/item['file']) for item in manifest if item['file']!='ecj.jar')
classes=build/'verified-classes'
if classes.exists(): shutil.rmtree(classes)
classes.mkdir(parents=True)
sources=sorted(str(p) for folder in ['src/main/java','src/test/java'] for p in (root/folder).rglob('*.java'))
subprocess.run(['java','-jar',str(cache/'ecj.jar'),'-11','-encoding','UTF-8','-proc:none','-classpath',classpath,'-d',str(classes),*sources],check=True)
runtime=os.pathsep.join([str(classes),str(root/'src/main/resources'),classpath])
subprocess.run(['java','-Djava.awt.headless=true','-cp',runtime,'org.junit.runner.JUnitCore','com.buildplanner.PlannerTest','com.buildplanner.SuppliesTest','com.buildplanner.FeaturesTest'],check=True)
subprocess.run(['java','-Djava.awt.headless=true','-cp',runtime,'com.buildplanner.PreviewRenderer',str(build/'previews')],check=True)
dist=root/'dist';dist.mkdir(exist_ok=True)
jar=dist/'build-planner-0.1.7-beta.jar'
production={p.stem for p in (root/'src/main/java/com/buildplanner').glob('*.java')}
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
    z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\r\nImplementation-Title: Build Planner\r\nImplementation-Version: 0.1.7-beta\r\n\r\n')
    for p in sorted(classes.rglob('*.class')):
        if p.stem.split('$')[0] in production:
            data=p.read_bytes();assert int.from_bytes(data[6:8],'big')==55
            z.writestr(str(p.relative_to(classes)),data)
    for p in sorted((root/'src/main/resources').rglob('*')):
        if p.is_file():z.write(p,str(p.relative_to(root/'src/main/resources')))
    for name in ['LICENSE','DATA-LICENSE.md','runelite-plugin.properties']:z.write(root/name,name)
(dist/'SHA256SUMS').write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+'  '+jar.name+'\n')
print('Compiled, tested and packaged:',jar)
