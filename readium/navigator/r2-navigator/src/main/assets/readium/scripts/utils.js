/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

var readium = (function() {
    // Catch JS errors to log them in the app.
    window.addEventListener("error", function(event) {
        Android.logError(event.message, event.filename, event.lineno);
    }, false);

    // Notify native code that the page has loaded.
    window.addEventListener("load", function(){ // on page load
        window.addEventListener("orientationchange", function() {
            onViewportWidthChanged();
            snapCurrentOffset();
        });

        onViewportWidthChanged();
    }, false);

    var pageWidth = 1;

    function onViewportWidthChanged() {
        // We can't rely on window.innerWidth for the pageWidth on Android, because if the
        // device pixel ratio is not an integer, we get rounding issues offsetting the pages.
        //
        // See https://github.com/readium/readium-css/issues/97
        // and https://github.com/readium/r2-navigator-kotlin/issues/146
        var width = Android.getViewportWidth()
        pageWidth = width / window.devicePixelRatio;
        setProperty("--RS__viewportWidth", "calc(" + width + "px / " + window.devicePixelRatio + ")")
    }

    function isScrollModeEnabled() {
        return document.documentElement.style.getPropertyValue("--USER__scroll").toString().trim() == 'readium-scroll-on';
    }

    function isRTL() {
        return document.body.dir.toLowerCase() == 'rtl';
    }

    // Scroll to the given TagId in document and snap.
    function scrollToId(id) {
        var element = document.getElementById(id);
        if (!element) {
            return false;
        }

        element.scrollIntoView({inline: "start", block: "start"});
        snapCurrentOffset()
        return true;
    }

    // Position must be in the range [0 - 1], 0-100%.
    function scrollToPosition(position) {
//        Android.log("scrollToPosition " + position);
        if ((position < 0) || (position > 1)) {
            throw "scrollToPosition() must be given a position from 0.0 to  1.0";
        }

        if (isScrollModeEnabled()) {
            var offset = document.scrollingElement.scrollHeight * position;
            document.scrollingElement.scrollTop = offset;
            // window.scrollTo(0, offset);
        } else {
            var documentWidth = document.scrollingElement.scrollWidth;
            var factor = isRTL() ? -1 : 1;
            var offset = documentWidth * position * factor;
            document.scrollingElement.scrollLeft = snapOffset(offset);
        }
    }

    // Scrolls to the first occurrence of the given text snippet.
    //
    // The expected text argument is a Locator Text object, as defined here:
    // https://readium.org/architecture/models/locators/
    function scrollToText(text) {
        // Wrapper around a browser Selection object.
        function Selection(selection) {
            this.selection = selection;
            this.markedRanges = []
        }

        // Removes all the ranges of the selection.
        Selection.prototype.clear = function() {
            this.selection.removeAllRanges();
        }

        // Saves the current selection ranges, to be restored later with reset().
        Selection.prototype.mark = function() {
            this.markedRanges = []
            for (var i = 0; i < this.selection.rangeCount; i++) {
                this.markedRanges.push(this.selection.getRangeAt(i));
            }
        }

        // Resets the selection with ranges previously saved with mark().
        Selection.prototype.reset = function() {
            this.clear();
            for (var i = 0; i < this.markedRanges.length; i++) {
                this.selection.addRange(this.markedRanges[i]);
            }
        }

        // Returns the text content of the selection.
        Selection.prototype.toString = function() {
            return this.selection.toString();
        }

        // Extends the selection by moving the start and end positions by the given offsets.
        Selection.prototype.adjust = function(offset, length) {
            for (var i = 0; i <= Math.abs(offset); i++) {
                var direction = (offset >= 0 ? "forward" : "backward")
                this.selection.modify("move", direction, "character");
            }
            for (var i = 0; i <= length; i++) {
                this.selection.modify("extend", "forward", "character");
            }
        }

        Selection.prototype.isEmpty = function() {
            return this.selection.isCollapsed
        }

        function removeWhitespaces(s) {
            return s.replace(/\s+/g, "");
        }

        var highlight = text.highlight;
        var before  = text.before || "";
        var after  = text.after || "";
        var snippet = before + highlight + after;
        var safeSnippet = removeWhitespaces(snippet);

        if (!highlight || !safeSnippet) {
            return false;
        }

        var selection = new Selection(window.getSelection());
        // We need to reset any selection to begin the search from the start of the resource.
        selection.clear();

        var found = false;
        while (window.find(text.highlight, true)) {
            if (selection.isEmpty()) {
                break; // Prevents infinite loop in edge cases.
            }

            // Get the surrounding context to compare to the expected snippet.
            selection.mark();
            selection.adjust(-before.length, snippet.length);
            var safeSelection = removeWhitespaces(selection.toString());
            selection.reset();

            if (safeSelection != "" && (safeSnippet.includes(safeSelection) || safeSelection.includes(safeSnippet))) {
                found = true;
                break;
            }
        }

        // Resets the selection otherwise the last found occurrence will be highlighted.
        selection.clear();

        snapCurrentOffset();
        return found;
    }

    function scrollToStart() {
//        Android.log("scrollToStart");
        if (!isScrollModeEnabled()) {
            document.scrollingElement.scrollLeft = 0;
        } else {
            document.scrollingElement.scrollTop = 0;
            window.scrollTo(0, 0);
        }
    }

    function scrollToEnd() {
//        Android.log("scrollToEnd");
        if (!isScrollModeEnabled()) {
            var factor = isRTL() ? -1 : 1;
            document.scrollingElement.scrollLeft = snapOffset(document.scrollingElement.scrollWidth * factor);
        } else {
            document.scrollingElement.scrollTop = document.body.scrollHeight;
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    // Returns false if the page is already at the left-most scroll offset.
    function scrollLeft() {
        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX - pageWidth;
        var minOffset = isRTL() ? -(documentWidth - pageWidth) : 0;
        return scrollToOffset(Math.max(offset, minOffset));
    }

    // Returns false if the page is already at the right-most scroll offset.
    function scrollRight() {
        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX + pageWidth;
        var maxOffset = isRTL() ? 0 : (documentWidth - pageWidth);
        return scrollToOffset(Math.min(offset, maxOffset));
    }

    // Scrolls to the given left offset.
    // Returns false if the page scroll position is already close enough to the given offset.
    function scrollToOffset(offset) {
//        Android.log("scrollToOffset " + offset);
        if (isScrollModeEnabled()) {
            throw "Called scrollToOffset() with scroll mode enabled. This can only be used in paginated mode.";
        }

        var currentOffset = window.scrollX;
        document.scrollingElement.scrollLeft = snapOffset(offset);
        // In some case the scrollX cannot reach the position respecting to innerWidth
        var diff = Math.abs(currentOffset - offset) / pageWidth;
        return (diff > 0.01);
    }

    // Snap the offset to the screen width (page width).
    function snapOffset(offset) {
        var value = offset + (isRTL() ? -1 : 1);
        return value - (value % pageWidth);
    }

    // Snaps the current offset to the page width.
    function snapCurrentOffset() {
//        Android.log("snapCurrentOffset");
        if (isScrollModeEnabled()) {
            return;
        }
        var currentOffset = window.scrollX;
        // Adds half a page to make sure we don't snap to the previous page.
        var factor = isRTL() ? -1 : 1;
        var delta = factor * (pageWidth / 2);
        document.scrollingElement.scrollLeft = snapOffset(currentOffset + delta);
    }

    /// User Settings.

    // For setting user setting.
    function setProperty(key, value) {
        var root = document.documentElement;

        root.style.setProperty(key, value);
    }

    // For removing user setting.
    function removeProperty(key) {
        var root = document.documentElement;

        root.style.removeProperty(key);
    }

    // Public API used by the navigator.
    return {
        'scrollToId': scrollToId,
        'scrollToPosition': scrollToPosition,
        'scrollToText': scrollToText,
        'scrollLeft': scrollLeft,
        'scrollRight': scrollRight,
        'scrollToStart': scrollToStart,
        'scrollToEnd': scrollToEnd,
        'setProperty': setProperty,
        'removeProperty': removeProperty
    };

})();
