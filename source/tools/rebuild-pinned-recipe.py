#!/usr/bin/env python3
"""Rebuild the freestanding ARM payload using reviewed 260507 recipe stock metadata.

The template contains stock window hashes and the verified library target table.
Only segment bytes, payload-relative relocations and branch destinations change.
No OEM library is distributed or needed in CI. Optional --stock validates metadata
against a privately held exact firmware library. No changes to stock patch sites.
"""
import argparse
import hashlib
import os
import struct
import tempfile
from pathlib import Path
from elftools.elf.elffile import ELFFile
from build import R, ARM, SEG_VA, STOCK_MD5, SITES, SETUP_LOG, TABLE, run, plt_targets

TARGETS = (
    0x91fd, 0x9c25, 0x12437, 0x200c8, 0x1fb20, 0x1fb1c, 0x1fb24,
    0x60e4, 0x60f0, 0x5c4c, 0x6168, 0x5d18, 0x69fc, 0x5d60, 0x6120,
    0x5ea4, 0x60d8, 0x6114, 0x6750, 0x678c, 0x5c64, 0x5e98, 0x5d00,
    0x5d84, 0x5d24, 0x5e2c, 0x5d54, 0x5eb0, 0x1171d, 0x11725,
    0xf015, 0xd59d, 0x5d3c, 0xaf19, 0xb6d1, 0xaf6d, 0x62e8, 0x62f4,
    0x5dcc, 0x65ac, 0x65b8, 0x5e14, 0x5dfc, 0x65c4, 0x5d90, 0x6234, 0x6618,
)
def require(ok, message):
    if not ok:
        raise SystemExit(message)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('template',type=Path)
    ap.add_argument('output',type=Path)
    ap.add_argument('--stock',type=Path)
    args=ap.parse_args()
    records=[l.split() for l in args.template.read_text().splitlines()
             if l.strip() and not l.lstrip().startswith('#')]
    require(all(x[0] in ('id','file','verify','segment','rel32','branch','patch') for x in records),'unexpected directive')
    group=lambda op:[x for x in records if x[0]==op]
    require(group('id')==[['id','carplay-climate-m1']] and group('file')==[['file','/system/lib/libCarPlayCore.so']],'wrong recipe')
    windows=group('verify')
    expected_windows=((0x9c18,20),(0xaf08,20),(0x10eec,17),(0x11710,20),(0x12428,20),(0x12542,17),(0x1255c,17),(0x12578,17))
    require(tuple((int(x[1],0),int(x[2],0)) for x in windows)==expected_windows,'unexpected stock verification windows')
    require(len(group('segment'))==1 and int(group('segment')[0][1],0)==SEG_VA,'unexpected segment')
    rel=group('rel32')
    require(len(rel)==len(TABLE)==len(TARGETS),'unexpected target table size')
    old_anchor=int(rel[0][2],0)
    require(all(int(x[1],0)==old_anchor+4*i and int(x[2],0)==old_anchor and int(x[3],0)==TARGETS[i] for i,x in enumerate(rel)),'target table changed')
    require(all(not isinstance(t,int) or t==TARGETS[i] for i,t in enumerate(TABLE)),'numeric table mismatch')
    sites={at:stub for at,_,stub in SITES}
    require(len(group('branch'))==len(sites) and {int(x[1],0) for x in group('branch')}==set(sites),'branch sites changed')
    patches={int(x[1],0)+i:v for x in group('patch') for i,v in enumerate(bytes.fromhex(x[2]))}
    expected={at+i:new for at,before,after in SETUP_LOG for i,(old,new) in enumerate(zip(bytes.fromhex(before),bytes.fromhex(after))) if old!=new}
    require(patches==expected,'stock byte patches changed')
    if args.stock:
        stock=args.stock.read_bytes()
        require(hashlib.md5(stock).hexdigest()==STOCK_MD5,'stock MD5 mismatch')
        targets=plt_targets(stock)
        require(tuple(targets[t] if isinstance(t,str) else t for t in TABLE)==TARGETS,'stock PLT table mismatch')
        for _,off,length,before,after in windows:
            lo,size=int(off,0),int(length,0)
            raw=stock[lo:lo+size]
            require(hashlib.md5(raw).hexdigest()==before,'stock verify mismatch '+off)
            if after!='-':
                patched=bytearray(raw)
                for at,value in patches.items():
                    if lo<=at<lo+size: patched[at-lo]=value
                require(hashlib.md5(patched).hexdigest()==after,'stock after mismatch '+off)
        for at,before,_ in SITES+SETUP_LOG:
            require(stock[at:at+len(bytes.fromhex(before))]==bytes.fromhex(before),'stock hook site mismatch')
    with tempfile.TemporaryDirectory() as temp:
        out=Path(temp); nat=R/'native'
        clang=os.environ.get('CLANG','clang'); lld=os.environ.get('LLD','ld.lld')
        run(clang,*ARM,'-I',nat,f'-DVSPI_FILE="{R / "data/vspi.bplist"}"','-c',nat/'seg.S','-o',out/'seg.o')
        run(clang,*ARM,'-I',nat,'-c',nat/'hooks.c','-o',out/'hooks.o')
        (out/'seg.ld').write_text(f"""ENTRY(cpc_stub_info)
SECTIONS {{
  . = {SEG_VA:#x};
  .seg : {{ *(.text.entry) *(.text .text.*) *(.rodata .rodata.*) }}
  /DISCARD/ : {{ *(.ARM.exidx*) *(.ARM.attributes) *(.comment) *(.note*) }}
}}
""")
        run(lld,'-static','--no-dynamic-linker','-T',out/'seg.ld','-o',out/'seg.elf',out/'seg.o',out/'hooks.o')
        with (out/'seg.elf').open('rb') as f:
            elf=ELFFile(f)
            require([s.name for s in elf.iter_sections() if s['sh_flags']&2]==['.seg'],'unexpected allocated section')
            require(not any(s.name.startswith('.rel') for s in elf.iter_sections()),'unresolved relocations')
            symbols=list(elf.get_section_by_name('.symtab').iter_symbols())
            require(not any(s.name and s['st_shndx']=='SHN_UNDEF' for s in symbols),'undefined symbol')
            sym={s.name:s['st_value'] for s in symbols if s.name}
            seg=elf.get_section_by_name('.seg')
            require(seg['sh_addr']==SEG_VA,'segment address mismatch')
            code=bytearray(seg.data())
        table=sym['cpc_table']-SEG_VA
        require(table>=0 and table%4==0 and table+4*len(TARGETS)<=len(code),'invalid table')
        require(code[table:table+4*len(TARGETS)]==bytes(4*len(TARGETS)),'nonzero table or layout drift')
        for i,target in enumerate(TARGETS):
            struct.pack_into('<i',code,table+4*i,target-(SEG_VA+table))
        lines=['# Rebuilt from CPUL source using pinned 260507 stock metadata (MD5 '+STOCK_MD5+')']
        lines += [' '.join(x) for x in group('id')+group('file')+windows]
        lines.append(f'segment {SEG_VA:#x} {code.hex()}')
        lines += [f'rel32 {table+4*i:#x} {table:#x} {target:#x}' for i,target in enumerate(TARGETS)]
        for at,stub in sites.items():
            dest=(sym[stub]&~1)-SEG_VA
            require(0<=dest<len(code) and dest%2==0,'invalid branch destination')
            lines.append(f'branch {at:#x} {dest:#x}')
        lines += [' '.join(x) for x in group('patch')]
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text('\n'.join(lines)+'\n')
        print(f'Compiled {len(code)} ARM payload bytes; {len(TARGETS)} relocations; {len(sites)} branches')
if __name__=='__main__':
    main()
