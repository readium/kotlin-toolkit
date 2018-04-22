package org.readium.r2.opds

import org.readium.r2.shared.*
import org.readium.r2.shared.opds.*


enum class OPDS2ParserError(v:String) {
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
     invalidNavigation("Invalid navigation"),

}

public class OPDS2Parser {

}
