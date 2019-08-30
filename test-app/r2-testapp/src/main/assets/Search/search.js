var markSearch = function(searchTerm, searchElement, href, type, title, index) {

    var markElements = [];
    var locators = [];

    var searchDocument = document;

    if (searchElement) {
        var searchDiv = document.createElement('div');
        searchDiv.innerHTML = searchElement;
        searchDocument = searchDiv;
    }

    new Mark(searchDocument).mark(searchTerm, {
        each: function (element) {

            var before = element.previousSibling.nodeValue
            var after = element.nextSibling.nodeValue

            if (before.length > 30) before = "..." + before.substring(before.length - 30, before.length);
            if (after.length > 30) after = after.substring(0, 30) + "...";

            var locator = {
                "href" : href,
                "type": type,
                "title" : title,
                "locations" : {
                    "fragment":  [ "i="+markElements.length ]
                },
                "text": {
                    "after": after,
                    "before": before,
                    "highlight": element.innerHTML
                }
            };

            markElements.push(element)
            locators.push(locator)

        },
        done: function(counter) {
            if (index) {
                jumpToMark(index ? index : 0);
            }
        }
    });

    return locators;


    function jumpToMark(index) {
        let offsetTop = 50;
        let currentClass = "current";

        if (markElements.length) {
            var position;
            var current = markElements[index ? index : 0];
            for (var i = 0; i < markElements.length; i++) {
                markElements[i].classList.remove(currentClass);
            }
            current.className += currentClass;
            checkScrollMode();
            if(!scrolling) {
                current.scrollIntoView();
                var offset = Math.round(window.scrollX + window.innerWidth);
                var scrollWidth = document.scrollingElement.scrollWidth;
                document.scrollingElement.scrollLeft = snapOffset(offset);
            } else {
                position = current.offsetTop - offsetTop;
                window.scrollTo(0, position);
            }
        }
    }

};
