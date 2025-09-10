version = 6

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Türkçe altyazılı yabancı dizi izle, sadece türkçe altyazılı ve 720p kalite."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=dizigom1.live&sz=%size%"
}
