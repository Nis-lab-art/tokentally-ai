#!/bin/sh
# builds the files to upload: source as .java and .txt per member, a full zip, report and slides
cd "$(dirname "$0")/.."
python3 - <<'PY'
import glob, os, re, shutil
files = sorted(glob.glob('core/src/**/*.java', recursive=True) + glob.glob('api/src/**/*.java', recursive=True))
# api files are not java only, they go in as txt too
extra = ['api/src/main/resources/static/index.html', 'api/src/main/resources/application.properties', 'api/pom.xml']
for d in ('submission/source_java', 'submission/source_txt'):
    shutil.rmtree(d, ignore_errors=True)
by = {}
for f in files:
    owner = re.search(r'^// owner: (.+)$', open(f, encoding='utf-8').read(), re.M).group(1)
    by.setdefault(owner, []).append(f)
def folder(owner):
    return 'api_Abhinav_Nishan' if ',' in owner else owner.replace(' ', '_')
for owner, fs in by.items():
    for f in fs:
        name = os.path.basename(f)
        for d, target in (('source_java', name), ('source_txt', name[:-5] + '.txt')):
            os.makedirs('submission/%s/%s' % (d, folder(owner)), exist_ok=True)
            shutil.copy(f, 'submission/%s/%s/%s' % (d, folder(owner), target))
for f in extra:
    shutil.copy(f, 'submission/source_txt/api_Abhinav_Nishan/' + os.path.basename(f).replace('.', '_') + '.txt')
out = ['# source files by member', '', 'Made by `submission/build.sh` from the `// owner:` line of every file.', '']
for owner in sorted(by, key=lambda o: (',' in o, o)):
    out += ['## ' + (owner.replace(',', ' and') + ' (web api)' if ',' in owner else owner), '']
    out += ['- `%s`' % f for f in by[owner]]
    if ',' in owner:
        out += ['- `%s`' % f for f in extra]
    out.append('')
open('submission/FILES.md', 'w').write('\n'.join(out))
for o, fs in by.items():
    print(len(fs), o)
PY
rm -f submission/TokenTally_source.zip
# the zip takes the committed files, so commit before running this
git archive --format=zip -o submission/TokenTally_source.zip HEAD core api README.md
cp docs/report/TokenTally_AI_Report.docx submission/
[ -f docs/slides/TokenTally_AI_Presentation.pptx ] && cp docs/slides/TokenTally_AI_Presentation.pptx submission/
ls submission
