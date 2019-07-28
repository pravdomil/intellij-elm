package org.frawa.elmtest.core

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import org.elm.workspace.compiler.ElmLocation
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object LabelUtils {
    val ELM_TEST_PROTOCOL = "elmTest"
    val DESCRIBE_PROTOCOL = ELM_TEST_PROTOCOL + "Describe"
    private val TEST_PROTOCOL = ELM_TEST_PROTOCOL + "Test"
    val ERROR_PROTOCOL = ELM_TEST_PROTOCOL + "Error"

    val EMPTY_PATH = Paths.get("")

    private fun getModuleName(path: Path): String {
        return pathString(path.getName(0))
    }

    private fun encodeLabel(label: String): String {
        try {
            return URLEncoder.encode(label, "utf8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }

    }

    internal fun decodeLabel(encoded: Path): String {
        try {
            return URLDecoder.decode(pathString(encoded), "utf8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }

    }

    fun toPath(labels: List<String>): Path {
        val encoded = labels.map { encodeLabel(it) }
        return if (encoded.isEmpty()) {
            EMPTY_PATH
        } else Paths.get(
                encoded[0],
                *encoded.subList(1, encoded.size).toTypedArray()
        )
    }

    fun pathString(path: Path): String {
        return FileUtil.toSystemIndependentName(path.toString())
    }

    fun getName(path: Path): String {
        return decodeLabel(path.fileName)
    }

    fun toSuiteLocationUrl(path: Path): String {
        return toLocationUrl(DESCRIBE_PROTOCOL, path)
    }

    fun toTestLocationUrl(path: Path): String {
        return toLocationUrl(TEST_PROTOCOL, path)
    }

    private fun toLocationUrl(protocol: String, path: Path): String {
        return String.format("%s://%s", protocol, pathString(path))
    }

    fun fromLocationUrlPath(path: String): Pair<String, String> {
        val path1 = Paths.get(path)
        val moduleName = getModuleName(path1)
        val moduleFile = String.format("tests/%s.elm", moduleName.replace(".", "/"))
        val label = if (path1.nameCount > 1) decodeLabel(path1.subpath(1, path1.nameCount)) else ""
        return Pair(moduleFile, label)
    }

    fun commonParent(path1: Path?, path2: Path): Path {
        if (path1 == null) {
            return EMPTY_PATH
        }
        if (path1.nameCount > path2.nameCount) {
            return commonParent(path2, path1)
        }
        return if (path2.startsWith(path1)) {
            path1
        } else {
            commonParent(path1.parent, path2)
        }
    }

    fun subParents(path: Path, excludeParent: Path): List<Path> {
        if (excludeParent === EMPTY_PATH) {
            // TODO remove duplication with below
            val result = ArrayList<Path>()
            var current: Path? = path.parent
            while (current != null) {
                result.add(current)
                current = current.parent
            }
            return result
        }

        if (!path.startsWith(excludeParent)) {
            throw IllegalStateException("not parent")
        }

        if (path === EMPTY_PATH) {
            return emptyList()
        }

        val result = ArrayList<Path>()
        var current = path.parent
        while (current != excludeParent) {
            result.add(current)
            current = current.parent
        }
        return result
    }

    fun fromErrorLocationUrlPath(spec: String): Pair<String, Pair<Int, Int>> {
        val parts = spec.split("::".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val file = parts[0]
        val line = if (parts.size > 1) Integer.parseInt(parts[1]) else 1
        val column = if (parts.size > 2) Integer.parseInt(parts[2]) else 1
        return Pair(file, Pair(line, column))
    }

}

fun ElmLocation.toTestErrorLocationUrl(): String {
    // TODO [kl] expand the error location URL to include the end line & column
    val (line, column) = region?.let { it.start.line to it.start.column } ?: 0 to 0
    return "%s://%s::%d::%d".format(LabelUtils.ERROR_PROTOCOL, path, line, column)
}

