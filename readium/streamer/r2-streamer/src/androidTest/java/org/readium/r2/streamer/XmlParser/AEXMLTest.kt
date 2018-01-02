package org.readium.r2.streamer.XmlParser

import junit.framework.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class AEXMLTest {

    private val aexml = XmlParser()

    @Test
    fun parseXml(){
        val filePath = "/sdcard/test.xml"
        val epubFile = File(filePath)
        val fis = FileInputStream(epubFile)
        aexml.parseXml(fis)
        val node1 = aexml.root()!!
        val nodea = node1.children[0]
        val nodeb = node1.getFirst("nodeb")!!
        val nodeb1 = nodeb.getFirst("nodeb1")!!.properties["prop1"]!!
        val nodec = node1.getFirst("nodec")!!
        assertTrue(!nodea.properties.isEmpty()
        && nodeb1 == "oui" && nodec.text == "texte")
    }

}