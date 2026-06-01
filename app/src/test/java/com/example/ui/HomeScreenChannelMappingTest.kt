package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenChannelMappingTest {

    @Test
    fun `pager channels keep visual order so swipe left advances to the next tab`() {
        val activeChannels = listOf("精选", "电商", "本地")

        val pagerChannels = buildPagerChannels(activeChannels)

        assertEquals(listOf("精选", "电商", "本地"), pagerChannels)
    }

    @Test
    fun `channel page lookup matches expected swipe positions`() {
        val pagerChannels = listOf("精选", "电商", "本地")

        assertEquals(0, findChannelPage(pagerChannels, "精选"))
        assertEquals(1, findChannelPage(pagerChannels, "电商"))
        assertEquals(2, findChannelPage(pagerChannels, "本地"))
    }

    @Test
    fun `resolve pager channel falls back safely when page is invalid`() {
        val pagerChannels = listOf("精选", "电商", "本地")

        assertEquals("精选", resolvePagerChannel(pagerChannels, 0, "本地"))
        assertEquals("电商", resolvePagerChannel(pagerChannels, 1, "精选"))
        assertEquals("本地", resolvePagerChannel(pagerChannels, 9, "本地"))
    }
}
