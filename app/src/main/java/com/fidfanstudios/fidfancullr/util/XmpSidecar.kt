package com.fidfanstudios.fidfancullr.util

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.fidfanstudios.fidfancullr.data.ColorTag
import com.fidfanstudios.fidfancullr.data.PhotoGroup

/** Lightweight Lightroom/Bridge-compatible XMP sidecar writer. */
object XmpSidecar {
    fun write(context: Context, folder: DocumentFile, group: PhotoGroup, rating: Int, tag: ColorTag, prefix: String = "", suffix: String = ""): Boolean {
        val name = "${prefix}${group.stem}${suffix}.xmp"
        val existing = folder.findFile(name)
        val file = existing ?: folder.createFile("application/rdf+xml", name) ?: return false
        val label = when (tag) { ColorTag.RED -> "Red"; ColorTag.YELLOW -> "Yellow"; ColorTag.GREEN -> "Green"; ColorTag.NONE -> "" }
        val xml = """<?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="FidFan Cullr">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"><rdf:Description xmlns:xmp="http://ns.adobe.com/xap/1.0/" xmlns:lr="http://ns.adobe.com/lightroom/1.0/" xmp:Rating="$rating" lr:Label="$label"/></rdf:RDF></x:xmpmeta><?xpacket end="w"?>"""
        return try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(xml.toByteArray(Charsets.UTF_8)) } ?: return false
            true
        } catch (_: Exception) { false }
    }
}
