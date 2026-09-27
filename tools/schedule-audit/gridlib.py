import re
from html.parser import HTMLParser
class P(HTMLParser):
    def __init__(s):
        super().__init__(); s.rows=[]; s.cur=None; s.cell=None; s.in_table=False
    def handle_starttag(s,t,a):
        a=dict(a)
        if t=='table' and 'waffle' in (a.get('class') or ''): s.in_table=True
        if not s.in_table: return
        if t=='tr': s.cur=[]
        elif t=='td' and s.cur is not None:
            s.cell={'text':'','rs':int(a.get('rowspan',1)),'cs':int(a.get('colspan',1))}
        elif t=='br' and s.cell is not None: s.cell['text']+='\n'
    def handle_endtag(s,t):
        if not s.in_table: return
        if t=='td' and s.cell is not None: s.cur.append(s.cell); s.cell=None
        elif t=='tr' and s.cur is not None: s.rows.append(s.cur); s.cur=None
        elif t=='table': s.in_table=False
    def handle_data(s,d):
        if s.cell is not None: s.cell['text']+=d
def grid_of(path):
    p=P(); p.feed(open(path,encoding='utf-8').read())
    grid={}
    for r,row in enumerate(p.rows):
        c=0
        for cell in row:
            while (r,c) in grid: c+=1
            for dr in range(cell['rs']):
                for dc in range(cell['cs']):
                    grid[(r+dr,c+dc)] = (cell['text'].strip(), cell['rs']) if (dr==0 and dc==0) else None
            c+=cell['cs']
    return grid
DAYS=['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY']
TITLES=[('Arş.Gör.Dr.','Arş. Gör. Dr.'),('Öğr.Gör.Dr.','Öğr. Gör. Dr.'),('Dr.Öğr.Üyesi','Dr. Öğr. Üyesi'),('Prof.Dr.','Prof. Dr.'),('Doç.Dr.','Doç. Dr.'),('Öğr.Gör.','Öğr. Gör.'),('Arş.Gör.','Arş. Gör.')]
def tr_title(w):
    low=w.replace('I','ı').replace('İ','i').lower()
    return low[:1].replace('i','İ').replace('ı','I').upper() if low[:1] in 'iı' else low[:1].upper()
def tr_cap(w):
    turkish=any(ch in w for ch in 'ĞÜŞÖÇİ') or w in ('IŞIK','ILGIN','IRMAK','ILKAY')
    low=(w.replace('I','ı').replace('İ','i') if turkish else w.replace('İ','i')).lower()
    first=low[0]
    first={'i':'İ','ı':'I'}.get(first, first.upper())
    return first+low[1:]
def fmt_instr(s):
    s=s.strip()
    for a,b in TITLES:
        if s.startswith(a):
            rest=s[len(a):].strip().split()
            if not rest: return b
            return b+' '+' '.join([tr_cap(x) for x in rest[:-1]]+[rest[-1]])
    return s
def parse_course(txt):
    lines=[l.strip() for l in txt.split('\n') if l.strip()]
    code=lines[0] if lines else ''
    name=lines[1] if len(lines)>1 else ''
    rooms=[l[3:].strip() if l.startswith('CR:') else None for l in lines]
    room=' / '.join(x.strip(' /') for l in lines if 'CR:' in l for x in l.split('CR:')[1:] if x.strip(' /'))
    instr=[l for l in lines[2:] if 'CR:' not in l]
    return dict(code=code,name=name,room=room,instr=' / '.join(fmt_instr(i) for i in instr))
def cell(grid,r,c):
    v=grid.get((r,c)); return v
def entries(grid, year_label, off=None):
    rows=sorted({k[0] for k in grid}); cols=sorted({k[1] for k in grid})
    start=None; tc=None
    for r in rows:
        for c in cols:
            v=grid.get((r,c))
            if v and v[0].startswith(year_label): start=r
        if start is not None: break
    # header row with Saat
    r=start+1
    for c in cols:
        v=grid.get((r,c))
        if v and v[0].startswith('Saat'): tc=c
    out=[]; r=start+2
    while True:
        t=grid.get((r,tc))
        if not t or not re.match(r'\d\d:\d\d-\d\d:\d\d',t[0]): break
        s_,e_=t[0].split('-')
        for i in range(5):
            c=tc+1+i
            v=grid.get((r,c))
            if not v or not v[0] or 'Ö Ğ L E' in v[0]: continue
            txt,rs=v
            end=grid[(r+rs-1,tc)][0].split('-')[1]
            for part in re.split(r'\n?─+\n?',txt):
                if part.strip():
                    out.append(dict(day=DAYS[i],start=s_,end=end,**parse_course(part)))
        r+=1
    return out
