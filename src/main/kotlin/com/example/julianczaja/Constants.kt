package com.example.julianczaja

object Constants {
    const val DEFAULT_BASE_URL = "192.168.1.57"//"0.0.0.0"
    const val DEFAULT_PORT = 8123
    const val DEFAULT_MAX_SPACE_MB = 1000

    const val PHOTO_FILENAME_REGEX = """[0-9]_[0-9]{17}.jpeg"""

    const val THUMBNAIL_SIZE_PX = 200

    val projectPath: String = System.getProperty("user.dir")
}
