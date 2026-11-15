from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root=Path.cwd()
files=[root/p for p in ['settings.gradle','build.gradle','gradle.properties','.gitignore','gradlew','gradlew.bat','README.md','app/build.gradle','artifacts/VERIFICATION.md']]
files += list((root/'app/src').rglob('*'))
files += list((root/'gradle/wrapper').rglob('*'))
files += list((root/'docs').rglob('*'))
archive=root/'artifacts/Pockie-Java-Source.zip'
with ZipFile(archive,'w',ZIP_DEFLATED) as z:
    for f in files:
        if f.is_file():z.write(f,f.relative_to(root))
print('Source archive:',archive.stat().st_size,'bytes')
