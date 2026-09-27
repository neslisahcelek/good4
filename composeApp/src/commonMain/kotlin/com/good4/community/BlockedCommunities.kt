package com.good4.community

/** Communities the student chose to block; stored on the device only. */
expect fun loadBlockedCommunityIds(): Set<String>

expect fun saveBlockedCommunityIds(ids: Set<String>)
