"""Hesap Ayarları'ndaki fakülte/bölüm seçeneklerini TSV olarak yazar (Combos.java girdisi)."""
import re, sys
s = open(sys.argv[1], encoding="utf-8").read()
def strs(block): return re.findall(r'"([^"]+)"', block)
fac_block = s[s.index("val facultyOptions = listOf("):]
faculties = strs(fac_block[:fac_block.index(")")])
i = s.index("val iibfDepartmentOptions")
iibf = strs(s[i:s.index(")", i)])
d = s.index("val departmentOptions = when (state.faculty) {")
block = s[d:s.index("\n    }\n", d)]
mapping = {m.group(1): strs(m.group(2)) for m in re.finditer(r'"([^"]+)" -> listOf\(([^)]*)\)', block)}
for f in faculties:
    for dep in mapping.get(f, iibf):
        print(f + "\t" + dep)
