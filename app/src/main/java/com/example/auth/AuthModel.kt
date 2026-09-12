package com.example.auth

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface CloudSyncState {
    data object Idle : CloudSyncState
    data object Syncing : CloudSyncState
    data class Synced(val cardCount: Int, val lastSyncedTime: Long) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}
