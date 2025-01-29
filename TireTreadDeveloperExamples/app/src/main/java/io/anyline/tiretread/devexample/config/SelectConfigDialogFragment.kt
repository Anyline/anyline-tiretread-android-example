package io.anyline.tiretread.devexample.config

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import io.anyline.tiretread.devexample.R

class SelectConfigDialogFragment(
    selectConfigContent: SelectConfigContent,
    onStartScanButtonClick: ((ValidationResult) -> Unit))
    : DialogFragment(R.layout.dialog_select_config) {

    private val selectConfigFragment = SelectConfigFragment.newInstance(
        selectConfigContent = selectConfigContent,
        onStartScanButtonClick = { validationResult ->
            if (validationResult is ValidationResult.Succeed) {
                dialog?.dismiss()
            }
            onStartScanButtonClick.invoke(validationResult)
        })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_select_config, selectConfigFragment)
        transaction.commit()
    }

    companion object {
        const val TAG = "SelectConfigDialogFragment"
    }
}