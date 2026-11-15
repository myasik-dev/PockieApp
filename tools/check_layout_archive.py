from pathlib import Path
from zipfile import ZipFile
archive=Path('artifacts/Pockie-Java-Source.zip')
with ZipFile(archive) as z:
    layouts=[p for p in z.namelist() if p.startswith('app/src/main/res/layout/') and p.endswith('.xml')]
    assert len(layouts)==38
    assert 'docs/LAYOUTS.md' in z.namelist()
print('Verified source archive contains all 38 layouts and their index.')
