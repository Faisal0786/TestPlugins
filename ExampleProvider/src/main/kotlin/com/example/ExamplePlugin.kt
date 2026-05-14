package com.example

import com.lagradost.cloudstream3.plugins.*

@CloudstreamPlugin
class ExamplePlugin : Plugin() {
    override fun load() {
        registerMainAPI(StreamImdbProvider())
    }
}
