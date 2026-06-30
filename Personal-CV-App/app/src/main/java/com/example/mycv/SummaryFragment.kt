package com.example.mycv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mycv.databinding.FragmentSummaryBinding

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textSummary.text =
            getString(R.string.final_year_computer_engineering_student_electrical_engineering_department_building_skills_in_software_development_embedded_systems_and_database_design_proficient_in_c_and_android_development_kotlin_with_experience_in_relational_databases_using_mysql_including_erd_and_sql_queries_seeking_a_junior_role_to_apply_knowledge_and_grow_professionally).trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}