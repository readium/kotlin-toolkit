package org.readium.r2.shared

class RootFile(){

    constructor(rootPath: String = "", rootFilePath: String = "", mimetype: String = "", version: Double? = null) : this() {
        this.rootPath = rootPath
        this.rootFilePath = rootFilePath
        this.mimetype = mimetype
        this.version = version
    }

    var rootPath: String = ""
    //  Path to OPF
    var rootFilePath: String = ""
    var mimetype: String = ""
    var version: Double? = null

}