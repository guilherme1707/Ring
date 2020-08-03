$(document).ready(function () {

    $('.tbList').toggleClass("dispDef");

    $('#table').DataTable({
        "paging": false,
        "info": false,
        "order": [],
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                targets: 'img-sort',
                "type": "alt-string"
            },
            {
                targets: 'no-search',
                "searchable": false
            }
        ]
    });

    $('#table_paginated').DataTable({
        "pageLength": 100,
        "bLengthChange": false,
        "info": false,
        "order": [],
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                targets: 'img-sort',
                "type": "alt-string"
            },
            {
                targets: 'no-search',
                "searchable": false
            }
        ]
    });

    $('[id^=table_clean]').DataTable({
        "paging": false,
        "info": false,
        "searching": false,
        "order": [],
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                targets: 'img-sort',
                "type": "alt-string"
            },
            {
                targets: 'no-search',
                "searchable": false
            }
        ]
    });

    $('[id^=table_paginated_clean]').DataTable({
        "pageLength": 10,
        "bLengthChange": false,
        "info": false,
        "searching": false,
        "order": [],
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                targets: 'img-sort',
                "type": "alt-string"
            },
            {
                targets: 'no-search',
                "searchable": false
            }
        ]
    });

    $('#table_heatmap').DataTable({
        "pageLength": 100,
        "bLengthChange": false,
        "info": false,
        "order": [],
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                "targets": '_all',
                "createdCell": function (td, data) {
                    if (data < 1) {
                        $(td).css('background-color', '#ffffff');
                    } else if (data == 1) {
                        $(td).css('background-color', '#eaf4ff');
                    } else if (data == 2) {
                        $(td).css('background-color', '#d6ebff');
                    } else if (data == 3) {
                        $(td).css('background-color', '#c3e1ff');
                    } else if (data == 4) {
                        $(td).css('background-color', '#afd7ff');
                    } else if (data >= 5) {
                        $(td).css('background-color', '#9bcdff');
                    }
                }
            }
        ]
    });

    $("#menu-toggle").click(function (e) {
        e.preventDefault();
        $("#wrapper").toggleClass("active");
    });

    $("#date-filter").datepicker({
        format: "yyyy-mm-dd",
        todayHighlight: true
    });
    
    $('[id^=query_stored_]').DataTable({        
        "pageLength": 10,
        "bLengthChange": false,
        "info": false,
        "order": [],        
        "columnDefs": [{
                "targets": 'no-sort',
                "orderable": false
            },
            {
                targets: 'img-sort',
                "type": "alt-string"
            },
            {
                targets: 'no-search',
                "searchable": false
            }
        ]
    });
});