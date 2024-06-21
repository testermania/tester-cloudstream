# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from Kekik.cli import konsol
from httpx     import Client as Session
from parsel    import Selector
from re        import findall
import base64, json

def atob(s:str) -> str:
    return base64.b64decode(s).decode("utf-8")

def rtt(s:str) -> str:
    def rot13_char(c):
        if "a" <= c <= "z":
            return chr((ord(c) - ord("a") + 13) % 26 + ord("a"))
        elif "A" <= c <= "Z":
            return chr((ord(c) - ord("A") + 13) % 26 + ord("A"))
        return c

    return "".join(rot13_char(c) for c in s)

def rapid2m3u8(url:str) -> str:
    oturum = Session()
    oturum.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"})

    istek       = oturum.get(url)
    escaped_hex = findall(r'file": "(.*)",', istek.text)[0]

    return bytes.fromhex(escaped_hex.replace("\\x", "")).decode("utf-8")

def trstx2m3u8(url:str) -> list[dict]:
    oturum = Session()
    oturum.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36", "Referer":"https://www.fullhdfilmizlesene.pw/"})

    istek     = oturum.get(url)
    file      = findall(r"file\":\"([^\"]+)", istek.text)[0]
    post_link = file.replace("\\", "")

    post_istek = oturum.post(f"https://trstx.org/{post_link}").json()

    veriler = []
    for bak in post_istek[1:]:
        vid_url = "https://trstx.org/playlist/" + bak.get("file")[1:] + ".txt"
        veriler.append({bak.get("title") : oturum.post(vid_url).text})

    return veriler

def fullhdfilmizlesene(url:str) -> list:
    oturum = Session()
    oturum.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"})

    konsol.print(f"\n\n{url}")
    istek  = oturum.get(url)
    secici = Selector(istek.text)

    script   = secici.xpath("(//script)[1]").get()
    scx_data = json.loads(findall(r'scx = (.*?);', script)[0])
    scx_keys = list(scx_data.keys())

    link_list = []
    for key in scx_keys:
        t = scx_data[key]["sx"]["t"]
        if isinstance(t, list):
            link_list.append({key: atob(rtt(elem)) for elem in t})
        if isinstance(t, dict):
            link_list.append({k: atob(rtt(v)) for k, v in t.items()})

    vid_links = []
    for elem in link_list:
        for key, value in elem.items():
            if "rapidvid" in value:
                vid_links.append({key: rapid2m3u8(value)})
            if "trstx.org" in value:
                vid_links.append({key: trstx2m3u8(value)})
            if "proton" in key:
                vid_links.append({key: value})

    return vid_links

# konsol.print(fullhdfilmizlesene("https://www.fullhdfilmizlesene.pw/film/hizli-ve-ofkeli-10-fast-x-fhd4/"))
konsol.print(fullhdfilmizlesene("https://www.fullhdfilmizlesene.pw/film/makine-2/"))
konsol.print(fullhdfilmizlesene("https://www.fullhdfilmizlesene.pw/film/bula-izle-1/"))
konsol.print(fullhdfilmizlesene("https://www.fullhdfilmizlesene.pw/film/cilgin-cocuklar-oyun-bitti-izle-1/"))
