var singleTouchGesture = false;
var startX = 0;
var startY = 0;

window.addEventListener("load", function() { // on page load
    // Get screen X and Y sizes.
    // Events listeners for the touches.
    window.document.addEventListener("touchstart", handleTouchStart, false);
    window.document.addEventListener("touchend", handleTouchEnd, false);
    window.document.addEventListener("click", handleClick, true);
    // When device orientation changes, screen X and Y sizes are recalculated.
}, false);


var handleClick = function(event) {
    Android.handleClick(event.target.outerHTML)
};


// When a touch is detected records its starting coordinates and if it's a singleTouchGesture.
var handleTouchStart = function(event) {
    var node = event.target.nodeName.toUpperCase()
    if (node === 'A' || node === 'VIDEO') {
        console.log("Touched a link or video.");
        // singleTouchGesture = false;
        return;
    }
    console.log("Touch sent to native code.");
    singleTouchGesture = event.touches.length == 1;

    var touch = event.changedTouches[0];

    startX = touch.clientX % document.documentElement.clientWidth;
    startY = touch.clientY % document.documentElement.clientHeight;

};

// When a touch ends, check if any action has to be made, and contact native code.
var handleTouchEnd = function(event) {
    if (!singleTouchGesture) {
        return;
    }

    var touch = event.changedTouches[0];

    var clientWidth = document.documentElement.clientWidth;
    var clientHeight = document.documentElement.clientHeight;
    var relativeDistanceX = Math.abs(((touch.clientX % clientWidth) - startX) / clientWidth);
    var relativeDistanceY = Math.abs(((touch.clientY % clientHeight) - startY) / clientHeight);
    var touchDistance = Math.max(relativeDistanceX, relativeDistanceY);

    if (touchDistance < 0.01) {
        var position = (touch.clientX % clientWidth) / clientWidth;
        if (position <= 0.2) {
            console.log("LeftTapped");
            Android.scrollLeft(false);
        } else if (position >= 0.8) {
            console.log("RightTapped");
            Android.scrollRight(false);
        } else {
            console.log("CenterTapped");
            Android.centerTapped();
        }
        event.stopPropagation();
        event.preventDefault();
        return;
    }
};
