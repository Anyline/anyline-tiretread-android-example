package io.anyline.tiretread.demo

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.anyline.tiretread.demo.databinding.BottomSheetOptionBinding
import io.anyline.tiretread.demo.databinding.ViewBottomSheetBinding

class CustomBottomSheet(
    val headerText: String,
    val options: List<BottomSheetOption>,
    val customBottomSheetListener: CustomBottomSheetListener?
) : BottomSheetDialogFragment() {

    private lateinit var binding: ViewBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ViewBottomSheetBinding.inflate(inflater)
        binding.header.text = headerText
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            options.adapter = BottomSheetRecyclerViewAdapter(
                this@CustomBottomSheet.options, this@CustomBottomSheet::dismiss
            )
            options.layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    inner class BottomSheetRecyclerViewAdapter(
        private val items: List<BottomSheetOption>, private val dismiss: () -> Unit
    ) : RecyclerView.Adapter<BottomSheetRecyclerViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): BottomSheetRecyclerViewHolder {
            return BottomSheetRecyclerViewHolder(
                BottomSheetOptionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), dismiss
            )
        }

        override fun onBindViewHolder(holder: BottomSheetRecyclerViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class BottomSheetRecyclerViewHolder(
        private val viewBinding: BottomSheetOptionBinding, private val dismiss: () -> Unit
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(bottomSheetOption: BottomSheetOption) {
            viewBinding.title.text = bottomSheetOption.title
            viewBinding.icon.isVisible = customBottomSheetListener?.let {
                bottomSheetOption.value == it.getCurrentSelectedOption()
            } ?: run { false }
            viewBinding.root.setOnClickListener {
                customBottomSheetListener?.onCustomBottomOptionSelected(bottomSheetOption)
                dismiss()
            }
        }
    }

    class BottomSheetOption(
        val value: Int, val title: String
    )

    interface CustomBottomSheetListener {
        fun getCurrentSelectedOption(): Int
        fun onCustomBottomOptionSelected(selectedOption: BottomSheetOption)
    }

}