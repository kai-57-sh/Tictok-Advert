package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdEntity
import com.example.data.AnalyticsEventEntity
import com.example.data.AppContainer
import com.example.data.GeminiHelper
import com.example.data.SearchIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdvertViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppContainer.repository

    // Local filter state per channel
    private val _selectedChannel = MutableStateFlow("精选")
    val selectedChannel: String get() = _selectedChannel.value

    // Maps to store active tag filter per channel
    private val _activeFilters = mutableStateMapOf<String, String?>()
    val activeFilters: Map<String, String?> get() = _activeFilters

    // UI state for Loading / Refreshing simulation
    var isRefreshing by mutableStateOf(false)
        private set

    // Active video progress store to resume seamlessly (adId -> positionMs)
    private val _videoProgressMap = mutableStateMapOf<String, Int>()
    
    // AI Search States
    var searchQuery by mutableStateOf("")
    var isSearching by mutableStateOf(false)
        private set
    var parsedSearchIntent by mutableStateOf<SearchIntent?>(null)
        private set
    var searchResultsList by mutableStateOf<List<AdEntity>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            // Seed Room from the bundled offline ad package.
            repository.populateAdsIfNeeded(application)
            
            // Backfill remote videos only when a bundled local file is unavailable.
            val ads = repository.allAdsFlow.first()
            ads.filter {
                it.cardType == "video" &&
                    it.localVideoPath == null &&
                    it.videoUrl.isNotBlank()
            }.forEach { ad ->
                launch {
                    val path = repository.downloadVideoLocally(application, ad.id, ad.videoUrl)
                    if (path != null) {
                        repository.updateLocalVideoPath(ad.id, path)
                    }
                }
            }
        }
    }

    // Connect Room flows
    val allAds: StateFlow<List<AdEntity>> = repository.allAdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analyticsEvents: StateFlow<List<AnalyticsEventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exposureCount: StateFlow<Int> = repository.exposureCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val clickCount: StateFlow<Int> = repository.clickCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val interactionCount: StateFlow<Int> = repository.interactionCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Derived: Current Channel Ads with tags filter applied
    val currentChannelAds: StateFlow<List<AdEntity>> = combine(
        allAds,
        _selectedChannel
    ) { ads, channel ->
        ads.filter { it.channel == channel }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectChannel(channel: String) {
        _selectedChannel.value = channel
    }

    fun toggleTagFilter(channel: String, tag: String) {
        val currentTag = _activeFilters[channel]
        if (currentTag == tag) {
            _activeFilters[channel] = null // Clear tag
        } else {
            _activeFilters[channel] = tag // Apply tag
        }
    }

    fun clearTagFilter(channel: String) {
        _activeFilters[channel] = null
    }

    /**
     * Pull down simulation: triggers content reload, simulated network latency,
     * resets local temporary flows and increments views where scrolling.
     */
    fun refreshContent() {
        viewModelScope.launch {
            isRefreshing = true
            kotlinx.coroutines.delay(800) // Aesthetic delay for premium load feel
            isRefreshing = false
        }
    }

    /**
     * Impressions logger - registers exposure on DB and events list
     */
    fun onAdExposed(adId: String) {
        viewModelScope.launch {
            repository.recordExposure(adId)
        }
    }

    /**
     * Click tracker - registers click in DB and increments analytics counters
     */
    fun onAdClicked(adId: String) {
        viewModelScope.launch {
            repository.recordClick(adId)
        }
    }

    // Toggles for interactions (likes, favorites, shares)
    fun toggleLike(adId: String) {
        viewModelScope.launch {
            repository.toggleLike(adId)
            // If in search results, refresh search list item details
            updateSearchResultsItem(adId)
        }
    }

    fun toggleFavorite(adId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(adId)
            updateSearchResultsItem(adId)
        }
    }

    fun shareAd(adId: String) {
        viewModelScope.launch {
            repository.recordShare(adId)
            val adEntity = AppContainer.database.adDao().getAdById(adId)
            if (adEntity != null) {
                ShareHelper.shareAd(getApplication(), adEntity)
            }
            updateSearchResultsItem(adId)
        }
    }

    private suspend fun updateSearchResultsItem(adId: String) {
        if (searchResultsList.isNotEmpty()) {
            val freshList = searchResultsList.map {
                if (it.id == adId) {
                    AppContainer.database.adDao().getAdById(adId) ?: it
                } else {
                    it
                }
            }
            searchResultsList = freshList
        }
    }

    // Video progress synchronization mechanics
    fun saveVideoProgress(adId: String, progressMs: Int) {
        _videoProgressMap[adId] = progressMs
    }

    fun getVideoProgress(adId: String): Int {
        return _videoProgressMap[adId] ?: 0
    }

    /**
     * AI Conversational search processor - contacts Gemini for parsing intent.
     */
    fun performSearch(query: String) {
        if (query.isBlank()) return
        searchQuery = query
        viewModelScope.launch {
            isSearching = true
            val resultIntent = GeminiHelper.parseSearchQuery(query)
            parsedSearchIntent = resultIntent

            // Apply intent filter to database list
            val ads = allAds.value
            var filtered = ads

            // 1. Channel filter from AI
            if (resultIntent.channel != null) {
                filtered = filtered.filter { it.channel == resultIntent.channel }
            }

            // 2. Tags filter from AI
            if (resultIntent.tags.isNotEmpty()) {
                filtered = filtered.filter { ad ->
                    val adTags = ad.getTagList()
                    resultIntent.tags.any { aiTag -> adTags.contains(aiTag) }
                }
            }

            // If nothing is matched at all, find by keywords in description or title manually for robust UX
            if (filtered.isEmpty()) {
                filtered = ads.filter { ad ->
                    ad.title.contains(query, ignoreCase = true) || 
                    ad.description.contains(query, ignoreCase = true) ||
                    ad.advertiserName.contains(query, ignoreCase = true)
                }
            }

            searchResultsList = filtered
            isSearching = false
        }
    }

    fun clearSearch() {
        searchQuery = ""
        parsedSearchIntent = null
        searchResultsList = emptyList()
    }

    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
            _videoProgressMap.clear()
            _activeFilters.clear()
        }
    }
}
