#!/usr/bin/env python3
"""Conservative cross-recipe audit of stock file offsets, not runtime addresses."""
import pathlib
import sys

def parse(path):
    verifies, writes = [], []
    for raw in pathlib.Path(path).read_text().splitlines():
        fields = raw.split()
        if not fields or fields[0].startswith('#'):
            continue
        op = fields[0]
        if op == 'verify':
            start = int(fields[1], 0)
            verifies.append((start, start + int(fields[2], 0), op))
        elif op in ('patch', 'branch'):
            start = int(fields[1], 0)
            # Reserve four bytes for a Thumb-2 branch; segment/rel32 refer to
            # the separately mapped payload, not the stock file offset space.
            size = len(bytes.fromhex(fields[2])) if op == 'patch' else 4
            writes.append((start, start + size, op))
        elif op not in ('id','file','native','segment','rel32'):
            raise ValueError(f'{path}: unrecognized recipe directive {op}')
    if not verifies or not writes:
        raise ValueError(f'{path}: empty verification or write set')
    return verifies, writes

def overlap(a,b):
    return max(a[0],b[0]) < min(a[1],b[1])

def conflicts(a,b):
    av,aw = a
    bv,bw = b
    return [(x,y) for left,right in ((aw,bv),(bw,av),(aw,bw))
            for x in left for y in right if overlap(x,y)]

def main():
    assert overlap((0,4,'branch'),(3,5,'verify'))
    assert not overlap((0,4,'branch'),(4,8,'verify'))
    assert conflicts(([(0,8,'verify')],[(2,6,'patch')]), ([(4,12,'verify')],[(8,9,'patch')]))
    cpul = parse(sys.argv[1])
    for path in sys.argv[2:]:
        wcp = parse(path)
        hits = conflicts(cpul,wcp)
        if hits:
            raise SystemExit(f'{path}: cross-recipe collision {hits}')
        print(f'{path}: no stock-offset write/verify or write/write collision')

if __name__ == '__main__':
    main()
