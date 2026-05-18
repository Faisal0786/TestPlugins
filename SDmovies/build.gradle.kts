version = "1.0.0"
description = "SDmovies Plugin"

android {
    compileSdk = 33
    
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
}

dependencies {
    compileOnly("com.lagradost:cloudstream3:latest")
    compileOnly("org.jsoup:jsoup:1.14.3")
}
