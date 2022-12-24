    $(function() {

    getDocNames() ;
    } );


    function myFunction() {
      alert("The PDF document was submitted");
    }

    function analyzeDoc() {

     // Get the doc
     var name = $("#docs").val();

     $.ajax('/analyzeDoc', {
        type: 'POST',  // http method
        data: 'name=' + name ,  // data to submit
        success: function (data, status, xhr) {
            var xml = data
            $(xml).find('Item').each(function () {

                var $field = $(this);
                var doc = $field.find('Doc').text();

                var oldValue = $("#textarea1").val();

                if (oldValue === "") {
                    $("#textarea1").val(doc);
                } else {
                    $("#textarea1").val(oldValue + "\n" + doc);
                }
           });
        },
        error: function (jqXhr, textStatus, errorMessage) {
            $('p').append('Error' + errorMessage);
        }
      });
      }

     function getDocNames() {

     // Clear the current drop down.
     $("#docs").empty()

       $.ajax('/getdocs', {
         type: 'GET',  // http method
         success: function (data, status, xhr) {

            var xml = data;
            $(xml).find('Doc').each(function () {
               var $field = $(this);
               var name = $field.find('Key').text();
          
              $('#docs').append($('<option/>', {
                    value: name,
                    text : name
                }));
            });
           },
        error: function (jqXhr, textStatus, errorMessage) {
            $('p').append('Error' + errorMessage);
        }
      });
     }