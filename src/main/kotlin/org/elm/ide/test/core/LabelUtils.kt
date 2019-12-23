package org.elm.ide.test.core

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths

object LabelUtils {
    val DESCRIBE_PROTOCOL = "elmTestDescribe"
    val TEST_PROTOCOL = "elmTestTest"
    val ERROR_PROTOCOL = "elmTestError"

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

    fun decodeLabel(encoded: Path): String {
        return decodeLabel(pathString(encoded))
    }

    fun decodeLabel(encoded: String): String {
        try {
            return URLDecoder.decode(encoded, "utf8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    fun toPath(labels: List<String>): Path {
        val encoded = labels
                .asSequence()
                .map { encodeLabel(it) }

        return if (encoded.count() < 1) {
            EMPTY_PATH
        } else {
            Paths.get(
                    encoded.first(),
                    *encoded.drop(1).toList().toTypedArray())
        }
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

    fun subParents(path: Path, excludeParent: Path): Sequence<Path> {
        if (excludeParent === EMPTY_PATH) {
            var current: Path? = path
            return generateSequence {
                current = current?.parent
                current
            }
        }

        if (!path.startsWith(excludeParent)) {
            throw IllegalStateException("not parent")
        }

        if (path === EMPTY_PATH) {
            return sequenceOf()
        }

        var current: Path? = path
        return generateSequence {
            current = current?.parent
            if (current != excludeParent) {
                current
            } else {
                null
            }
        }
    }

}

data class ErrorLabelLocation(
        val file: String,
        val line: Int,
        val column: Int
) {
    fun toUrl() =
            String.format("%s://%s::%d::%d", LabelUtils.ERROR_PROTOCOL, file, line, column)

    companion object {
        fun fromUrl(spec: String): ErrorLabelLocation {
            val parts = spec.split("::").dropLastWhile { it.isEmpty() }
            return ErrorLabelLocation(
                    file = parts[0],
                    line = if (parts.size > 1) Integer.parseInt(parts[1]) else 1,
                    column = if (parts.size > 2) Integer.parseInt(parts[2]) else 1
            )
        }
    }
}