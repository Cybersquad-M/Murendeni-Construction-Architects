package com.cybersquad.murendeni

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cybersquad.murendeni.data.HelpContent
import com.cybersquad.murendeni.data.HelpItem
import com.cybersquad.murendeni.data.SessionManager
import com.cybersquad.murendeni.databinding.ActivityHelpBinding
import com.cybersquad.murendeni.databinding.ItemHelpQuestionBinding
import com.cybersquad.murendeni.databinding.ItemHelpSectionBinding
import com.cybersquad.murendeni.util.applySystemBarPadding

// Shows the help topics as questions that open when tapped.
// Each role sees the topics written for it. A visitor who is not logged in sees the general topics.
class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.tvBack.setOnClickListener { finish() }

        // The role is empty when nobody is logged in.
        val role = SessionManager(this).getRole()

        for (section in HelpContent.forRole(role)) {
            val title = ItemHelpSectionBinding.inflate(layoutInflater, binding.helpContainer, false)
            title.tvSectionTitle.text = section.title
            binding.helpContainer.addView(title.root)

            for (item in section.items) {
                addQuestion(item)
            }
        }
    }

    private fun addQuestion(item: HelpItem) {
        val row = ItemHelpQuestionBinding.inflate(layoutInflater, binding.helpContainer, false)
        row.tvQuestion.text = item.question
        row.tvAnswer.text = item.answer

        // Tapping the card opens or closes its answer.
        row.root.setOnClickListener {
            val isOpen = row.tvAnswer.visibility == View.VISIBLE
            row.tvAnswer.visibility = if (isOpen) View.GONE else View.VISIBLE
            row.tvToggle.text = if (isOpen) "+" else "–"
        }

        binding.helpContainer.addView(row.root)
    }
}