# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from json                import loads
from Crypto.Hash         import MD5
from Crypto.Cipher       import AES
from Crypto.Util.Padding import unpad
from base64              import b64decode

def decrypt_aes_with_custom_kdf(crypted_data, password) -> str:
    """
    AES/CBC/PKCS5Padding şifreleme şemasını kullanarak şifre çözme işlemi yapar.

    :param crypted_data: JSON formatında şifrelenmiş veri (ct, iv, s içerir).
    :param password: Anahtar türetmede kullanılacak şifre.
    :return: Çözülmüş veri (string olarak).
    """

    def generate_key_and_iv(password, salt, key_length=32, iv_length=16, iterations=1):
        """Anahtar ve IV oluşturmak için bir KDF fonksiyonu."""
        d = d_i = b""
        while len(d) < key_length + iv_length:
            d_i = MD5.new(d_i + password + salt).digest()
            for _ in range(1, iterations):
                d_i = MD5.new(d_i).digest()
            d += d_i
        return d[:key_length], d[key_length : key_length + iv_length]

    def hex_to_bytes(hex_str):
        """Hex string'i byte array'e çevirir."""
        return bytes.fromhex(hex_str)

    data      = loads(crypted_data)

    key, iv   = generate_key_and_iv(password, hex_to_bytes(data["s"]), iv_length=len(data["iv"]) // 2)

    cipher    = AES.new(key, AES.MODE_CBC, iv)
    decrypted = unpad(cipher.decrypt(b64decode(data["ct"])), AES.block_size)

    return decrypted.decode("utf-8")

from Kekik.cli    import konsol
from cloudscraper import CloudScraper
from parsel       import Selector
from re           import search

oturum = CloudScraper()
istek  = oturum.get("https://cizgimax.online/doraemon-1-sezon-13-bolum-izle-2")

secici  = Selector(istek.text)
linkler = secici.css("ul.linkler li a::attr(data-frame)").getall()

for link in linkler:
    konsol.log(link)
    oturum.headers.update({"Referer": "https://cizgimax.online/"})
    istek = oturum.get(link)

    be_player = search(r"bePlayer\('([^']+)',\s*'(\{[^\}]+\})'\);", istek.text).groups()
    konsol.print(be_player)

    be_player_pass = be_player[0]
    be_player_data = be_player[1]

    encrypted = decrypt_aes_with_custom_kdf(be_player_data, be_player_pass.encode()).replace("\\", "")
    konsol.print(loads(encrypted))
