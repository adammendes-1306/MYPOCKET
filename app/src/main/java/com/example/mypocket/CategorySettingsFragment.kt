package com.example.mypocket.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.entity.CategoryEntity
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.databinding.FragmentCategorySettingsBinding
import com.example.mypocket.adapter.CategoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Full screen fragment showing all categories of a given type.
 * Toolbar has "+" button to add a new category.
 * RecyclerView lists all categories (item_settings_row.xml)
 */
class CategorySettingsFragment : Fragment() {

    private var _binding: FragmentCategorySettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var db: AppDatabase
    private var categoryType: String = "INCOME" // default type
    private var reorderedList: List<CategoryEntity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // enable toolbar menu

        // Get category type from arguments, default to INCOME
        categoryType = arguments?.getString("CATEGORY_TYPE") ?: "INCOME"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategorySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        // Setup toolbar
        binding.toolbar.title = "${categoryType.lowercase().replaceFirstChar { it.uppercase() }} Category"
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }

        // Inflate menu on toolbar
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    // Open this same fragment to add new category
                    val fragment = ManageCategoryFragment().apply {
                        arguments = Bundle().apply { putString("CATEGORY_TYPE", categoryType) }
                    }
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Setup RecyclerView and adapter with click handlers
        categoryAdapter = CategoryAdapter { action, category ->
            when (action) {
                "delete" -> deleteCategory(category)
                "edit" -> openManageCategoryFragment(category)
            }
        }

        binding.rvSettings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {

            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition

                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false
                }

                // Work on the actual adapter list instead of creating a new list
                val currentList = categoryAdapter.currentList.toMutableList()
                Collections.swap(currentList, fromPos, toPos)

                // Update sortOrder immediately in the local list
                currentList.forEachIndexed { index, category ->
                    category.sortOrder = index
                }

                // Optimistically update the adapter visually
                categoryAdapter.submitList(currentList.toList()) // submit a new copy

                // IMPORTANT: Visually moves the item during drag
                categoryAdapter.notifyItemMoved(fromPos, toPos)

                // Save for later
                reorderedList = currentList

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder?.itemView?.alpha = 0.7f
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f

                // Persist the reordered list in DB asynchronously
                reorderedList?.let { list ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        list.forEachIndexed { index, category ->
                            // create a new CategoryEntity with updated sortOrder
                            val updatedCategory = category.copy(sortOrder = index)
                            db.categoryDao().updateCategory(updatedCategory)
                        }
                    }
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSettings)

        categoryAdapter.onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        // Load categories from DB
        loadCategories()
    }

    /**
     * Load categories from Room DB by type
     * Runs in IO thread and updates adapter on Main thread
     */
    private fun loadCategories() {
        // Use lifecycleScope to collect Flow safely
        viewLifecycleOwner.lifecycleScope.launch {
            db.categoryDao()
                .getCategoriesByTypeFlow(categoryType) // should return Flow<List<CategoryEntity>>
                .collect { categories ->
                    // Submit list to adapter on main thread
                    categoryAdapter.submitList(categories)
                }
        }
    }

    /**
     * Delete category from DB and refresh list
     */
    private fun deleteCategory(category: CategoryEntity) {
        // Show confirmation dialog first
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete \"${category.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                // User confirmed → perform deletion
                lifecycleScope.launch(Dispatchers.IO) {
                    db.categoryDao().deleteCategory(category)
                }
            }
            .setNegativeButton("Cancel", null) // just dismiss
            .show()
    }

    /**
     * Open AddCategoryFragment for adding or editing a category
     */
    private fun openManageCategoryFragment(category: CategoryEntity? = null) {
        val fragment = ManageCategoryFragment().apply {
            arguments = Bundle().apply {
                putString("CATEGORY_TYPE", categoryType)
                // Only pass ID if editing an existing category
                category?.let { putLong("CATEGORY_ID", it.categoryId) }
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Inflate "+" menu in toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add, menu) // Make sure menu_add.xml has item with id action_add
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Handle toolbar menu item clicks
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                openManageCategoryFragment() // Opens AddCategoryFragment
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}