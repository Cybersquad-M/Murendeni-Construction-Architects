package com.cybersquad.murendeni

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cybersquad.murendeni.api.StaffInquiry
import com.cybersquad.murendeni.databinding.ItemStaffInquiryBinding
import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.StatusUi

// Turns a list of inquiries into scrolling cards for sales staff.
class StaffInquiryAdapter(
    private val onClick: (StaffInquiry) -> Unit
) : RecyclerView.Adapter<StaffInquiryAdapter.StaffInquiryViewHolder>() {

    private var inquiries: List<StaffInquiry> = emptyList()

    fun submit(newInquiries: List<StaffInquiry>) {
        inquiries = newInquiries
        notifyDataSetChanged() // Simple and fine for short lists.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffInquiryViewHolder {
        val binding = ItemStaffInquiryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StaffInquiryViewHolder(binding)
    }

    override fun getItemCount(): Int = inquiries.size

    override fun onBindViewHolder(holder: StaffInquiryViewHolder, position: Int) {
        holder.bind(inquiries[position])
    }

    inner class StaffInquiryViewHolder(private val binding: ItemStaffInquiryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(inquiry: StaffInquiry) {
            binding.tvName.text = inquiry.contactName

            binding.tvStatus.text = StatusUi.inquiryLabel(inquiry.status)
            binding.tvStatus.backgroundTintList =
                ColorStateList.valueOf(StatusUi.inquiryColor(inquiry.status))

            // "Guest" marks a visitor who does not have a client account yet.
            val guestLabel = if (inquiry.hasClientAccount) null else "Guest"
            binding.tvMeta.text = listOfNotNull(
                inquiry.service,
                guestLabel,
                DateFormatter.format(inquiry.createdAt)
            ).joinToString(" · ")

            binding.tvMessage.text = inquiry.message ?: ""

            binding.tvAssigned.text = if (inquiry.assignedToName != null) {
                "Assigned to ${inquiry.assignedToName}"
            } else {
                "Not assigned yet"
            }

            binding.root.setOnClickListener { onClick(inquiry) }
        }
    }
}