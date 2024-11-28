/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2024. Kovi <kovihome86@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.reqsmith.composer.repository.api

import org.apache.commons.io.FileUtils
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.parser.ReferenceExtractor
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.parser.entities.Ref
import dev.reqsmith.composer.parser.entities.References
import dev.reqsmith.composer.parser.entities.ReqMSource
import dev.reqsmith.composer.repository.api.entities.ItemCollection
import dev.reqsmith.composer.repository.api.entities.RepositoryIndex
import dev.reqsmith.composer.repository.api.entities.ReqMRepository
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

/**
 * Find repositories, load indices, find and load reqm sources needed to merge
 */
class RepositoryFinder {
    private val stdReqMLib = ReqMRepository()
    private val reqMCache = ReqMRepository()
//    private val externalRepo = ReqMRepository()
    private val projectRepo = ReqMRepository()

    private val id = ";" // index delimiter

    fun find(itemType: Ref.Type, t: String, domain: String?): ItemCollection {
        val name = if (domain != null) "${domain}.$t" else t
        val indices : MutableList<RepositoryIndex.IndexRecord> = ArrayList()
        findInRepo(projectRepo, indices, name, itemType)
        findInRepo(stdReqMLib, indices, name, itemType)
        findInRepo(reqMCache, indices, name, itemType)

        val ic = ItemCollection()
        for (ix in indices) {
            ic.items.add(ItemCollection.ItemCollectionItem(ix.recType, ix.itemType, ix.name, 1.0, ix.filename))
        }
        return ic
    }

    private fun findInRepo(repo: ReqMRepository, indices: MutableList<RepositoryIndex.IndexRecord>, name: String, itemType: Ref.Type) {
        repo.indices.forEach { ri ->
            indices.addAll(ri.index.filter { it.name == name && it.recType == RepositoryIndex.RecordType.content && it.itemType == itemType })
        }
    }

    fun connect(appHome: String): Boolean {
        // 1. locate standard reqm library
        val stdlibDir = Path.of("$appHome/stdlib").absolute().normalize()
        stdReqMLib.path = stdlibDir
        // Debug
        Log.info("Standard ReqM Library directory = ${stdReqMLib.path}")

        if (stdReqMLib.path!!.notExists()) {
            // Error
            Log.error("Standard ReqM Library directory ${stdReqMLib.path} does not exists.")
            return false
        }

        val ok = loadIndices(stdReqMLib)
        if (!ok) {
            // Error
            Log.error("loading index of ${stdReqMLib.path} failed.")
            return false
        }

        // 2. locate reqm cache, create, if it does not exist
        val userDir = FileUtils.getUserDirectory()
        val cacheDir = Path.of("$userDir/.reqm/cache")
        reqMCache.path = cacheDir
        // Debug
        Log.info("ReqM Cache directory = ${reqMCache.path}")

        if (reqMCache.path!!.notExists()) {
            // Info
            Log.info("ReqM Cache directory ${reqMCache.path} does not exists; create it")
            try {
                cacheDir.createDirectory()
            } catch (e : Exception) {
                // Error
                Log.error("Creating ReqM Cache directory ${reqMCache.path} failed: ${e.localizedMessage}")
            }
        }

        // 3. configure external repo
        // TODO: get external repo configuration

        return true
    }

    private fun loadIndices(repo: ReqMRepository): Boolean {
        // traverse the directory tree
        File(repo.path!!.absolutePathString()).walk().filter { it.isDirectory }.forEach {
            Log.debug("load index file from $it")
            loadIndex(repo, it.toPath())
        }
        return true
    }

    private fun loadIndex(repo: ReqMRepository, path: Path): Boolean {
        val repoIndexFile = File("${path}/index")
        val index = if (!repoIndexFile.exists()) buildIndexFile(path) else loadIndexFile(repoIndexFile)
        if (index.index.isNotEmpty()) repo.indices.add(index)
        return true
    }

    private fun loadIndexFile(repoIndexFile: File): RepositoryIndex {
        val index = RepositoryIndex()
        val reader = FileReader(repoIndexFile)
        val lines = reader.readLines()
        for (line in lines) {
            val s = line.split(id)
            val ix = RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.valueOf(s[0]), Ref.Type.valueOf(s[1]), s[2], s[3])
            index.index.add(ix)
        }
        reader.close()
        return index
    }

    private fun buildIndexFile(path: Path?): RepositoryIndex {

        val index = RepositoryIndex()

        val files = path!!.toFile().listFiles()?.filter { it.extension == "reqm" }
        if (files.isNullOrEmpty()) return index

        Log.text("Build index file for $path")
        val parser = ReqMParser()
        val extractor = ReferenceExtractor()

        val reqmsrc = ReqMSource()
        val ok = parser.parseFolder(path.absolutePathString(), reqmsrc)

        if (ok) {

            // search repository for reqm source elements
            val refs = extractor.extract(reqmsrc)

            addToIndex(index, refs)

            saveIndex(path, index)

        }

        return index
    }

    private fun saveIndex(path: Path, index: RepositoryIndex) {
        val writer = FileWriter("$path/index")
        for (ix in index.index) {
            writer.append("${ix.recType}$id${ix.itemType}$id${ix.name}$id${ix.filename}\n")
        }
        writer.flush()
        writer.close()
    }

    private fun addToIndex(index: RepositoryIndex, refs: References, filename: String = "") {
        for (item in refs.items) {
            val ix = RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.content, item.type, item.qid.toString(), item.filename)
            if (!findIndex(index, ix)) {
                index.index.add(ix)
            }
        }
        for (srcref in refs.sourceRefs) {
            val ix = RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.dependency, srcref.type, srcref.referred.toString(), srcref.filename)
            if (!findIndex(index, ix) && !findIndex(index, RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.content, srcref.type, srcref.referred.toString(), srcref.filename))) {
                index.index.add(ix)
            }
        }
        for (t in refs.types) {
            val ix = RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.dependency, Ref.Type.cls, t.qid.toString(), t.filename)
            if (!findIndex(index, ix) && !findIndex(index, RepositoryIndex.IndexRecord(RepositoryIndex.RecordType.content, Ref.Type.cls, t.qid.toString(), filename)) && !findIndex(index, RepositoryIndex.IndexRecord(
                    RepositoryIndex.RecordType.content, Ref.Type.ent, t.qid.toString(), t.filename))) {
                index.index.add(ix)
            }
        }
    }

    private fun findIndex(index: RepositoryIndex, ix: RepositoryIndex.IndexRecord): Boolean {
        for (rec in index.index) {
            if (rec.toString() == ix.toString()) {
                return true
            }
        }
        return false
    }

    fun addProjectIndex(refs : References) {
        val index = RepositoryIndex()
        addToIndex(index, refs)
        projectRepo.indices.add(index)
    }

}
