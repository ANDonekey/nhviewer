package com.nhviewer.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.ui.common.ErrorText
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val profileState: LoadState<UserProfile> = LoadState.Loading
)

class UserProfileViewModel(
    private val galleryRepository: GalleryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(profileState = LoadState.Loading) }
        viewModelScope.launch {
            val meResult = galleryRepository.getMe()
            val profileResult = meResult.fold(
                onSuccess = { me -> galleryRepository.getUserProfile(me.id, me.slug) },
                onFailure = { Result.failure(it) }
            )
            profileResult.fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(profileState = LoadState.Content(profile)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            profileState = LoadState.Error(
                                ErrorText.fromMessage(error.message, "Load profile failed")
                            )
                        )
                    }
                }
            )
        }
    }
}
