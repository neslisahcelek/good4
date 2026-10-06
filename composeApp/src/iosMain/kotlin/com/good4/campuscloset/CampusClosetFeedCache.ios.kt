package com.good4.campuscloset

import platform.Foundation.NSUserDefaults

private const val FEED_KEY = "good4_campus_closet_feed"

actual fun loadCampusClosetFeedCache(): String? = NSUserDefaults.standardUserDefaults.stringForKey(FEED_KEY)

actual fun saveCampusClosetFeedCache(value: String?) {
    if (value == null) NSUserDefaults.standardUserDefaults.removeObjectForKey(FEED_KEY)
    else NSUserDefaults.standardUserDefaults.setObject(value, FEED_KEY)
}
