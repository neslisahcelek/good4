"""Yayımlanmış Google Sheets ders programını (pubhtml/sheet?...&gid=...) satırlara çevirir.
Birleştirilmiş hücrelerin (rowspan) süresini korur. Kullanım: python3 sheet_entries.py sheet.html "1st Year" "2nd Year" ..."""
import os, sys
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "gridlib.py"), encoding="utf-8").read())
grid = grid_of(sys.argv[1])
for year in sys.argv[2:]:
    print("==", year)
    for e in entries(grid, year):
        print(f"{e['day']}|{e['start']}|{e['end']}|{e['code']}|{e['name']}|{e['instr']}|{e['room']}")
