import json, urllib.request, sys
from pathlib import Path
inventory = Path('apps/backend/build/dependency-inventory.json')
if not inventory.exists():
    sys.exit('Run apps/backend/gradlew dependencyInventory first.')
deps = json.loads(inventory.read_text(encoding='utf-8'))
request = urllib.request.Request('https://api.osv.dev/v1/querybatch', data=json.dumps({'queries': [
    {'package': {'name': d['name'], 'ecosystem': 'Maven'}, 'version': d['version']} for d in deps
]}).encode(), headers={'Content-Type': 'application/json'})
try:
    results = json.load(urllib.request.urlopen(request, timeout=60))['results']
except Exception as error:
    sys.exit('OSV audit unavailable: ' + type(error).__name__)
report = [dict(d, vulnerabilities=[v['id'] for v in r.get('vulns', [])]) for d, r in zip(deps, results) if r.get('vulns')]
inventory.with_name('security-audit.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({'packagesChecked': len(deps), 'affectedPackages': report}, indent=2))
sys.exit(1 if report else 0)
