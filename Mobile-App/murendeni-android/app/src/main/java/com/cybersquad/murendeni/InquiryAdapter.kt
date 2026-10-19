package com.cybersquad.murendeni

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cybersquad.murendeni.api.InquiryItem
import com.cybersquad.murendeni.databinding.ItemInquiryBinding
import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.StatusUi

// Turns a list of inquiries into scrolling cards.
class InquiryAdapter : RecyclerView.Adapter<InquiryAdapter.InquiryViewHolder>() {

    private var inquiries: List<InquiryItem> = emptyList()

    fun submit(newInquiries: List<InquiryItem>) {
        inquiries = newInquiries
        notifyDataSetChanged() // Simple and fine for short lists.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InquiryViewHolder {
        val binding = ItemInquiryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InquiryViewHolder(binding)
    }

    override fun getItemCount(): Int = inquiries.size

    override fun onBindViewHolder(holder: InquiryViewHolder, position: Int) {
        holder.bind(inquiries[position])
    }

    class InquiryViewHolder(private val binding: ItemInquiryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(inquiry: InquiryItem) {
            binding.tvService.text = inquiry.service ?: "General inquiry"
            binding.tvStatus.text = StatusUi.inquiryLabel(inquiry.status)
            binding.tvStatus.backgroundTintList =
                ColorStateList.valueOf(StatusUi.inquiryColor(inquiry.status))
            binding.tvDate.text = "Sent ${DateFormatter.format(inquiry.createdAt)}"
            binding.tvMessage.text = inquiry.message ?: ""
        }
    }
}