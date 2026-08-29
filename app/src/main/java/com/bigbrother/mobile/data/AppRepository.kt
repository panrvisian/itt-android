package com.bigbrother.mobile.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.bigbrother.mobile.domain.TimeUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class NoteEditorState(val text: String, val imageNames: List<String>)
data class NoteViewState(val text: String, val imageNames: List<String>)

class AppRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore
) {
    private val groupsDao = database.groups()
    private val eventsDao = database.events()
    private val recordsDao = database.records()
    private val noteImagesDao = database.noteImages()
    private val noteDraftStore = NoteDraftStore(context)
    private val zone: ZoneId = ZoneId.systemDefault()

    val groups: Flow<List<GroupEntity>> = groupsDao.observeAll()
    val events: Flow<List<EventEntity>> = eventsDao.observeAll()
    val records: Flow<List<RecordEntity>> = recordsDao.observeAll()
    val noteImages: Flow<List<NoteImageEntity>> = noteImagesDao.observeAll()
    val settings: Flow<AppSettings> = settingsStore.flow

    suspend fun initialize() {
        ensureSystemGroup()
        normalizeOvernight()
        cleanupNoteDrafts()
    }

    suspend fun ensureSystemGroup() = database.withTransaction {
        ensureSystemGroupInTransaction()
    }

    private suspend fun ensureSystemGroupInTransaction() {
        if (groupsDao.getAllOnce().none { it.isSystem }) {
            groupsDao.insert(GroupEntity(name = "未分组", colorArgb = 0xFF9E9E9E.toInt(), isSystem = true, isDeleted = false, sortOrder = 0))
        }
    }

    suspend fun addGroup(name: String, colorArgb: Int): String = database.withTransaction {
        val nextOrder = (groupsDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val group = GroupEntity(name = name, colorArgb = colorArgb, sortOrder = nextOrder)
        groupsDao.insert(group)
        group.id
    }

    suspend fun renameGroup(groupId: String, name: String) = database.withTransaction {
        groupsDao.rename(groupId, name)
        recordsDao.syncGroupName(groupId, name)
    }

    suspend fun changeGroupColor(groupId: String, colorArgb: Int) = database.withTransaction {
        groupsDao.updateColor(groupId, colorArgb)
        recordsDao.syncGroupColor(groupId, colorArgb)
    }

    suspend fun moveGroup(groupId: String, direction: Int) = database.withTransaction {
        val visible = groupsDao.getAllOnce()
            .filter { !it.isDeleted && !it.isSystem }
            .sortedWith(compareBy<GroupEntity> { it.sortOrder }.thenBy { it.name })
            .toMutableList()
        val index = visible.indexOfFirst { it.id == groupId }
        if (index < 0) return@withTransaction
        val target = (index + direction.coerceIn(-1, 1)).coerceIn(0, visible.lastIndex)
        if (target == index) return@withTransaction
        val item = visible.removeAt(index)
        visible.add(target, item)
        visible.forEachIndexed { order, group -> groupsDao.updateSortOrder(group.id, order + 1) }
    }

    suspend fun deleteGroup(groupId: String): Boolean = database.withTransaction {
        val group = groupsDao.getById(groupId) ?: return@withTransaction false
        if (group.isSystem) return@withTransaction false
        if (recordsDao.countRunningByGroup(groupId) > 0) return@withTransaction false
        if (eventsDao.getAllOnce().any { it.groupId == groupId && !it.isDeleted }) return@withTransaction false
        groupsDao.setDeleted(groupId, true)
        true
    }

    suspend fun addEvent(groupId: String, name: String): String = database.withTransaction {
        val nextOrder = (eventsDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val event = EventEntity(groupId = groupId, name = name, sortOrder = nextOrder)
        eventsDao.insert(event)
        event.id
    }

    suspend fun renameEvent(eventId: String, name: String) = database.withTransaction {
        eventsDao.rename(eventId, name)
        recordsDao.syncEventName(eventId, name)
    }

    suspend fun moveEvent(eventId: String, groupId: String) = database.withTransaction {
        eventsDao.move(eventId, groupId)
        val event = eventsDao.getById(eventId) ?: return@withTransaction
        val group = groupsDao.getById(groupId) ?: return@withTransaction
        recordsDao.syncEventGroup(event.id, group.id, group.name, group.colorArgb)
    }

    suspend fun toggleFavorite(eventId: String, favorite: Boolean) = database.withTransaction {
        eventsDao.setFavorite(eventId, favorite)
    }

    suspend fun deleteEvent(eventId: String): Boolean = database.withTransaction {
        if (recordsDao.countRunningByEvent(eventId) > 0) return@withTransaction false
        eventsDao.setDeleted(eventId, true)
        true
    }

    suspend fun startEvent(eventId: String): String = database.withTransaction {
        val event = eventsDao.getById(eventId) ?: error("event not found")
        val group = groupsDao.getById(event.groupId) ?: error("group not found")
        val record = RecordEntity(
            eventId = event.id,
            eventNameSnapshot = event.name,
            groupIdSnapshot = group.id,
            groupNameSnapshot = group.name,
            groupColorArgbSnapshot = group.colorArgb,
            startTime = System.currentTimeMillis()
        )
        recordsDao.insert(record)
        record.id
    }

    suspend fun addManualRecord(eventId: String, startTime: Long, endTime: Long): String = database.withTransaction {
        val event = eventsDao.getById(eventId) ?: error("event not found")
        val group = groupsDao.getById(event.groupId) ?: error("group not found")
        val record = RecordEntity(
            eventId = event.id,
            eventNameSnapshot = event.name,
            groupIdSnapshot = group.id,
            groupNameSnapshot = group.name,
            groupColorArgbSnapshot = group.colorArgb,
            startTime = startTime,
            endTime = endTime
        )
        recordsDao.insert(record)
        normalizeOvernightInTransaction()
        record.id
    }

    suspend fun cloneRecord(recordId: String, eventId: String): String? = database.withTransaction {
        val source = recordsDao.getById(recordId) ?: return@withTransaction null
        val event = eventsDao.getById(eventId) ?: return@withTransaction null
        if (event.isDeleted) return@withTransaction null
        val group = groupsDao.getById(event.groupId) ?: return@withTransaction null

        val cloned = RecordEntity(
            eventId = event.id,
            eventNameSnapshot = event.name,
            groupIdSnapshot = group.id,
            groupNameSnapshot = group.name,
            groupColorArgbSnapshot = group.colorArgb,
            startTime = source.startTime,
            endTime = source.endTime
        )
        recordsDao.insert(cloned)

        // 已结束记录保持现有跨天拆分规则；进行中记录保留相同开始时间和空结束时间。
        if (cloned.endTime != null) {
            normalizeOvernightInTransaction()
        }
        cloned.id
    }

    suspend fun endRecord(recordId: String): Boolean = database.withTransaction {
        val record = recordsDao.getById(recordId) ?: return@withTransaction false
        if (record.endTime != null) return@withTransaction true
        recordsDao.end(recordId, System.currentTimeMillis())
        normalizeOvernightInTransaction()
        true
    }

    suspend fun deleteRunningRecord(recordId: String): Boolean = database.withTransaction {
        val record = recordsDao.getById(recordId) ?: return@withTransaction false
        if (record.endTime != null) return@withTransaction false
        recordsDao.deleteById(recordId)
        deleteNoteData(recordId)
        true
    }

    suspend fun updateRecord(recordId: String, eventId: String, startTime: Long, endTime: Long?) = database.withTransaction {
        val event = eventsDao.getById(eventId) ?: return@withTransaction
        val group = groupsDao.getById(event.groupId) ?: return@withTransaction
        val existing = recordsDao.getById(recordId)
        recordsDao.deleteById(recordId)
        recordsDao.insert(
            RecordEntity(
                id = recordId,
                eventId = event.id,
                eventNameSnapshot = event.name,
                groupIdSnapshot = group.id,
                groupNameSnapshot = group.name,
                groupColorArgbSnapshot = group.colorArgb,
                startTime = startTime,
                endTime = endTime,
                isContinuation = false,
                noteText = existing?.noteText ?: ""
            )
        )
        normalizeOvernightInTransaction()
    }

    suspend fun deleteHistoryRecord(recordId: String) = database.withTransaction {
        recordsDao.deleteById(recordId)
        deleteNoteData(recordId)
    }

    suspend fun normalizeOvernight(now: Long = System.currentTimeMillis()) = database.withTransaction {
        normalizeOvernightInTransaction(now)
    }

    private suspend fun normalizeOvernightInTransaction(now: Long = System.currentTimeMillis()) {
        val all = recordsDao.getAllOnce().sortedBy { it.startTime }
        for (record in all) {
            val actualEnd = record.endTime ?: now
            if (actualEnd <= record.startTime) continue
            val startDate = TimeUtils.toLocalDate(record.startTime)
            val endDate = TimeUtils.toLocalDate(actualEnd)
            if (startDate == endDate) continue
            val noteImages = noteImagesDao.getByRecord(record.id)
            recordsDao.deleteById(record.id)
            var firstSegmentId: String? = null
            var segmentStart = record.startTime
            var day = startDate
            while (true) {
                val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val isLastDay = day == endDate
                val segmentEnd = if (record.endTime == null && isLastDay) null else minOf(actualEnd, nextMidnight)
                val segmentId = newId()
                if (firstSegmentId == null) firstSegmentId = segmentId
                recordsDao.insert(
                    record.copy(
                        id = segmentId,
                        startTime = segmentStart,
                        endTime = segmentEnd,
                        isContinuation = record.isContinuation || day != startDate
                    )
                )
                if (segmentEnd == null || segmentEnd >= actualEnd) break
                segmentStart = nextMidnight
                day = day.plusDays(1)
            }
            if (noteImages.isNotEmpty() && firstSegmentId != null) {
                reassignNoteImages(record.id, firstSegmentId, noteImages)
            }
        }
    }

    suspend fun exportBundle(): AppBundle = AppBundle(
        settings = settings.first(),
        groups = groupsDao.getAllOnce(),
        events = eventsDao.getAllOnce(),
        records = recordsDao.getAllOnce().sortedBy { it.startTime },
        noteImages = noteImagesDao.getAllOnce()
    )

    suspend fun exportCsvText(): String = CsvCodec.export(exportBundle())

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val bundle = exportBundle()
        val zipBytes = buildZip(bundle)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(zipBytes)
        }
    }

    suspend fun importCsv(uri: Uri, merge: Boolean) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext
        val (csvText, imageEntries) = if (isZip(bytes)) readZip(bytes) else Pair(bytes.toString(Charsets.UTF_8), emptyList())
        val imported = CsvCodec.parse(csvText.removePrefix("\uFEFF"))
        if (!merge) {
            clearAllNoteFiles()
            noteDraftStore.clearAll()
        }
        imageEntries.forEach { (relPath, data) -> writeNoteImageFile(relPath, data) }
        val validImages = imported.noteImages.filter { noteImageFile(it.recordId, it.fileName).exists() }
        val bundle = imported.copy(noteImages = validImages)
        if (!merge) replaceAll(bundle) else mergeIntoCurrent(bundle)
    }

    suspend fun replaceAll(bundle: AppBundle) = database.withTransaction {
        recordsDao.deleteAll()
        eventsDao.deleteAll()
        groupsDao.deleteAll()
        noteImagesDao.deleteAll()
        bundle.groups.forEach { groupsDao.insert(it) }
        bundle.events.forEach { eventsDao.insert(it) }
        bundle.records.forEach { recordsDao.insert(it) }
        bundle.noteImages.forEach { noteImagesDao.insert(it) }
        settingsStore.set(bundle.settings)
        ensureSystemGroupInTransaction()
        normalizeOvernightInTransaction()
    }

    suspend fun mergeIntoCurrent(bundle: AppBundle) = database.withTransaction {
        val currentGroups = groupsDao.getAllOnce()
        val currentEvents = eventsDao.getAllOnce()
        val groupMap = mutableMapOf<String, String>()
        bundle.groups.forEach { imported ->
            val existing = currentGroups.firstOrNull { it.name == imported.name }
            val targetId = existing?.id ?: imported.id
            groupMap[imported.id] = targetId
            if (existing == null) groupsDao.insert(imported.copy(id = targetId))
        }
        val eventMap = mutableMapOf<String, String>()
        val systemGroupId = groupsDao.getAllOnce().firstOrNull { it.isSystem }?.id.orEmpty()
        bundle.events.forEach { imported ->
            val targetGroupId = groupMap[imported.groupId] ?: systemGroupId
            val existing = currentEvents.firstOrNull { it.name == imported.name && it.groupId == targetGroupId }
            val targetId = existing?.id ?: imported.id
            eventMap[imported.id] = targetId
            if (existing == null) eventsDao.insert(imported.copy(id = targetId, groupId = targetGroupId))
        }
        bundle.records.forEach { imported ->
            val targetEventId = eventMap[imported.eventId] ?: return@forEach
            val event = eventsDao.getById(targetEventId) ?: return@forEach
            val group = groupsDao.getById(event.groupId) ?: return@forEach
            recordsDao.insert(
                imported.copy(
                    id = imported.id,
                    eventId = targetEventId,
                    eventNameSnapshot = event.name,
                    groupIdSnapshot = group.id,
                    groupNameSnapshot = group.name,
                    groupColorArgbSnapshot = group.colorArgb
                )
            )
        }
        bundle.noteImages.forEach { image ->
            if (recordsDao.getById(image.recordId) != null) {
                noteImagesDao.insert(image.copy(id = newId()))
            }
        }
    }

    // ---------- Notes ----------

    suspend fun loadNoteView(recordId: String): NoteViewState {
        val record = recordsDao.getById(recordId)
        val images = noteImagesDao.getByRecord(recordId).map { it.fileName }
        return NoteViewState(text = record?.noteText ?: "", imageNames = images)
    }

    suspend fun loadNoteEditor(recordId: String): NoteEditorState {
        if (noteDraftStore.hasDraft(recordId)) {
            return NoteEditorState(
                text = noteDraftStore.loadText(recordId),
                imageNames = noteDraftStore.loadImages(recordId)
            )
        }
        val record = recordsDao.getById(recordId)
        val savedImages = noteImagesDao.getByRecord(recordId)
        val draftDir = draftDirFor(recordId)
        draftDir.mkdirs()
        savedImages.forEach { image ->
            val src = noteImageFile(recordId, image.fileName)
            val dst = File(draftDir, image.fileName)
            if (src.exists() && !dst.exists()) {
                runCatching { src.copyTo(dst, overwrite = true) }
            }
        }
        val names = savedImages.map { it.fileName }
        val text = record?.noteText ?: ""
        noteDraftStore.save(recordId, text, names)
        return NoteEditorState(text = text, imageNames = names)
    }

    suspend fun copyImageToDraft(recordId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val name = newId() + guessImageExtension(uri)
        val dir = draftDirFor(recordId).apply { mkdirs() }
        val target = File(dir, name)
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (target.exists() && target.length() > 0) name else null
    }

    suspend fun removeDraftImageFile(recordId: String, fileName: String) {
        withContext(Dispatchers.IO) {
            runCatching { File(draftDirFor(recordId), fileName).delete() }
        }
    }

    suspend fun saveNoteDraft(recordId: String, text: String, imageNames: List<String>) {
        noteDraftStore.save(recordId, text, imageNames)
    }

    suspend fun saveNote(recordId: String, text: String, imageNames: List<String>) = database.withTransaction {
        recordsDao.getById(recordId) ?: return@withTransaction
        recordsDao.updateNoteText(recordId, text)
        val notesDir = notesDirFor(recordId)
        runCatching { notesDir.deleteRecursively() }
        notesDir.mkdirs()
        noteImagesDao.deleteByRecord(recordId)
        val draftDir = draftDirFor(recordId)
        imageNames.forEachIndexed { index, name ->
            val src = File(draftDir, name)
            val dst = File(notesDir, name)
            if (src.exists()) runCatching { src.copyTo(dst, overwrite = true) }
            noteImagesDao.insert(NoteImageEntity(recordId = recordId, fileName = name, sortOrder = index))
        }
        noteDraftStore.clear(recordId)
        runCatching { draftDir.deleteRecursively() }
    }

    suspend fun cleanupNoteDrafts(now: Long = System.currentTimeMillis()) {
        val oneDay = 24L * 60L * 60L * 1000L
        val records = recordsDao.getAllOnce()
        noteDraftStore.allDraftRecordIds().forEach { recordId ->
            val record = records.firstOrNull { it.id == recordId }
            val shouldDelete = when {
                record == null -> true
                record.endTime == null -> false
                else -> record.endTime < now - oneDay
            }
            if (shouldDelete) {
                noteDraftStore.clear(recordId)
                withContext(Dispatchers.IO) {
                    runCatching { draftDirFor(recordId).deleteRecursively() }
                }
            }
        }
    }

    fun noteImageFile(recordId: String, fileName: String): File = File(notesDirFor(recordId), fileName)

    fun draftImageFile(recordId: String, fileName: String): File = File(draftDirFor(recordId), fileName)

    private fun notesDirFor(recordId: String): File = File(File(context.filesDir, "notes"), recordId)

    private fun draftDirFor(recordId: String): File = File(File(context.filesDir, "notes_draft"), recordId)

    private fun notesRootDir(): File = File(context.filesDir, "notes")

    private fun draftsRootDir(): File = File(context.filesDir, "notes_draft")

    private suspend fun deleteNoteData(recordId: String) {
        noteImagesDao.deleteByRecord(recordId)
        noteDraftStore.clear(recordId)
        runCatching { notesDirFor(recordId).deleteRecursively() }
        runCatching { draftDirFor(recordId).deleteRecursively() }
    }

    private suspend fun reassignNoteImages(oldRecordId: String, newRecordId: String, images: List<NoteImageEntity>) {
        noteImagesDao.deleteByRecord(oldRecordId)
        images.forEachIndexed { index, image ->
            noteImagesDao.insert(image.copy(id = newId(), recordId = newRecordId, sortOrder = index))
        }
        runCatching {
            val oldDir = notesDirFor(oldRecordId)
            val newDir = notesDirFor(newRecordId)
            if (oldDir.exists()) {
                newDir.parentFile?.mkdirs()
                oldDir.renameTo(newDir)
            }
        }
    }

    private fun guessImageExtension(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)?.lowercase()
        val fromMime = when {
            mime == "image/png" -> ".png"
            mime == "image/webp" -> ".webp"
            mime == "image/gif" -> ".gif"
            mime == "image/jpeg" || mime == "image/jpg" -> ".jpg"
            else -> null
        }
        if (fromMime != null) return fromMime
        val name = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (name) {
            "png" -> ".png"
            "webp" -> ".webp"
            "gif" -> ".gif"
            else -> ".jpg"
        }
    }

    private fun buildZip(bundle: AppBundle): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("big_brother_mobile.csv"))
            zip.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            zip.write(CsvCodec.export(bundle).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            bundle.noteImages.forEach { image ->
                val file = noteImageFile(image.recordId, image.fileName)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("notes/${image.recordId}/${image.fileName}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return bos.toByteArray()
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun readZip(bytes: ByteArray): Pair<String, List<Pair<String, ByteArray>>> {
        var csv = ""
        val images = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val data = zip.readBytes()
                when {
                    entry.name.endsWith(".csv", ignoreCase = true) -> csv = data.toString(Charsets.UTF_8)
                    entry.name.startsWith("notes/") -> images += entry.name to data
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return csv to images
    }

    private fun writeNoteImageFile(relPath: String, data: ByteArray) {
        runCatching {
            val root = context.filesDir.canonicalFile
            val target = File(root, relPath).canonicalFile
            if (!target.path.startsWith(root.path + File.separator)) return@runCatching
            target.parentFile?.mkdirs()
            target.writeBytes(data)
        }
    }

    private suspend fun clearAllNoteFiles() {
        withContext(Dispatchers.IO) {
            runCatching { notesRootDir().deleteRecursively() }
            runCatching { draftsRootDir().deleteRecursively() }
        }
    }

    suspend fun setSettings(update: (AppSettings) -> AppSettings) {
        settingsStore.update(update)
    }

    suspend fun currentSettings(): AppSettings = settings.first()
}




