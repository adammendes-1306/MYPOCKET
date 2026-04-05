package com.example.mypocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.entity.CategoryEntity
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentManageCategoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageCategoryFragment : Fragment() {

    private var _binding: FragmentManageCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private var categoryType: String = "INCOME"
    private var categoryId: Long? = null // null = new category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryType = arguments?.getString("CATEGORY_TYPE") ?: "INCOME"
        categoryId = arguments?.getLong("CATEGORY_ID", -1L)?.takeIf { it >= 0L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        // Set toolbar title
        binding.toolbar.title = if (categoryId != null) "Edit Category" else "Add Category"

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // If editing, prefill category name
        categoryId?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                val category = db.categoryDao().getCategoryByIdNullable(id)
                withContext(Dispatchers.Main) {
                    if (category != null) binding.etCategoryName.setText(category.name)
                    else Toast.makeText(requireContext(), "Category not found", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etCategoryName.text?.toString()?.trim()
            if (name.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                if (categoryId == null) {
                    // Insert new category with correct order
                    val maxOrder = db.categoryDao().getMaxOrder(categoryType) ?: 0

                    db.categoryDao().insertCategory(
                        CategoryEntity(
                            name = name,
                            type = categoryType,
                            sortOrder = maxOrder + 1 // Place at bottom
                        )
                    )
                } else {
                    val cat = db.categoryDao().getCategoryById(categoryId!!)
                    db.categoryDao().updateCategory(cat.copy(name = name))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "New category saved", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}