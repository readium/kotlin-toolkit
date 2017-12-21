package org.readium.r2.shared

import org.json.JSONObject
import java.io.Serializable

class Metadata : Serializable {

    /// The structure used for the serialisation.
    var multilangTitle: MultilangString? = null
    /// The title of the publication.
    var title: String = ""
        get() = multilangTitle?.singleString ?: ""

    var languages: MutableList<String> = mutableListOf()
    lateinit var identifier: String
    // Contributors.
    var authors: MutableList<Contributor> = mutableListOf()
    var translators: MutableList<Contributor> = mutableListOf()
    var editors: MutableList<Contributor> = mutableListOf()
    var artists: MutableList<Contributor> = mutableListOf()
    var illustrators: MutableList<Contributor> = mutableListOf()
    var letterers: MutableList<Contributor> = mutableListOf()
    var pencilers: MutableList<Contributor> = mutableListOf()
    var colorists: MutableList<Contributor> = mutableListOf()
    var inkers: MutableList<Contributor> = mutableListOf()
    var narrators: MutableList<Contributor> = mutableListOf()
    var imprints: MutableList<Contributor> = mutableListOf()
    var direction:String = "default"
    var subjects: MutableList<Subject> = mutableListOf()
    var publishers: MutableList<Contributor> = mutableListOf()
    var contributors: MutableList<Contributor> = mutableListOf()
    var modified: String? = null
    var publicationDate: String? = null
    var description: String? = null
    var rendition: Rendition = Rendition()
    var source: String? = null
    var epubType: MutableList<String> = mutableListOf()
    var rights: String? = null
    var otherMetadata: MutableList<MetadataItem> = mutableListOf()

    fun titleForLang(key: String) : String?  = multilangTitle?.multiString?.get(key)

    fun writeJSON() : JSONObject{
        val obj = JSONObject()
        obj.putOpt("languages", getStringArray(languages))
        obj.putOpt("publicationDate", publicationDate)
        obj.putOpt("identifier", identifier)
        obj.putOpt("modified", modified)
        obj.putOpt("title", title)
        obj.putOpt("rendition", rendition.getJSON())
        obj.putOpt("source", source)
        obj.putOpt("rights", rights)
        tryPut(obj, subjects, "subjects")
        tryPut(obj, authors, "authors")
        tryPut(obj, translators, "translators")
        tryPut(obj, editors, "editors")
        tryPut(obj, artists, "artists")
        tryPut(obj, illustrators, "illustrators")
        tryPut(obj, letterers, "letterers")
        tryPut(obj, pencilers, "pencilers")
        tryPut(obj, colorists, "colorists")
        tryPut(obj, inkers, "inkers")
        tryPut(obj, narrators, "narrators")
        tryPut(obj, contributors, "contributors")
        tryPut(obj, publishers, "publishers")
        tryPut(obj, imprints, "imprints")
        return obj
    }

}