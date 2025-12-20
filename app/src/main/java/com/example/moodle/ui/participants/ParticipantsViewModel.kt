package com.example.moodle.ui.participants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moodle.data.model.Participant
import com.example.moodle.data.repository.AttendanceRepository

class ParticipantsViewModel : ViewModel() {
    
    private val repository = AttendanceRepository()
    
    private val _participants = MutableLiveData<List<Participant>>()
    val participants: LiveData<List<Participant>> = _participants
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadParticipants(accessToken: String) {
        _isLoading.value = true
        _error.value = null
        
        repository.getParticipants(
            accessToken = accessToken,
            onSuccess = { participantsList ->
                _isLoading.value = false
                _participants.value = participantsList
            },
            onError = { errorMessage ->
                _isLoading.value = false
                _error.value = errorMessage
            }
        )
    }
    
    fun clearError() {
        _error.value = null
    }
}

