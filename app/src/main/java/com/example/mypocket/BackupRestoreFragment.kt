package com.example.mypocket.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.mypocket.R
import com.example.mypocket.data.*
import com.example.mypocket.entity.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

class BackupRestoreFragment : Fragment() {

    private lateinit var btnBackup: MaterialButton
    private lateinit var btnChooseFile: MaterialButton
    private lateinit var tvSelectedFile: MaterialTextView
    private lateinit var toolbar: MaterialToolbar

    // Launcher for creating backup file
    private val pickBackupFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            uri?.let { saveBackupToUri(it) }
        }

    // Launcher for picking JSON file to restore
    private val getContentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                tvSelectedFile.text = it.lastPathSegment ?: "Selected backup"
                restoreFromJson(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_backup_restore, container, false)

        btnBackup = view.findViewById(R.id.btnBackup)
        btnChooseFile = view.findViewById(R.id.btnChooseFile)
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        toolbar = view.findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnBackup.setOnClickListener { backupToJson() }
        btnChooseFile.setOnClickListener { getContentLauncher.launch("*/*") }

        return view
    }

    private fun backupToJson() {
        val fileName = "mypocket_backup_${System.currentTimeMillis()}.json"
        pickBackupFileLauncher.launch(fileName)
    }

    private fun saveBackupToUri(uri: Uri) {
        val db = AppDatabase.getInstance(requireContext())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupData = mutableMapOf<String, Any>()

                // Export all tables
                backupData["categories"] = db.categoryDao().getAllCategories()
                backupData["accounts"] = db.accountDao().getAllAccountsList()
                backupData["accountGroups"] = db.accountGroupDao().getAllGroupsList()
                backupData["transactions"] = db.transactionDao().getAllTransactions()
                backupData["tasks"] = db.taskDao().getAllTasks()
                backupData["events"] = db.eventDao().observeAllOnce()

                val json = Gson().toJson(backupData)

                requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray())
                }

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Backup saved at: $uri", Toast.LENGTH_LONG).show()
                }

            } catch (e: IOException) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun restoreFromJson(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage("This will overwrite current data. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = requireContext().contentResolver.openInputStream(uri)
                            ?.bufferedReader().use { it?.readText() }
                        if (json == null) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(requireContext(), "Failed to read backup file", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val backupData = Gson().fromJson(json, Map::class.java)
                        val db = AppDatabase.getInstance(requireContext())

                        // Clear all tables
                        db.transactionDao().deleteAll()
                        db.eventDao().deleteAll()
                        db.taskDao().deleteAll()
                        db.accountDao().deleteAll()
                        db.accountGroupDao().deleteAllGroups()
                        db.categoryDao().deleteAll()

                        // Restore account groups
                        (backupData["accountGroups"] as? List<*>)?.forEach {
                            val group = Gson().fromJson(Gson().toJson(it), AccountGroupEntity::class.java)
                            db.accountGroupDao().insertGroup(group)
                        }

                        // Restore accounts
                        (backupData["accounts"] as? List<*>)?.forEach {
                            val account = Gson().fromJson(Gson().toJson(it), AccountEntity::class.java)
                            db.accountDao().insertAccount(account)
                        }

                        // Restore categories
                        (backupData["categories"] as? List<*>)?.forEach {
                            val category = Gson().fromJson(Gson().toJson(it), CategoryEntity::class.java)
                            db.categoryDao().insertCategory(category)
                        }

                        // Restore tasks
                        (backupData["tasks"] as? List<*>)?.forEach {
                            val task = Gson().fromJson(Gson().toJson(it), TaskEntity::class.java)
                            db.taskDao().insert(task)
                        }

                        // Restore events
                        (backupData["events"] as? List<*>)?.forEach {
                            val event = Gson().fromJson(Gson().toJson(it), EventEntity::class.java)
                            db.eventDao().insert(event)
                        }

                        // Restore transactions
                        (backupData["transactions"] as? List<*>)?.forEach {
                            val tx = Gson().fromJson(Gson().toJson(it), TransactionEntity::class.java)
                            db.transactionDao().insert(tx)
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireContext(), "Restore successful", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Extension helpers for DAOs that return List once (not Flow)
    private suspend fun EventDao.observeAllOnce(): List<EventEntity> = observeAll().firstOrNull() ?: emptyList()
    private suspend fun AccountGroupDao.getAllGroupsList(): List<AccountGroupEntity> = getAllGroups().firstOrNull() ?: emptyList()
}