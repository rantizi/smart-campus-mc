import subprocess,json,re,sys
pdf=sys.argv[1]
items=[l.split("', ")[0].strip(" ['") for l in re.findall(r"\['([^']+)', [01]\]", open('laporanakhir.js').read())]
n=int(re.search(r'Pages:\s+(\d+)',subprocess.run(['pdfinfo',pdf],capture_output=True,text=True).stdout).group(1))
toc={}
for pg in range(3,n+1):
    t=subprocess.run(['pdftotext','-f',str(pg),'-l',str(pg),'-layout',pdf,'-'],capture_output=True,text=True).stdout
    for line in t.splitlines():
        for it in items:
            if it not in toc and line.strip()==it: toc[it]=pg
json.dump(toc,open('toc.json','w')); print(n,'halaman;', 'hilang:',[i for i in items if i not in toc]); print(toc)
