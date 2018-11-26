// Notify native code that the page has loaded.
window.addEventListener("load", function(){ // on page load
                        // Notify native code that the page is loaded.
                        //webkit.messageHandlers.didLoad.postMessage("");
                        }, false);

var last_known_scroll_position = 0;
var last_known_scrollY_position = 0;
var ticking = false;
var scrolling = false;

// Position in range [0 - 1].
var update = function(position) {
    let positionString = position.toString()
    //webkit.messageHandlers.updateProgression.postMessage(positionString);
    Android.progressionDidChange(positionString);
//    console.log("update progression position : " + positionString);
};




window.addEventListener('scroll', function(e) {

    last_known_scrollY_position = window.scrollY / document.getElementsByTagName("body")[0].scrollHeight;
    last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;

    var scroll = document.documentElement.style.getPropertyValue("--USER__scroll").toString().trim();
    var scroll_on = 'readium-scroll-on'.toString().trim();

    console.log("scroll " + scroll);
    console.log("scroll_on " + scroll_on);
    console.log("(scroll == scroll_on) " + (scroll === scroll_on) );

    if(scroll == scroll_on) {
        scrolling = true;
    } else {
        scrolling = false;
    }

    if (!ticking) {
        window.requestAnimationFrame(function() {
            if(scrolling) {
                update(last_known_scrollY_position);
                console.log("last_known_scrollY_position " + last_known_scrollY_position);
            } else {
                update(last_known_scroll_position);
                console.log("last_known_scroll_position " + last_known_scroll_position);
            }
            ticking = false;
        });
    }
    ticking = true;
    return false;
});

// Scroll to the given TagId in document and snap.
var scrollToId = function(id) {
    var element = document.getElementById(id);
    var elementOffset = element.scrollLeft // element.getBoundingClientRect().left works for Gutenbergs books
    var offset = window.scrollX + elementOffset;

    document.body.scrollLeft = snapOffset(offset);
};

// Position must be in the range [0 - 1], 0-100%.
var scrollToPosition = function(position) {
    console.log("ScrollToPosition " + position);
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }
    var offset = document.getElementsByTagName("body")[0].scrollWidth * position;

    console.log("ScrollToOffset " + offset);
    document.body.scrollLeft = snapOffset(offset);
    update(position);
};

var scrollToEnd = function() {
    if(!scrolling) {
        console.log("scrollToEnd " + document.getElementsByTagName("body")[0].scrollWidth);
        document.body.scrollLeft = document.getElementsByTagName("body")[0].scrollWidth;
    } else {
        console.log("scrollToBottom " + document.getElementsByTagName("body")[0].scrollHeight);
        var body = (document.documentElement || document.body.parentNode || document.body)
        body.scrollTop = document.body.scrollHeight;
        window.scrollTo(0, document.body.scrollHeight);
    }
};

var scrollToStart = function() {
    if(!scrolling) {
        console.log("scrollToStart " + 0);
        document.body.scrollLeft = 0;
    } else {
        console.log("scrollToTop " + 0);
        var body = (document.documentElement || document.body.parentNode || document.body)
        body.scrollTop = 0;
        window.scrollTo(0, 0);
    }
};

var scrollToPosition = function(position, dir) {
    console.log("ScrollToPosition");
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }
    if(!scrolling) {
        var offset = 0.0;
        if (dir == 'rtl') {
            offset = (-document.getElementsByTagName("body")[0].scrollWidth + maxScreenX) * (1.0-position);
        } else {
            offset = document.getElementsByTagName("body")[0].scrollWidth * position;
        }
        console.log(offset);
        document.body.scrollLeft = snapOffset(offset);
        update(position);
    } else {
        var offset = document.getElementsByTagName("body")[0].scrollHeight * position;
        console.log(offset);
        var body = (document.documentElement || document.body.parentNode || document.body)
        body.scrollTop = offset;
        window.scrollTo(0, offset);
        update(position);
    }
};

var scrollLeft = function() {
    console.log("scrollLeft");
    scrollToPosition(last_known_scroll_position, 'ltr')
    var offset = window.scrollX - maxScreenX;

    if (offset >= 0) {
        document.body.scrollLeft = offset;
        last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;
        update(last_known_scroll_position);
        return "";
    } else {
        document.body.scrollLeft = 0;
        update(1.0);
        return "edge"; // Need to previousDocument.
    }
};

var scrollLeftRTL = function() {
    console.log("scrollLeftRTL");

    var scrollWidth = document.body.scrollWidth;
    var offset = window.scrollX - maxScreenX;
    var edge = -scrollWidth + window.innerWidth;

    if (window.innerWidth == scrollWidth) {
        // No scroll and default zoom
        return "edge";
    } else {
        // Scrolled and zoomed
        if (offset > edge) {
            document.body.scrollLeft = offset
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.body.scrollLeft = edge;
            var diff = Math.abs(edge-oldOffset)/window.innerWidth;
            // In some case the scrollX cannot reach the position respecting to innerWidth
            if (diff > 0.01) {
                return 0;
            } else {
                return "edge";
            }
        }
    }
};

var scrollRight = function() {
    console.log("scrollRight");
    scrollToPosition(last_known_scroll_position, 'ltr')
    var offset = window.scrollX + maxScreenX;
    var scrollWidth = document.getElementsByTagName("body")[0].scrollWidth;

    if (offset < scrollWidth) {
        document.body.scrollLeft = offset;
        last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;
        update(last_known_scroll_position);
        return "";
    } else {
        document.body.scrollLeft = scrollWidth;
        update(0.0);
        return "edge"; // Need to nextDocument.
    }
};

var scrollRightRTL = function() {
   console.log("scrollRightRTL");

    var scrollWidth = document.body.scrollWidth;
    var offset = window.scrollX + window.innerWidth;
    var edge = 0;

    if (window.innerWidth == scrollWidth) {
        // No scroll and default zoom
        return "edge";
    } else {
        // Scrolled and zoomed
        if (offset < edge) {
            document.body.scrollLeft = offset
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.body.scrollLeft = edge;
            var diff = Math.abs(edge-oldOffset)/window.innerWidth;
            // In some case the scrollX cannot reach the position respecting to innerWidth
            if (diff > 0.01) {
                return 0;
            } else {
                return "edge";
            }
        }
    }
};

// Snap the offset to the screen width (page width).
var snapOffset = function(offset) {
    let value = offset + 1;

    return value - (value % maxScreenX);
};

/// User Settings.

// For setting user setting.
var setProperty = function(key, value) {
    var root = document.documentElement;

    root.style.setProperty(key, value);
};

// For removing user setting.
var removeProperty = function(key) {
    var root = document.documentElement;

    root.style.removeProperty(key);
};
