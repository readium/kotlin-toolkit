function test(ok) {
    return ok;
};


//This function perform search and returns a list of results
function performSearch(keyword, element) {
      //console.log("IN PERFORM");

      var resList = [];
      var results = 0;
      var htmlObject = document.createElement('div');
      htmlObject.innerHTML = element;
      console.log(htmlObject);
      var markInstance = new Mark(htmlObject);
      //Performing search
      markInstance.mark(keyword, {
        done: function(counter) {
            console.log(counter);
        }
      });
};