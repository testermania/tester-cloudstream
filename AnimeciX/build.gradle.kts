version = 6

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "tr"
    description = "AnimeciX, animeleri keşfedebileceğiniz ve animeler hakkında her şeyi öğrenebileceğiniz bir paylaşım platformudur."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Anime")
    iconUrl = "https://anm.cx/favicon.ico"
}
