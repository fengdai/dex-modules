package com.github.fengdai

import com.android.dex.ClassDef
import com.android.dex.Dex
import com.android.dex.DexFormat
import com.android.dx.cf.direct.DirectClassFile
import com.android.dx.cf.direct.StdAttributeFactory
import com.android.dx.dex.DexOptions
import com.android.dx.dex.cf.CfOptions
import com.android.dx.dex.cf.CfTranslator
import com.android.dx.dex.file.DexFile
import java.io.ByteArrayInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object RClasses {
    private val CLASS_MAGIC = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
    private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00)

    /** List method references in the bytes of any `.dex`, `.class`, `.jar`, `.aar`, or `.apk`. */
    @JvmStatic fun list(bytes: List<ByteArray>): List<RClass> {
        val collection = bytes
                .fold(ClassAndDexCollection()) { collection, bytes ->
                    if (bytes.startsWith(DEX_MAGIC)) {
                        collection.dexes += bytes
                    } else if (bytes.startsWith(CLASS_MAGIC)) {
                        collection.classes += bytes
                    } else {
                        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                            zis.entries().forEach {
                                if (it.name.endsWith(".dex")) {
                                    collection.dexes += zis.readBytes()
                                } else if (it.name.endsWith(".class")) {
                                    collection.classes += zis.readBytes()
                                } else if (it.name.endsWith(".jar")) {
                                    ZipInputStream(ByteArrayInputStream(zis.readBytes())).use { jar ->
                                        jar.entries().forEach {
                                            if (it.name.endsWith(".class")) {
                                                collection.classes += jar.readBytes()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    collection
                }

        if (collection.classes.isNotEmpty()) {
            collection.dexes += classesToDex(collection.classes)
        }
        return collection.dexes
                .map { Dex(it) }
                .flatMap { dex -> dex.classDefs().filter { isRClass(dex, it) }.map { toRClass(dex, it) } }
    }

    private fun isRClass(dex: Dex, classDef: ClassDef): Boolean {
        return RClass.NAMES.contains(typeOnly(typeName(dex, classDef)))
    }

    private fun typeName(dex: Dex, classDef: ClassDef): String {
        val typeName = dex.typeNames()[classDef.typeIndex]
        return typeName.substring(1, typeName.length - 1).replace('/', '.')
    }

    private fun typeOnly(typeName: String): String {
        return typeName.substring(typeName.lastIndexOf(".") + 1, typeName.length)
    }

    private fun toRClass(dex: Dex, classDef: ClassDef): RClass {
        val typeName = typeName(dex, classDef)
        val classData = dex.readClassData(classDef)
        val fieldNames = classData.staticFields.map {
            dex.strings()[dex.nameIndexFromFieldIndex(it.fieldIndex)]
        }.toList()
        return RClass(typeName, fieldNames)
    }

    private fun classesToDex(bytes: List<ByteArray>): ByteArray {
        val dexOptions = DexOptions()
        dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES
        val dexFile = DexFile(dexOptions)

        bytes.forEach {
            val cf = DirectClassFile(it, "None.class", false)
            cf.setAttributeFactory(StdAttributeFactory.THE_ONE)
            CfTranslator.translate(cf, it, CfOptions(), dexOptions, dexFile)
        }

        return dexFile.toDex(null, false)
    }

    private fun ByteArray.startsWith(value: ByteArray): Boolean {
        if (value.size > size) return false
        value.forEachIndexed { i, byte ->
            if (get(i) != byte) {
                return false
            }
        }
        return true
    }

    private fun ZipInputStream.entries(): Sequence<ZipEntry> {
        return object : Sequence<ZipEntry> {
            override fun iterator(): Iterator<ZipEntry> {
                return object : Iterator<ZipEntry> {
                    var next: ZipEntry? = null

                    override fun hasNext(): Boolean {
                        next = nextEntry
                        return next != null
                    }

                    override fun next() = next!!
                }
            }
        }
    }

    internal class ClassAndDexCollection {
        val classes = ArrayList<ByteArray>()
        val dexes = ArrayList<ByteArray>()
    }
}
