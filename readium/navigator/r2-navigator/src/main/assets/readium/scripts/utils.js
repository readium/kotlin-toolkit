
var readium = (function() {
    // Catch JS errors to log them in the app.
    window.addEventListener("error", function(event) {
        Android.logError(event.message, event.filename, event.lineno);
    }, false);

    // Notify native code that the page has loaded.
    window.addEventListener("load", function(){ // on page load
        window.addEventListener("orientationchange", function() {
            onViewportWidthChanged();
            orientationChanged();
            snapCurrentOffset();
        });

        onViewportWidthChanged();
        orientationChanged();
    }, false);

    var last_known_scrollX_position = 0;
    var last_known_scrollY_position = 0;
    var ticking = false;
    var maxScreenX = 0;
    var pageWidth = 1;

    // Position in range [0 - 1].
    function update(position) {
        let positionString = position.toString();
        Android.progressionDidChange(positionString);
    }

    window.addEventListener('scroll', function(e) {
        last_known_scrollY_position = window.scrollY / document.scrollingElement.scrollHeight;
        // Using Math.abs because for RTL books, the value will be negative.
        last_known_scrollX_position = Math.abs(window.scrollX / document.scrollingElement.scrollWidth);

        // Window is hidden
        if (document.scrollingElement.scrollWidth === 0 || document.scrollingElement.scrollHeight === 0) {
            return;
        }

        if (!ticking) {
            window.requestAnimationFrame(function() {
                update(isScrollModeEnabled() ? last_known_scrollY_position : last_known_scrollX_position);
                ticking = false;
            });
        }
        ticking = true;
    });

    function orientationChanged() {
        maxScreenX = (window.orientation === 0 || window.orientation == 180) ? screen.width : screen.height;
    }

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

    // Scroll to the given TagId in document and snap.
    function scrollToId(id) {
        var element = document.getElementById(id);
        if (!element) {
            return;
        }

        element.scrollIntoView();
        snapCurrentOffset()
    }

    // Position must be in the range [0 - 1], 0-100%.
    function scrollToPosition(position, dir) {
//        console.log("scrollToPosition " + position);
        if ((position < 0) || (position > 1)) {
            throw "scrollToPosition() must be given a position from 0.0 to  1.0";
        }

        if (isScrollModeEnabled()) {
            var offset = document.scrollingElement.scrollHeight * position;
            document.scrollingElement.scrollTop = offset;
            // window.scrollTo(0, offset);
        } else {
            var documentWidth = document.scrollingElement.scrollWidth;
            var factor = (dir == 'rtl') ? -1 : 1;
            var offset = documentWidth * position * factor;
            document.scrollingElement.scrollLeft = snapOffset(offset);
        }
    }

    function scrollToStart() {
//        console.log("scrollToStart");
        if (!isScrollModeEnabled()) {
            document.scrollingElement.scrollLeft = 0;
        } else {
            document.scrollingElement.scrollTop = 0;
            window.scrollTo(0, 0);
        }
    }

    function scrollToEnd() {
//        console.log("scrollToEnd");
        if (!isScrollModeEnabled()) {
            document.scrollingElement.scrollLeft = document.scrollingElement.scrollWidth;
        } else {
            document.scrollingElement.scrollTop = document.body.scrollHeight;
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    // Returns false if the page is already at the left-most scroll offset.
    function scrollLeft(dir) {
        var isRTL = (dir == "rtl");
        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX - pageWidth;
        var minOffset = isRTL ? -(documentWidth - pageWidth) : 0;
        return scrollToOffset(Math.max(offset, minOffset));
    }

    // Returns false if the page is already at the right-most scroll offset.
    function scrollRight(dir) {
        var isRTL = (dir == "rtl");
        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX + pageWidth;
        var maxOffset = isRTL ? 0 : (documentWidth - pageWidth);
        return scrollToOffset(Math.min(offset, maxOffset));
    }

    // Scrolls to the given left offset.
    // Returns false if the page scroll position is already close enough to the given offset.
    function scrollToOffset(offset) {
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
        var value = offset + 1;
        return value - (value % pageWidth);
    }

    // Snaps the current offset to the page width.
    function snapCurrentOffset() {
        if (isScrollModeEnabled()) {
            return;
        }
        var currentOffset = window.scrollX;
        // Adds half a page to make sure we don't snap to the previous page.
        document.scrollingElement.scrollLeft = snapOffset(currentOffset + (pageWidth / 2));
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

    /// Toolkit

    function debounce(delay, func) {
        var timeout;
        return function() {
            var self = this;
            var args = arguments;
            function callback() {
                func.apply(self, args);
                timeout = null;
            }
            clearTimeout(timeout);
            timeout = setTimeout(callback, delay);
        };
    }


    // Public API used by the navigator.

    return {
        'scrollToId': scrollToId,
        'scrollToPosition': scrollToPosition,
        'scrollLeft': scrollLeft,
        'scrollRight': scrollRight,
        'scrollToStart': scrollToStart,
        'scrollToEnd': scrollToEnd,
        'setProperty': setProperty,
        'removeProperty': removeProperty,
        'onViewportWidthChanged': onViewportWidthChanged
    };

})();
