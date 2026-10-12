package it.polimi.webeep.core.common

/** Names of the app-private directories, shared so writers and wipers never drift apart. */
object AppDirectories {
    /** OkHttp response cache, under `cacheDir`. */
    const val HTTP_CACHE = "http_cache"

    /** Coil image cache, under `cacheDir`. */
    const val IMAGE_CACHE = "image_cache"

    /** Temporary copies opened by the file viewer, under `cacheDir`. Also declared in `res/xml/file_paths.xml`. */
    const val VIEWER_CACHE = "viewer"

    /** Persistent downloaded files, under `filesDir`. Also declared in `res/xml/file_paths.xml` as `downloads/`. */
    const val DOWNLOADS = "downloads"
}
