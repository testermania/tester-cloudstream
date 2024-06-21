# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from Kekik.cli    import konsol
from cloudscraper import CloudScraper
from parsel       import Selector
from re           import search

oturum = CloudScraper()
oturum.headers.update({"User-Agent": "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"})
# oturum.cookies.update({"wordpress_logged_in_7e0a80686bffd7035218d41e8240d65f": "keyiflerolsun|1704461004|TJh8nQRYrqZ9xlAyO7rO5QgiqTQiw7op8I6LKkSvytX|0dc654883fad5f0301df32e6465aa676a3f235288dd9c8a73260d9c8a20b19ae"})

oturum.post(
    url  = "https://www.dizimom.tv/wp-login.php",
    data = {
        "log"         : "keyiflerolsun",
        "pwd"         : "12345",
        "rememberme"  : "forever",
        "redirect_to" : "https://www.dizimom.tv",
    }
)

istek  = oturum.get("https://www.dizimom.tv/modern-dogu-masallari-1-sezon-7-bolum-izle/")
konsol.print(istek.url)

secici = Selector(istek.text)
iframe = secici.css("div.video p iframe::attr(src)").get()
konsol.print(iframe)

oturum.headers.update({"Referer": "https://www.dizimom.tv/"})
i_source = oturum.get(iframe).text

be_player = search(r"bePlayer\('([^']+)',\s*'(\{[^\}]+\})'\);", i_source).groups()
konsol.print(be_player)

be_player_pass = be_player[0]
be_player_data = be_player[1]


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


encrypted = decrypt_aes_with_custom_kdf(be_player_data, be_player_pass.encode()).replace("\\", "")
konsol.print(loads(encrypted))
