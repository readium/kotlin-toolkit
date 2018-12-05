// Notify native code that the page has loaded.
window.addEventListener("load", function(){ // on page load
                        // Notify native code that the page is loaded.
                        //webkit.messageHandlers.didLoad.postMessage("");
                            checkScrollMode();
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

var checkScrollMode = function() {

    var scrollMode = document.documentElement.style.getPropertyValue("--USER__scroll").toString().trim();
    var scroll_on = 'readium-scroll-on'.toString().trim();

    console.log("scrollMode " + scrollMode);
    console.log("scroll_on " + scroll_on);
    console.log("(scrollMode == scroll_on) " + (scrollMode === scroll_on) );

    if(scrollMode == scroll_on) {
        scrolling = true;
    } else {
        scrolling = false;
    }

};


window.addEventListener('scroll', function(e) {
    last_known_scrollY_position = window.scrollY / document.body.scrollHeight;
    last_known_scroll_position = window.scrollX / document.body.scrollWidth;

    checkScrollMode();

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
    var offset = document.body.scrollWidth * position;

    console.log("ScrollToOffset " + offset);
    document.body.scrollLeft = snapOffset(offset);
    update(position);
};

var scrollToEnd = function() {

    checkScrollMode();

    if(!scrolling) {
        console.log("scrollToEnd " + document.body.scrollWidth);
        document.body.scrollLeft = document.body.scrollWidth;
    } else {
        console.log("scrollToBottom " + document.body.scrollHeight);
        document.scrollingElement.scrollTop = document.body.scrollHeight;
        window.scrollTo(0, document.body.scrollHeight);
    }
};

var scrollToStart = function() {

    checkScrollMode();

    if(!scrolling) {
        console.log("scrollToStart " + 0);
        document.body.scrollLeft = 0;
    } else {
        console.log("scrollToTop " + 0);
        document.scrollingElement.scrollTop = 0;
        window.scrollTo(0, 0);
    }
};

var scrollToPosition = function(position, dir) {
    console.log("ScrollToPosition " + position);
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }

    checkScrollMode();

    if(!scrolling) {
        var offset = 0.0;
        if (dir == 'rtl') {
            offset = (-document.body.scrollWidth + maxScreenX) * (1.0-position);
        } else {
            offset = document.body.scrollWidth * position;
        }
        console.log("offset " + offset);
        document.body.scrollLeft = snapOffset(offset);
        update(position);
    } else {
        var offset = document.body.scrollHeight * position;
        console.log(offset);
        document.scrollingElement.scrollTop = offset;
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
        last_known_scroll_position = window.scrollX / document.body.scrollWidth;
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
            document.scrollingElement.scrollLeft = offset
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.scrollingElement.scrollLeft = edge;
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
    var scrollWidth = document.body.scrollWidth;

    if (offset < scrollWidth) {
        document.scrollingElement.scrollLeft = offset;
        last_known_scroll_position = window.scrollX / document.body.scrollWidth;
        console.log("last_known_scroll_position " + last_known_scroll_position);
        update(last_known_scroll_position);
        return "";
    } else {
        document.scrollingElement.scrollLeft = scrollWidth;
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
            document.scrollingElement.scrollLeft = offset
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.scrollingElement.scrollLeft = edge;
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




//Highlighting related
var setHighlight = function() {
        var paragraphs = document.getElementsByClassName("highlighted");
	for (var i=0 ; i<paragraphs.length ; i++) {
		if (paragraphs[i].style.backgroundColor != "yellow") {
			paragraphs[i].style.backgroundColor = "yellow";
		} else {
			var parentNode = paragraphs[i].parentNode;

			var frag = (function() {
				var wrap = document.createElement('div'),
				    fragm = document.createDocumentFragment();
				wrap.innerHTML = paragraphs[i].textContent;
				while (wrap.firstChild) {
				    fragm.appendChild(wrap.firstChild);
				}
				return fragm;
			    })();

			parentNode.insertBefore(frag, paragraphs[i]);
			parentNode.removeChild(paragraphs[i]);
		}
	}
};

var findUtterance = function(searchText, searchNode) {
    var regex = typeof searchText === 'string' ? new RegExp(searchText, 'g') : searchText,
        childNodes = (searchNode || document.body).childNodes,
        cnLength = childNodes.length,
        excludes = 'html,head,style,title,link,meta,script,object,iframe';

    while (cnLength--) {
        var currentNode = childNodes[cnLength];

        if (currentNode.nodeType === 1 && (excludes + ',').indexOf(currentNode.nodeName.toLowerCase() + ',') === -1) {
            arguments.callee(searchText, currentNode);
        }

        if (currentNode.nodeType !== 3 || !currentNode.data.includes(searchText)) {
	        //console.log("(Node " + cnLength + ", " + currentNode.nodeType + ", " + currentNode.data + ") isn't what I'm looking for");
            continue;
        }
	    //console.log("data : " + typeof currentNode.data);

        var parent = currentNode.parentNode;
	    var frag = (function() {
		var spanBegin = "<span class=\"highlighted\">",
                    spanEnd = "</span>";
                var readByTTS = spanBegin + searchText + spanEnd;
                var html = currentNode.nodeValue.replace(regex, readByTTS),
                    wrap = document.createElement('div'),
                    fragm = document.createDocumentFragment();
                wrap.innerHTML = html;
                while (wrap.firstChild) {
                    fragm.appendChild(wrap.firstChild);
                }
                return fragm;
            })();

	    parent.insertBefore(frag, currentNode);
	    parent.removeChild(currentNode);
	    setHighlight();
    }
};