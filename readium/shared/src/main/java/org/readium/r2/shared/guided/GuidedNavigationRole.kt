/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.guided

import kotlinx.serialization.Serializable

/**
 * A role usable in a guided navigation object.
 */
@Serializable
@JvmInline
public value class GuidedNavigationRole(public val value: String) {

    public companion object {

        /*
         * Inherited from HTML and/or ARIA
         */

        public val ASIDE: GuidedNavigationRole = GuidedNavigationRole("aside")
        public val CELL: GuidedNavigationRole = GuidedNavigationRole("cell")
        public val DEFINITION: GuidedNavigationRole = GuidedNavigationRole("definition")
        public val FIGURE: GuidedNavigationRole = GuidedNavigationRole("figure")
        public val LIST: GuidedNavigationRole = GuidedNavigationRole("list")
        public val LIST_ITEM: GuidedNavigationRole = GuidedNavigationRole("listItem")
        public val ROW: GuidedNavigationRole = GuidedNavigationRole("row")
        public val TABLE: GuidedNavigationRole = GuidedNavigationRole("table")
        public val TERM: GuidedNavigationRole = GuidedNavigationRole("term")

        /*
         * Inherited from DPUB ARIA 1.0
         */

        public val ABSTRACT: GuidedNavigationRole = GuidedNavigationRole("abstract")
        public val ACKNOWLEDGMENTS: GuidedNavigationRole = GuidedNavigationRole("acknowledgments")
        public val AFTERWORD: GuidedNavigationRole = GuidedNavigationRole("afterword")
        public val APPENDIX: GuidedNavigationRole = GuidedNavigationRole("appendix")
        public val BACKLINK: GuidedNavigationRole = GuidedNavigationRole("backlink")
        public val BIBLIOGRAPHY: GuidedNavigationRole = GuidedNavigationRole("bibliography")
        public val BIBLIOREF: GuidedNavigationRole = GuidedNavigationRole("biblioref")
        public val CHAPTER: GuidedNavigationRole = GuidedNavigationRole("chapter")
        public val COLOPHON: GuidedNavigationRole = GuidedNavigationRole("colophon")
        public val CONCLUSION: GuidedNavigationRole = GuidedNavigationRole("conclusion")
        public val COVER: GuidedNavigationRole = GuidedNavigationRole("cover")
        public val CREDIT: GuidedNavigationRole = GuidedNavigationRole("credit")
        public val CREDITS: GuidedNavigationRole = GuidedNavigationRole("credits")
        public val DEDICATION: GuidedNavigationRole = GuidedNavigationRole("dedication")
        public val ENDNOTES: GuidedNavigationRole = GuidedNavigationRole("endnotes")
        public val EPIGRAPH: GuidedNavigationRole = GuidedNavigationRole("epigraph")
        public val EPILOGUE: GuidedNavigationRole = GuidedNavigationRole("epilogue")
        public val ERRATA: GuidedNavigationRole = GuidedNavigationRole("errata")
        public val EXAMPLE: GuidedNavigationRole = GuidedNavigationRole("example")
        public val FOOTNOTE: GuidedNavigationRole = GuidedNavigationRole("footnote")
        public val GLOSSARY: GuidedNavigationRole = GuidedNavigationRole("glossary")
        public val GLOSSREF: GuidedNavigationRole = GuidedNavigationRole("glossref")
        public val INDEX: GuidedNavigationRole = GuidedNavigationRole("index")
        public val INTRODUCTION: GuidedNavigationRole = GuidedNavigationRole("introduction")
        public val NOTEREF: GuidedNavigationRole = GuidedNavigationRole("noteref")
        public val NOTICE: GuidedNavigationRole = GuidedNavigationRole("notice")
        public val PAGEBREAK: GuidedNavigationRole = GuidedNavigationRole("pagebreak")
        public val PAGELIST: GuidedNavigationRole = GuidedNavigationRole("page-list")
        public val PART: GuidedNavigationRole = GuidedNavigationRole("part")
        public val PREFACE: GuidedNavigationRole = GuidedNavigationRole("preface")
        public val PROLOGUE: GuidedNavigationRole = GuidedNavigationRole("prologue")
        public val PULLQUOTE: GuidedNavigationRole = GuidedNavigationRole("pullquote")
        public val QNA: GuidedNavigationRole = GuidedNavigationRole("qna")
        public val SUBTITLE: GuidedNavigationRole = GuidedNavigationRole("subtitle")
        public val TIP: GuidedNavigationRole = GuidedNavigationRole("tip")
        public val TOC: GuidedNavigationRole = GuidedNavigationRole("toc")

        /*
         * Inherited from EPUB 3 Structural Semantics Vocabulary 1.1
         */

        public val LANDMARKS: GuidedNavigationRole = GuidedNavigationRole("landmarks")
        public val LOA: GuidedNavigationRole = GuidedNavigationRole("loa")
        public val LOI: GuidedNavigationRole = GuidedNavigationRole("loi")
        public val LOT: GuidedNavigationRole = GuidedNavigationRole("lot")
        public val LOV: GuidedNavigationRole = GuidedNavigationRole("lov")
    }
}
