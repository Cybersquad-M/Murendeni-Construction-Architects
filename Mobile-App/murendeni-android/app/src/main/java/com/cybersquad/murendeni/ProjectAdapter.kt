package com.cybersquad.murendeni

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cybersquad.murendeni.api.ProjectSummary
import com.cybersquad.murendeni.databinding.ItemProjectBinding
import com.cybersquad.murendeni.util.DateFormatter
import com.cybersquad.murendeni.util.StatusUi

// Turns a list of projects into scrolling cards.
// showClient is true for staff, who need to see which client owns each project.
class ProjectAdapter(
    private val showClient: Boolean,
    private val onClick: (ProjectSummary) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private var projects: List<ProjectSummary> = emptyList()

    fun submit(newProjects: List<ProjectSummary>) {
        projects = newProjects
        notifyDataSetChanged() // Simple and fine for short lists.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun getItemCount(): Int = projects.size

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: ProjectSummary) {
            binding.tvName.text = project.name
            binding.tvMeta.text = listOfNotNull(project.propertyType, project.location)
                .joinToString(" · ")

            if (showClient && project.clientName != null) {
                binding.tvClient.text = "Client: ${project.clientName}"
                binding.tvClient.visibility = View.VISIBLE
            } else {
                binding.tvClient.visibility = View.GONE
            }

            binding.tvStatus.text = StatusUi.projectLabel(project.status)
            binding.tvStatus.backgroundTintList =
                ColorStateList.valueOf(StatusUi.projectColor(project.status))
            binding.tvDate.text = "Updated ${DateFormatter.format(project.updatedAt)}"

            // A grey block is shown while the photo loads, or when the project has no photo.
            Glide.with(binding.ivCover)
                .load(project.coverImageUrl)
                .centerCrop()
                .placeholder(R.color.placeholder_gray)
                .fallback(R.color.placeholder_gray)
                .error(R.color.placeholder_gray)
                .into(binding.ivCover)

            binding.root.setOnClickListener { onClick(project) }
        }
    }
}