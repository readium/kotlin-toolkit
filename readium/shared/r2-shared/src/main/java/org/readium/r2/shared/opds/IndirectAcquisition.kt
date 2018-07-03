package org.readium.r2.shared.opds

import org.json.JSONObject
import java.io.Serializable


data class IndirectAcquisition(var typeAcquisition: String):Serializable {
    var child = mutableListOf<IndirectAcquisition>()

}

enum class IndirectAcquisitionError(v:String) {
    invalidJSON("OPDS 2 manifest is not valid JSON"),
    metadataNotFound("Metadata not found"),
    invalidMetadata("Invalid metadata"),
    invalidLink("Invalid link"),
    invalidIndirectAcquisition("Invalid indirect acquisition"),
    missingTitle("Missing title"),
    invalidFacet("Invalid facet"),
    invalidGroup("Invalid group"),
    invalidPublication("Invalid publication"),
    invalidContributor("Invalid contributor"),
    invalidCollection("Invalid collection"),
    invalidNavigation("Invalid navigation")
}

fun parseIndirectAcquisition(indirectAcquisitionDict: JSONObject) : IndirectAcquisition {
    val indirectAcquisitionType = indirectAcquisitionDict["type"] as? String ?: throw Exception(IndirectAcquisitionError.invalidIndirectAcquisition.name)
    val indirectAcquisition = IndirectAcquisition(typeAcquisition = indirectAcquisitionType)
    val childDict = indirectAcquisitionDict.getJSONObject("child")
    val child = parseIndirectAcquisition(indirectAcquisitionDict = childDict)
    indirectAcquisition.child.add(child)
    return indirectAcquisition
}
