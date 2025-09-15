// package com.example.moodle.ui.login

// import androidx.lifecycle.LiveData
// import androidx.lifecycle.MutableLiveData
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import com.example.moodle.data.repository.AuthRepository
// import kotlinx.coroutines.launch
// import android.util.Log

// class LoginViewModel(
//     private val repo: AuthRepository
// ) : ViewModel() {

//     sealed class UiState {
//         object Idle : UiState()
//         object Loading : UiState()
//         data class Success(val token: String) : UiState()
//         data class Error(val message: String) : UiState()
//     }

//     private val _state = MutableLiveData<UiState>(UiState.Idle)
//     val state: LiveData<UiState> = _state

//     fun login(username: String, password: String) {
//         _state.value = UiState.Loading
//         viewModelScope.launch {
//             val result = repo.login(username, password, "moodle_mobile_app")
//             _state.value = result.fold(
//                 onSuccess = { UiState.Success(it.token ?: "") },
//                 onFailure = { UiState.Error(it.message ?: "Unknown error") }
//             )
//         }
//     }
// }
