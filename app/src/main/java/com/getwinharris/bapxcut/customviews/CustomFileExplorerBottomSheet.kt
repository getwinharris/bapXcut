package com.getwinharris.bapxcut.customviews

import android.content.ContentUris
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.getwinharris.bapxcut.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class CustomFileExplorerBottomSheet : BottomSheetDialogFragment() {

    enum class FilterType { ALL, VIDEO, IMAGE, AUDIO }

    data class FileNode(
        val file: File,
        val isDirectory: Boolean,
        val displayName: String,
        val subtext: String,
        val durationMs: Long = 0L,
        val uri: Uri
    )

    var targetFilter: FilterType = FilterType.ALL
    var onFileSelectedListener: ((Uri, File) -> Unit)? = null

    private lateinit var rvExplorerNodes: RecyclerView
    private lateinit var pbExplorerLoading: ProgressBar
    private lateinit var tvExplorerEmpty: TextView
    private lateinit var tvCurrentPath: TextView
    private lateinit var btnUpDirectory: ImageView
    private lateinit var chipGroupLocations: ChipGroup

    private var currentDirectory: File = Environment.getExternalStorageDirectory()
    private val nodeList = mutableListOf<FileNode>()
    private var adapter: NodeAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_file_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnCloseExplorer)?.setOnClickListener {
            dismiss()
        }

        rvExplorerNodes = view.findViewById(R.id.rvExplorerNodes)
        pbExplorerLoading = view.findViewById(R.id.pbExplorerLoading)
        tvExplorerEmpty = view.findViewById(R.id.tvExplorerEmpty)
        tvCurrentPath = view.findViewById(R.id.tvCurrentPath)
        btnUpDirectory = view.findViewById(R.id.btnUpDirectory)
        chipGroupLocations = view.findViewById(R.id.chipGroupLocations)

        rvExplorerNodes.layoutManager = LinearLayoutManager(requireContext())
        adapter = NodeAdapter(nodeList) { selectedNode ->
            if (selectedNode.isDirectory) {
                navigateToDirectory(selectedNode.file)
            } else {
                val mediaStoreUri = findMediaStoreUriForFile(selectedNode.file) ?: selectedNode.uri
                onFileSelectedListener?.invoke(mediaStoreUri, selectedNode.file)
                dismiss()
            }
        }
        rvExplorerNodes.adapter = adapter

        btnUpDirectory.setOnClickListener {
            val parent = currentDirectory.parentFile
            if (parent != null && parent.canRead()) {
                navigateToDirectory(parent)
            }
        }

        setupLocationChips(view)
        navigateToDirectory(currentDirectory)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.behavior?.skipCollapsed = true
    }

    private fun setupLocationChips(view: View) {
        view.findViewById<View>(R.id.chipDownloads)?.setOnClickListener {
            navigateToDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        }
        view.findViewById<View>(R.id.chipDcim)?.setOnClickListener {
            navigateToDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        }
        view.findViewById<View>(R.id.chipMovies)?.setOnClickListener {
            navigateToDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
        }
        view.findViewById<View>(R.id.chipPictures)?.setOnClickListener {
            navigateToDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        }
        view.findViewById<View>(R.id.chipRootStorage)?.setOnClickListener {
            navigateToDirectory(Environment.getExternalStorageDirectory())
        }
    }

    private fun navigateToDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        currentDirectory = directory
        tvCurrentPath.text = directory.absolutePath

        pbExplorerLoading.visibility = View.VISIBLE
        tvExplorerEmpty.visibility = View.GONE
        nodeList.clear()
        adapter?.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            val nodes = listDirectoryNodes(directory)
            withContext(Dispatchers.Main) {
                pbExplorerLoading.visibility = View.GONE
                if (nodes.isEmpty()) {
                    tvExplorerEmpty.visibility = View.VISIBLE
                } else {
                    tvExplorerEmpty.visibility = View.GONE
                    nodeList.addAll(nodes)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun listDirectoryNodes(dir: File): List<FileNode> {
        val files = dir.listFiles() ?: return emptyList()
        val dirsList = mutableListOf<FileNode>()
        val filesList = mutableListOf<FileNode>()

        for (file in files) {
            if (file.name.startsWith(".")) continue // Skip hidden files/folders

            if (file.isDirectory) {
                val childCount = file.listFiles()?.size ?: 0
                dirsList.add(
                    FileNode(
                        file = file,
                        isDirectory = true,
                        displayName = file.name,
                        subtext = "$childCount items",
                        uri = Uri.fromFile(file)
                    )
                )
            } else {
                val ext = file.extension.lowercase(Locale.getDefault())
                val isVideo = ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")
                val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                val isAudio = ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")

                val matchesFilter = when (targetFilter) {
                    FilterType.VIDEO -> isVideo
                    FilterType.IMAGE -> isImage || isVideo
                    FilterType.AUDIO -> isAudio
                    FilterType.ALL -> true
                }

                if (matchesFilter) {
                    val sizeStr = formatFileSize(file.length())
                    var durationMs = 0L
                    if (isVideo || isAudio) {
                        durationMs = extractMediaDuration(file)
                    }

                    filesList.add(
                        FileNode(
                            file = file,
                            isDirectory = false,
                            displayName = file.name,
                            subtext = sizeStr,
                            durationMs = durationMs,
                            uri = Uri.fromFile(file)
                        )
                    )
                }
            }
        }

        // Sort directories alphabetically, then files by name alphabetically
        dirsList.sortBy { it.displayName.lowercase(Locale.getDefault()) }
        filesList.sortBy { it.displayName.lowercase(Locale.getDefault()) }

        return dirsList + filesList
    }

    private fun extractMediaDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            try { retriever.release() } catch (_: Exception) {}
            0L
        }
    }

    private fun findMediaStoreUriForFile(file: File): Uri? {
        val context = context ?: return null
        val filePath = file.absolutePath
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        val urisToQuery = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )

        for (contentUri in urisToQuery) {
            try {
                context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        return ContentUris.withAppendedId(contentUri, id)
                    }
                }
            } catch (e: Exception) {
                // Ignore query failures
            }
        }
        return null
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    private inner class NodeAdapter(
        private val items: List<FileNode>,
        private val onItemClick: (FileNode) -> Unit
    ) : RecyclerView.Adapter<NodeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_explorer_node, parent, false)
            return NodeViewHolder(view)
        }

        override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, onItemClick)
        }

        override fun getItemCount(): Int = items.size
    }

    private inner class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivNodeIcon: ImageView = itemView.findViewById(R.id.ivNodeIcon)
        val tvNodeDuration: TextView = itemView.findViewById(R.id.tvNodeDuration)
        val tvNodeName: TextView = itemView.findViewById(R.id.tvNodeName)
        val tvNodeSubtext: TextView = itemView.findViewById(R.id.tvNodeSubtext)
        val ivChevron: ImageView = itemView.findViewById(R.id.ivChevron)

        fun bind(node: FileNode, onItemClick: (FileNode) -> Unit) {
            tvNodeName.text = node.displayName
            tvNodeSubtext.text = node.subtext
            ivNodeIcon.setImageBitmap(null)
            ivNodeIcon.clearColorFilter()

            if (node.isDirectory) {
                ivChevron.visibility = View.VISIBLE
                tvNodeDuration.visibility = View.GONE
                ivNodeIcon.setImageResource(R.drawable.ic_folder_24)
                ivNodeIcon.setColorFilter(itemView.context.getColor(R.color.colorPrimary))
            } else {
                ivChevron.visibility = View.GONE
                val ext = node.file.extension.lowercase(Locale.getDefault())
                val isVideo = ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")
                val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

                if (isVideo || node.durationMs > 0) {
                    tvNodeDuration.visibility = View.VISIBLE
                    tvNodeDuration.text = formatDuration(node.durationMs)
                } else {
                    tvNodeDuration.visibility = View.GONE
                }

                if (isVideo || isImage) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val bitmap = loadThumbnail(node.file, isVideo)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                ivNodeIcon.setImageBitmap(bitmap)
                            } else {
                                val placeholder = if (isVideo) R.drawable.ic_play_24 else R.drawable.ic_image_24
                                ivNodeIcon.setImageResource(placeholder)
                            }
                        }
                    }
                } else if (ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")) {
                    ivNodeIcon.setImageResource(R.drawable.ic_audio_24)
                } else {
                    ivNodeIcon.setImageResource(R.drawable.ic_folder_24)
                }
            }

            itemView.setOnClickListener {
                onItemClick(node)
            }
        }

        private fun loadThumbnail(file: File, isVideo: Boolean): Bitmap? {
            val context = itemView.context ?: return null
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(Uri.fromFile(file), Size(300, 300), null)
                } else {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val frame = retriever.frameAtTime
                    retriever.release()
                    frame
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
