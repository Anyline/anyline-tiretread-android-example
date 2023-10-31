package io.anyline.tiretread.demo.common

import android.content.Context
import android.graphics.Typeface
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.anyline.tiretread.demo.R


fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
    val spannableString = SpannableString(this.text)
    for (link in links) {
        val clickableSpan = LinkText.CustomActionLinkText(context, link.second)
        val startIndexOfLink = this.text.toString().indexOf(link.first)
        spannableString.setSpan(
            clickableSpan,
            startIndexOfLink,
            startIndexOfLink + link.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod = LinkMovementMethod.getInstance()
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun TextView.makeLinks(vararg links: String) {
    val spannableString = SpannableString(this.text)
    for (link in links) {
        val clickableSpan = LinkText.DefaultActionLinkText(context)
        val startIndexOfLink = this.text.toString().indexOf(link)
        spannableString.setSpan(
            clickableSpan,
            startIndexOfLink,
            startIndexOfLink + link.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod = LinkMovementMethod.getInstance()
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

sealed class LinkText(
    val context: Context,
    private val onClick: View.OnClickListener?,
    private val cancelPendingInputEvents: Boolean = false
) : ClickableSpan() {

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.color = ContextCompat.getColor(context, R.color.primary)
        textPaint.typeface = Typeface.DEFAULT_BOLD
    }

    override fun onClick(view: View) {
        if (cancelPendingInputEvents) {
            view.cancelPendingInputEvents()
        }
        Selection.setSelection((view as TextView).text as Spannable, 0)
        view.invalidate()
        onClick?.onClick(view)
    }

    class DefaultActionLinkText(
        context: Context,
        cancelPendingInputEvents: Boolean = false
    ) : LinkText(context, null, cancelPendingInputEvents) {

        override fun onClick(view: View) {

        }
    }

    class CustomActionLinkText(
        context: Context,
        onClick: View.OnClickListener?,
        cancelPendingInputEvents: Boolean = false
    ) : LinkText(context, onClick, cancelPendingInputEvents)
}