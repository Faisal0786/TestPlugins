package com.example

import com.lagradost.cloudstream3.plugins.*

@CloudstreamPlugin
class ExamplePlugin : plugin() {
    override fun load() {
        registerMainAPI(ExampleProvider())
    }
}
