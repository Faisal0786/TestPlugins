package com.example

import com.lagradost.cloudstream3.plugins.*

@CloudstreamPlugin
class StreamImdbPlugin : Plugin() {
    override fun load() {
        registerMainAPI(StreamImdbProvider())
    }
}
