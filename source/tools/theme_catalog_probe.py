#!/usr/bin/env python3
"""Standalone metadata probe, based on LIVI theme_catalog.rs (f84638ed).
No device credentials, no firmware changes, no asset redistribution.
"""
import base64, hashlib, io, json, pathlib, re, ssl, urllib.request, zipfile

OUT = pathlib.Path("out/theme-catalog")
OUT.mkdir(parents=True, exist_ok=True)
CATALOG = "https://gdmf.apple.com/v2/assets"
ROOT = "https://www.apple.com/appleca/AppleIncRootCertificate.cer"
ROOT_HASH = "b0b1730ecbc7ff4505142c49f1295e6eda6bcaed7e2c68c5be91b5a11001f024"
ASSET, VERSION = "44C9FA31EC", 985
ASK = {
    "AssetAudience": "0c88076f-c292-4dad-95e7-304db9d29d34",
    "AssetType": "com.apple.MobileAsset.CarDDPAssets",
    "ClientVersion": 2, "ProductType": "iPhone17,1",
    "BuildVersion": "24A446", "ProductVersion": "27.0",
    "HWModelStr": "D93AP", "DeviceName": "iPhone",
    "ClientData": {"AllowXmlFallback": False, "DeviceAccessClient": "softwareupdated"},
}
def save(name, obj):
    (OUT / name).write_text(json.dumps(obj, indent=2) + "\n")
def fetch(req, context=None, limit=8*1024*1024):
    with urllib.request.urlopen(req, context=context, timeout=45) as response:
        data = response.read(limit + 1)
        if len(data) > limit:
            raise RuntimeError("response exceeds diagnostic limit")
        return data

class RemoteZip(io.RawIOBase):
    """Seekable HTTP range reader; zipfile validates member CRCs."""
    def __init__(self, url):
        self.url, self.pos = url, 0
        with urllib.request.urlopen(urllib.request.Request(url, headers={"Range":"bytes=-65557","Accept-Encoding":"identity"}), timeout=45) as r:
            match = re.fullmatch(r"bytes (\d+)-(\d+)/(\d+)", r.headers.get("Content-Range",""))
            if r.status != 206 or not match:
                raise RuntimeError("asset server does not support ranges")
            self.start, end, self.size = map(int, match.groups())
            self.tail = r.read(65558)
            if len(self.tail) != end-self.start+1 or end != self.size-1:
                raise RuntimeError("invalid ZIP tail")
    def readable(self): return True
    def seekable(self): return True
    def tell(self): return self.pos
    def seek(self, offset, whence=0):
        self.pos = offset if whence == 0 else self.pos+offset if whence == 1 else self.size+offset
        if self.pos < 0: raise ValueError("negative offset")
        return self.pos
    def read(self, n=-1):
        n = self.size-self.pos if n<0 else min(n,self.size-self.pos)
        if n<=0: return b""
        if n>32*1024*1024: raise RuntimeError("range exceeds diagnostic limit")
        start,end=self.pos,self.pos+n-1
        if start>=self.start:
            data=self.tail[start-self.start:start-self.start+n]
        else:
            req=urllib.request.Request(self.url,headers={"Range":f"bytes={start}-{end}","Accept-Encoding":"identity"})
            with urllib.request.urlopen(req,timeout=45) as r:
                if r.status!=206 or r.headers.get("Content-Range")!=f"bytes {start}-{end}/{self.size}":
                    raise RuntimeError("incorrect range response")
                data=r.read(n+1)
        if len(data)!=n: raise RuntimeError("short range")
        self.pos+=n
        return data

def walk(obj,path=""):
    if isinstance(obj,dict):
        for k,v in obj.items():
            p=f"{path}/{k}"
            if re.search("midnight|wallpaper|palette|default",str(k),re.I):
                print(p, json.dumps(v,ensure_ascii=False)[:350])
            walk(v,p)
    elif isinstance(obj,list):
        for i,v in enumerate(obj): walk(v,f"{path}/{i}")
    elif isinstance(obj,str) and re.search("midnight|wallpaper",obj,re.I):
        print(path,repr(obj)[:350])

def main():
    der=fetch(ROOT)
    if hashlib.sha256(der).hexdigest()!=ROOT_HASH: raise RuntimeError("Apple root hash mismatch")
    ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    ctx.load_verify_locations(cadata=ssl.DER_cert_to_PEM_cert(der))
    req=urllib.request.Request(CATALOG,data=json.dumps(ASK).encode(),headers={"Content-Type":"application/json"})
    token=fetch(req,ctx).decode()
    part=token.split(".")[1]
    catalog=json.loads(base64.urlsafe_b64decode(part+"="*(-len(part)%4)))
    assets=catalog["Assets"]
    versions=[a.get("_ContentVersion") for a in assets if a.get("ContentIdentifier")==ASSET]
    save("catalog-summary.json",{"asset":ASSET,"requested_version":VERSION,"versions":versions,"asset_count":len(assets)})
    print("Matching asset versions:",versions,flush=True)
    asset=next((a for a in assets if a.get("ContentIdentifier")==ASSET and a.get("_ContentVersion")==VERSION),None)
    if asset is None: raise RuntimeError("exact asset version absent; no substitution")
    url=asset["__BaseURL"]+asset["__RelativePath"]
    with zipfile.ZipFile(RemoteZip(url)) as z:
        names=z.namelist()
        save("zip-index.json",names)
        wanted=["AssetData/CarPlayPhone.assets/package.json","AssetData/CarPlayPhone.assets/theme.json"]
        for name in wanted:
            info=z.getinfo(name)
            if info.file_size>8*1024*1024: raise RuntimeError("metadata exceeds limit")
            obj=json.loads(z.read(info))
            save(pathlib.Path(name).name,obj)
            print("\nMetadata:",name)
            walk(obj)
        print("\nWallpaper-related filenames:")
        for name in names:
            if re.search("wallpaper|midnight",name,re.I): print(name)
if __name__=="__main__":
    main()
