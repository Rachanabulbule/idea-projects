$(document).ready(function () {

    slide("", 1, "completed", 10);

    $('#search-text').keyup(function () {
        var filter = $('input[name="session-filter"]:checked').val();
        var pageSize = $('#show-entries').val();
        slide(this.value, 1, filter, pageSize);
    });

    $('.custom-checkbox').click(function () {
        var filter = $('input[name="session-filter"]:checked').val();
        var pageSize = $('#show-entries').val();
        slide($('#search-text').val(), 1, filter, pageSize);
    });

    $('.mobile-custom-checkbox').click(function () {
        var filter = $('input[name="mobile-session-filter"]:checked').val();
        var pageSize = $('#show-entries-mobile').val();
        slide($('#search-text-mobile').val(), 1, filter, pageSize);
    });

    $('#show-entries').on('change', function () {
        var filter = $('input[name="session-filter"]:checked').val();
        var keyword = $('#search-text').val();
        slide(keyword, 1, filter, this.value);
    });

    $('#search-text-mobile').keyup(function () {
        var filter = $('input[name="mobile-session-filter"]:checked').val();
        var pageSize = $('#show-entries-mobile').val();
        slide(this.value, 1, filter, pageSize);
    });

    $('#show-entries-mobile').on('change', function () {
        var filter = $('input[name="mobile-session-filter"]:checked').val();
        var keyword = $('#search-text-mobile').val();
        slide(keyword, 1, filter, this.value);
    });

    document.getElementById("default-check").checked = true;
    document.getElementById("mobile-default-check").checked = true;

});

