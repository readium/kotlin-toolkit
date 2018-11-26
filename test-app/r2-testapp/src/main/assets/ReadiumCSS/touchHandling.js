var singleTouchGesture = false;
var startX = 0;
var startY = 0;
var maxScreenX = window.screen.availWidth;
var maxScreenY = window.screen.availHeight;


window.addEventListener("load", function(){ // on page load
                        // Get screen X and Y sizes.
                        // Events listeners for the touches.
                        window.document.addEventListener("touchstart", handleTouchStart, false);
                        window.document.addEventListener("touchend", handleTouchEnd, false);
                        window.document.addEventListener("click",handleClick,true);
                        // When device orientation changes, screen X and Y sizes are recalculated.
                        }, false);


var handleClick = function(event) {
    Android.handleClick(event.target.outerHTML)
};


// When a touch is detected records its starting coordinates and if it's a singleTouchGesture.
var handleTouchStart = function(event) {
    if (event.target.nodeName.toUpperCase() === 'A') {
        console.log("Touched a link.");
        // singleTouchGesture = false;
        return;
    }
    console.log("Touch sent to native code.");
    singleTouchGesture = event.touches.length == 1;

    var touch = event.changedTouches[0];

    startX = touch.screenX % maxScreenX;
    startY = touch.screenY % maxScreenY;

};

// When a touch ends, check if any action has to be made, and contact native code.
var handleTouchEnd = function(event) {
    if(!singleTouchGesture) {
        return;
    }

    var touch = event.changedTouches[0];

    var relativeDistanceX = Math.abs(((touch.screenX % maxScreenX) - startX) / maxScreenX);
    var relativeDistanceY = Math.abs(((touch.screenY % maxScreenY) - startY) / maxScreenY);
    var touchDistance = Math.max(relativeDistanceX, relativeDistanceY);

    var scrollWidth = document.scrollWidth;
    var screenWidth = maxScreenX;
    var tapAreaWidth = maxScreenX * 0.2;

    if(touchDistance < 0.01) {
        var position = (touch.screenX % maxScreenX) / maxScreenX;
        if (position <= 0.2) {
            console.log("LeftTapped");
            Android.scrollLeft();
        } else if(position >= 0.8) {
            console.log("RightTapped");
            Android.scrollRight();
        } else {
            console.log("CenterTapped");
            Android.centerTapped();
        }
        event.stopPropagation();
        event.preventDefault();
        return;
    }
};
