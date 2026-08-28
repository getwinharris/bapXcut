package com.getwinharris.bapxcut.customviews

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.getwinharris.bapxcut.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MediaPickerBottomSheet : BottomSheetDialogFragment() {

    enum class MediaType { VIDEO, IMAGE, AUDIO }

    data class MediaModel(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val durationMs: Long,
        val type: MediaType,
        val dateAdded: Long,
        val bucketName: String = "All",
        val artist: String = "Unknown Artist",
        val localFile: File? = null
    )

    var initialMediaType: MediaType = MediaType.VIDEO
    var showCategoryTabs: Boolean = false
    var showAudioTab: Boolean = true
    var onMediaSelectedListener: ((Uri) -> Unit)? = null
    var onBrowseSystemFoldersRequested: (() -> Unit)? = null

    private lateinit var rvMediaGrid: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmptyContainer: View
    private lateinit var tvEmptyState: TextView
    private lateinit var btnGrantPermission: View
    private lateinit var btnSystemPickerFallback: View
    private lateinit var layoutTabsContainer: View
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var chipVideos: Chip
    private lateinit var chipImages: Chip
    private lateinit var chipAudio: Chip
    private lateinit var btnAlbumDropdown: View
    private lateinit var tvSelectedAlbumName: TextView

    private val allFetchedItems = mutableListOf<MediaModel>()
    private val displayedItems = mutableListOf<MediaModel>()
    private val albumBuckets = mutableMapOf<String, MutableList<MediaModel>>()
    private var selectedAlbum: String = "All"
    private var currentMediaType: MediaType = MediaType.VIDEO
    private var adapter: MediaAdapter? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            btnGrantPermission.visibility = View.GONE
            loadMedia(currentMediaType)
        } else {
            pbLoading.visibility = View.GONE
            layoutEmptyContainer.visibility = View.VISIBLE
            tvEmptyState.text = "Permission denied. Grant access or browse folders."
            btnGrantPermission.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_media_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnCloseSheet)?.setOnClickListener {
            dismiss()
        }

        rvMediaGrid = view.findViewById(R.id.rvMediaGrid)
        pbLoading = view.findViewById(R.id.pbLoading)
        layoutEmptyContainer = view.findViewById(R.id.layoutEmptyContainer)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        btnSystemPickerFallback = view.findViewById(R.id.btnSystemPickerFallback)
        layoutTabsContainer = view.findViewById(R.id.layoutTabsContainer)
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter)
        chipVideos = view.findViewById(R.id.chipVideos)
        chipImages = view.findViewById(R.id.chipImages)
        chipAudio = view.findViewById(R.id.chipAudio)
        btnAlbumDropdown = view.findViewById(R.id.btnAlbumDropdown)
        tvSelectedAlbumName = view.findViewById(R.id.tvSelectedAlbumName)

        layoutTabsContainer.visibility = if (showCategoryTabs) View.VISIBLE else View.GONE
        chipAudio.visibility = if (showAudioTab) View.VISIBLE else View.GONE

        currentMediaType = initialMediaType
        updateLayoutManagerForType(currentMediaType)

        adapter = MediaAdapter(displayedItems) { selectedItem ->
            onMediaSelectedListener?.invoke(selectedItem.uri)
            dismiss()
        }
        rvMediaGrid.adapter = adapter

        btnGrantPermission.setOnClickListener {
            requestStoragePermissions()
        }

        btnSystemPickerFallback.setOnClickListener {
            dismiss()
            onBrowseSystemFoldersRequested?.invoke()
        }

        when (currentMediaType) {
            MediaType.VIDEO -> chipVideos.isChecked = true
            MediaType.IMAGE -> chipImages.isChecked = true
            MediaType.AUDIO -> chipAudio.isChecked = true
        }

        btnAlbumDropdown.setOnClickListener {
            showAlbumPopupMenu(it)
        }

        if (showCategoryTabs) {
            chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
                currentMediaType = when {
                    checkedIds.contains(R.id.chipVideos) -> MediaType.VIDEO
                    checkedIds.contains(R.id.chipImages) -> MediaType.IMAGE
                    checkedIds.contains(R.id.chipAudio) -> MediaType.AUDIO
                    else -> MediaType.VIDEO
                }

                updateLayoutManagerForType(currentMediaType)

                selectedAlbum = "All"
                tvSelectedAlbumName.text = getDefaultAlbumTitle(currentMediaType)
                checkPermissionsAndLoadMedia()
            }
        }

        tvSelectedAlbumName.text = getDefaultAlbumTitle(currentMediaType)
        checkPermissionsAndLoadMedia()
    }

    private fun updateLayoutManagerForType(type: MediaType) {
        rvMediaGrid.layoutManager = if (type == MediaType.AUDIO) {
            GridLayoutManager(requireContext(), 1)
        } else {
            GridLayoutManager(requireContext(), 3)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.behavior?.skipCollapsed = true
    }

    private fun checkPermissionsAndLoadMedia() {
        if (hasRequiredPermissions()) {
            btnGrantPermission.visibility = View.GONE
            loadMedia(currentMediaType)
        } else {
            requestStoragePermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val context = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val videoPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val imagePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val audioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            var userSelectedPerm = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                userSelectedPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            }
            videoPerm || imagePerm || audioPerm || userSelectedPerm
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun getDefaultAlbumTitle(type: MediaType): String {
        return when (type) {
            MediaType.VIDEO -> "All Videos"
            MediaType.IMAGE -> "All Photos"
            MediaType.AUDIO -> "All Audio"
        }
    }

    private fun showAlbumPopupMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add(0, 0, 0, getDefaultAlbumTitle(currentMediaType))

        val bucketNames = albumBuckets.keys.sorted()
        bucketNames.forEachIndexed { index, bucketName ->
            val count = albumBuckets[bucketName]?.size ?: 0
            popup.menu.add(1, index + 1, index + 1, "$bucketName ($count)")
        }

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == 0) {
                selectedAlbum = "All"
                tvSelectedAlbumName.text = getDefaultAlbumTitle(currentMediaType)
                filterDisplayedItemsByAlbum("All")
            } else {
                val bucketName = bucketNames.getOrNull(menuItem.itemId - 1)
                if (bucketName != null) {
                    selectedAlbum = bucketName
                    tvSelectedAlbumName.text = bucketName
                    filterDisplayedItemsByAlbum(bucketName)
                }
            }
            true
        }
        popup.show()
    }

    private fun filterDisplayedItemsByAlbum(album: String) {
        displayedItems.clear()
        if (album == "All") {
            displayedItems.addAll(allFetchedItems)
        } else {
            val bucketList = albumBuckets[album]
            if (bucketList != null) {
                displayedItems.addAll(bucketList)
            }
        }
        adapter?.notifyDataSetChanged()

        if (displayedItems.isEmpty()) {
            layoutEmptyContainer.visibility = View.VISIBLE
            tvEmptyState.text = "No media files found."
        } else {
            layoutEmptyContainer.visibility = View.GONE
        }
    }

    private fun loadMedia(type: MediaType) {
        pbLoading.visibility = View.VISIBLE
        layoutEmptyContainer.visibility = View.GONE
        allFetchedItems.clear()
        displayedItems.clear()
        albumBuckets.clear()
        adapter?.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            val items = queryMediaStoreMedia(type)
            withContext(Dispatchers.Main) {
                pbLoading.visibility = View.GONE
                allFetchedItems.addAll(items)

                for (item in items) {
                    val bucket = item.bucketName
                    val bucketList = albumBuckets.getOrPut(bucket) { mutableListOf() }
                    bucketList.add(item)
                }

                filterDisplayedItemsByAlbum(selectedAlbum)
            }
        }
    }

    private fun queryMediaStoreMedia(type: MediaType): List<MediaModel> {
        val context = context ?: return emptyList()
        val result = mutableListOf<MediaModel>()
        val contentResolver = context.contentResolver

        val (contentUri, projection) = when (type) {
            MediaType.VIDEO -> Pair(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )
            )
            MediaType.IMAGE -> Pair(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
            )
            MediaType.AUDIO -> Pair(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST
                )
            )
        }

        try {
            contentResolver.query(contentUri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val durationColumn = if (type != MediaType.IMAGE) cursor.getColumnIndex(MediaStore.MediaColumns.DURATION) else -1
                val bucketColumn = if (type != MediaType.AUDIO) cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME) else cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artistColumn = if (type == MediaType.AUDIO) cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Media"
                    val dateAdded = cursor.getLong(dateColumn)
                    val duration = if (durationColumn != -1 && !cursor.isNull(durationColumn)) cursor.getLong(durationColumn) else 0L
                    val bucketName = if (bucketColumn != -1 && !cursor.isNull(bucketColumn)) cursor.getString(bucketColumn) ?: "All Audio" else "All"
                    val artistName = if (artistColumn != -1 && !cursor.isNull(artistColumn)) cursor.getString(artistColumn) ?: "Unknown Artist" else "Unknown Artist"
                    val uri = ContentUris.withAppendedId(contentUri, id)

                    result.add(
                        MediaModel(
                            id = id,
                            uri = uri,
                            displayName = name,
                            durationMs = duration,
                            type = type,
                            dateAdded = dateAdded,
                            bucketName = bucketName,
                            artist = artistName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaPicker", "Error querying MediaStore for $type", e)
        }

        if (result.isEmpty()) {
            scanPublicFolderFallback(type, result)
        }

        return result
    }

    private fun scanPublicFolderFallback(type: MediaType, result: MutableList<MediaModel>) {
        try {
            val targetDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            )

            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirFiles(dir, type, result, maxDepth = 2)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaPicker", "Error in public folder fallback scan", e)
        }
    }

    private fun scanDirFiles(dir: File, type: MediaType, result: MutableList<MediaModel>, maxDepth: Int) {
        if (maxDepth <= 0) return
        val files = try { dir.listFiles() } catch (e: Exception) { null } ?: return

        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                scanDirFiles(file, type, result, maxDepth - 1)
            } else {
                val ext = file.extension.lowercase(Locale.getDefault())
                val isVideo = ext in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")
                val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                val isAudio = ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")

                val matches = when (type) {
                    MediaType.VIDEO -> isVideo
                    MediaType.IMAGE -> isImage
                    MediaType.AUDIO -> isAudio
                }

                if (matches) {
                    var durationMs = 0L
                    if (isVideo || isAudio) {
                        durationMs = extractMediaDuration(file)
                    }

                    result.add(
                        MediaModel(
                            id = file.hashCode().toLong(),
                            uri = Uri.fromFile(file),
                            displayName = file.name,
                            durationMs = durationMs,
                            type = type,
                            dateAdded = file.lastModified() / 1000,
                            bucketName = dir.name,
                            localFile = file
                        )
                    )
                }
            }
        }
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

    private inner class MediaAdapter(
        private val items: List<MediaModel>,
        private val onItemClick: (MediaModel) -> Unit
    ) : RecyclerView.Adapter<MediaViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_picker_thumbnail, parent, false)
            return MediaViewHolder(view)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, onItemClick)
        }

        override fun getItemCount(): Int = items.size
    }

    private inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutGridContainer: View = itemView.findViewById(R.id.layoutGridContainer)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        val layoutAudioContainer: View = itemView.findViewById(R.id.layoutAudioContainer)
        val ivAudioCover: com.google.android.material.imageview.ShapeableImageView = itemView.findViewById(R.id.ivAudioCover)
        val tvAudioTitle: TextView = itemView.findViewById(R.id.tvAudioTitle)
        val tvAudioSubTitle: TextView = itemView.findViewById(R.id.tvAudioSubTitle)
        val tvAudioDuration: TextView = itemView.findViewById(R.id.tvAudioDuration)

        fun bind(item: MediaModel, onItemClick: (MediaModel) -> Unit) {
            itemView.tag = item.id

            if (item.type == MediaType.AUDIO) {
                layoutGridContainer.visibility = View.GONE
                layoutAudioContainer.visibility = View.VISIBLE

                tvAudioTitle.text = item.displayName
                val subText = if (item.artist.isNotBlank() && item.artist != "<unknown>") item.artist else item.bucketName
                tvAudioSubTitle.text = subText
                tvAudioDuration.text = formatDuration(item.durationMs)

                lifecycleScope.launch(Dispatchers.IO) {
                    val albumArt = loadAudioAlbumArt(item)
                    withContext(Dispatchers.Main) {
                        if (itemView.tag == item.id) {
                            if (albumArt != null) {
                                ivAudioCover.setImageBitmap(albumArt)
                                ivAudioCover.setPadding(0, 0, 0, 0)
                                ivAudioCover.clearColorFilter()
                            } else {
                                ivAudioCover.setImageResource(R.drawable.ic_audio_24)
                                val density = itemView.context.resources.displayMetrics.density
                                val pad = (10 * density).toInt()
                                ivAudioCover.setPadding(pad, pad, pad, pad)
                                ivAudioCover.setColorFilter(android.graphics.Color.WHITE)
                            }
                        }
                    }
                }
            } else {
                layoutGridContainer.visibility = View.VISIBLE
                layoutAudioContainer.visibility = View.GONE

                ivThumbnail.setImageBitmap(null)

                if (item.type == MediaType.VIDEO) {
                    tvDuration.visibility = View.VISIBLE
                    tvDuration.text = formatDuration(item.durationMs)
                } else {
                    tvDuration.visibility = View.GONE
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = loadThumbnail(item)
                    withContext(Dispatchers.Main) {
                        if (itemView.tag == item.id) {
                            if (bitmap != null) {
                                ivThumbnail.setImageBitmap(bitmap)
                            } else {
                                val placeholder = if (item.type == MediaType.VIDEO) R.drawable.ic_play_24 else R.drawable.ic_image_24
                                ivThumbnail.setImageResource(placeholder)
                            }
                        }
                    }
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun loadAudioAlbumArt(item: MediaModel): Bitmap? {
            val context = itemView.context ?: return null
            return try {
                val retriever = MediaMetadataRetriever()
                if (item.localFile != null) {
                    retriever.setDataSource(item.localFile.absolutePath)
                } else {
                    retriever.setDataSource(context, item.uri)
                }
                val artBytes = retriever.embeddedPicture
                retriever.release()
                if (artBytes != null) {
                    BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        private fun loadThumbnail(item: MediaModel): Bitmap? {
            val context = itemView.context ?: return null
            return try {
                if (item.localFile != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(item.uri, Size(300, 300), null)
                    } else {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(item.localFile.absolutePath)
                        val frame = retriever.frameAtTime
                        retriever.release()
                        frame
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(item.uri, Size(300, 300), null)
                    } else {
                        @Suppress("DEPRECATION")
                        if (item.type == MediaType.VIDEO) {
                            val id = ContentUris.parseId(item.uri)
                            MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null)
                        } else {
                            val id = ContentUris.parseId(item.uri)
                            MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
                        }
                    }
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