function slide(keyword, pageNumber, filter, pageSize) {
    var email = keyword;
    var formData = new FormData();
    formData.append("email", email);
    formData.append("page", pageNumber);
    formData.append("filter", filter);
    formData.append("pageSize", pageSize);

    jsRoutes.controllers.SessionsController.searchSessions().ajax(
        {
            type: 'POST',
            processData: false,
            contentType: false,
            data: formData,
            beforeSend: function (request) {
                var csrfToken = document.getElementById('csrfToken').value;

                return request.setRequestHeader('CSRF-Token', csrfToken);
            },
            success: function (data) {
                var sessionInfo = JSON.parse(data);
                var sessions = sessionInfo["sessions"];
                var page = sessionInfo["page"];
                var pages = sessionInfo["pages"];
                var usersFound = "";
                var mobileSessionsFound = "";
                if (sessions.length > 0) {
                    for (var session = 0; session < sessions.length; session++) {

                        mobileSessionsFound += "<tr class='session-topic'><td class='session-topic' colspan='2'>" + sessions[session].topic + "</td></tr>" +
                            "<tr class='session-info'><td>" +
                            "<p>" + sessions[session].email + "</p>" +
                            "<p>" + sessions[session].dateString + "</p>" +
                            "</td>" + "<td>";

                        usersFound += "<tr>" +
                            "<td>" + sessions[session].dateString + "</td>" +
                            "<td>" + sessions[session].session + "</td>" +
                            "<td>" + sessions[session].topic + "</td>" +
                            "<td>" + sessions[session].email + "</td>";

                        if (sessions[session].meetup) {
                            usersFound += '<td><span class="label label-info meetup-session ">Meetup</span></td>';
                            mobileSessionsFound += '<span class="label label-info meetup-session ">Meetup</span>';
                        } else {
                            usersFound += '<td><span class="label label-info knolx-session ">Knolx</span></td>';
                            mobileSessionsFound += '<span class="label label-info knolx-session ">Knolx</span>';
                        }

                        if (sessions[session].cancelled) {
                            usersFound += "<td class='suspended'>Yes</td>";
                        } else {
                            usersFound += "<td class='active-status'>No</td>";
                        }

                        if (sessions[session].completed && !sessions[session].cancelled) {
                            usersFound += "<td><div><span class='label label-success' >Completed</span></div></td>";
                            mobileSessionsFound += "<div><span class='label label-success' >Completed</span></div>";
                        } else if (sessions[session].cancelled) {
                            usersFound += "<td><div><span class='label label-warning cancelled-session'>Cancelled</span></div></td>";
                            mobileSessionsFound += "<div><span class='label label-warning cancelled-session'>Cancelled</span></div>";
                        } else {
                            usersFound += "<td><div><span class='label label-warning' >Pending</span><br/></div></td>";
                            mobileSessionsFound += "<div><span class='label label-warning' >Pending</span><br/></div>";
                        }

                        if (sessions[session].completed && !sessions[session].cancelled) {
                            if (sessions[session].contentAvailable) {
                                usersFound += "<td  title='Click here for slides & videos' class='clickable-row'>" +
                                    "<a href='" + jsRoutes.controllers.SessionsController.shareContent(sessions[session].id)['url'] +
                                    "' style='text-decoration: none;' target='_blank'><span class='label more-detail-session'>Click here</span></a></td>";

                                mobileSessionsFound += "<a href='" + jsRoutes.controllers.SessionsController.shareContent(sessions[session].id)['url'] +
                                    "' style='text-decoration: none;' target='_blank'><span class='label more-detail-session'>Click here</span></a>";
                            } else {
                                usersFound += "<td><span class='label label-danger'>Not Available</span></td>";
                            }
                        } else if (sessions[session].cancelled) {
                            usersFound += "<td title='The session has been cancelled'><span class='label label-warning cancelled-session'>Cancelled</span></td>";
                        }
                        else if (!sessions[session].completed) {
                            usersFound += "<td title='Wait for session to be completed'><span class='label label-warning'>Pending</span></td>";
                        }
                        usersFound += "</tr>";

                        mobileSessionsFound += "</td><tr class='row-space'></tr>";

                        $('#user-found').html(usersFound);
                        $('#main-session-tbody-mobile').html(mobileSessionsFound);
                    }

                    var totalSessions = sessionInfo["totalSessions"];
                    var startingRange = (pageSize * (page - 1)) + 1;
                    var endRange = (pageSize * (page - 1)) + sessions.length;

                    $('#starting-range').html(startingRange);
                    $('#ending-range').html(endRange);
                    $('#total-range').html(totalSessions);

                    paginate(page, pages);

                    var paginationLinks = document.querySelectorAll('.paginate');
                    var width = screen.width;
                    if(width > 768) {
                        for (var i = 0; i < paginationLinks.length; i++) {
                            paginationLinks[i].addEventListener('click', function () {
                                var filter = $('input[name="session-filter"]:checked').val();
                                var keyword = document.getElementById('search-text').value;
                                slide(keyword, this.id, filter, pageSize);
                            });
                        }
                    } else {
                        for (var i = 0; i < paginationLinks.length; i++) {
                            paginationLinks[i].addEventListener('click', function () {
                                var filter = $('input[name="mobile-session-filter"]:checked').val();
                                var keyword = document.getElementById('search-text').value;
                                slide(keyword, this.id, filter, pageSize);
                            });
                        }
                    }
                } else {
                    $('#user-found').html(
                        "<tr><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-6'><i class='fa fa-database' aria-hidden='true'></i><span class='no-record-found'>Oops! No Record Found</span></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td></tr>"
                    );

                    $('#main-session-tbody-mobile').html(
                        "<tr class='no-record-mobile'><td align='center' class='col-md-12' colspan='2'><i class='fa fa-database' aria-hidden='true'></i><span class='no-record-found'>Oops! No Record Found</span></td></tr>"
                    );

                    $('#starting-range').html('0');
                    $('#ending-range').html('0');
                    $('#total-range').html('0');
                    $('.pagination').html("");
                }
            },
            error: function (er) {
                $('#user-found').html(
                    "<tr><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-6'>" + er.responseText + "</td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td><td align='center' class='col-md-1'></td></tr>"
                );

                $('#main-session-tbody-mobile').html(
                    "<tr><td align='center'>" + er.responseText + "</td></tr>"
                );
                $('.pagination').html("");
            }
        });
}
