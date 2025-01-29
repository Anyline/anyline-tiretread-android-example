package io.anyline.tiretread.devexample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.anyline.tiretread.devexample.config.SelectConfigContent
import io.anyline.tiretread.devexample.config.ValidationResult

class MainViewModel: ViewModel() {
    val lastSelectConfigContent: MutableLiveData<SelectConfigContent> = MutableLiveData()
    val lastOnSelectConfigDialogFragmentButton: MutableLiveData<((ValidationResult) -> Unit)> = MutableLiveData()
}