package com.example.fastfood.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    protected fun setError(error: String) {
        _error.value = error
    }
    
    protected fun setMessage(message: String) {
        _message.value = message
    }
    
    protected fun clearError() {
        _error.value = null
    }
    
    protected fun executeAsync(
        showLoading: Boolean = true,
        action: suspend () -> Unit
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            _isLoading.value = false
            _error.value = throwable.message ?: "Unknown error occurred"
        }
        
        viewModelScope.launch(exceptionHandler) {
            if (showLoading) _isLoading.value = true
            try {
                action()
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }
} 