$(function () {

    slide("", 1, "all", 10);

    $('#search-text').keyup(function () {
        var filter = $('input[name="user-filter"]:checked').val();
        var pageSize = $('#show-entries').val();
        slide(this.value, 1, filter, pageSize);
    });

    $('.custom-checkbox').click(function () {
        var filter = $('input[name="user-filter"]:checked').val();
        var pageSize = $('#show-entries').val();
        slide($('#search-text').val(), 1, filter, pageSize);
    });

    $('#show-entries').on('change', function () {
        var filter = $('input[name="user-filter"]:checked').val();
        var keyword = $('#search-text').val();
        slide(keyword, 1, filter, this.value);
    });

    $('#search-text-mobile').keyup(function () {
        var filter = $('input[name="user-filter"]:checked').val();
        var pageSize = $('#show-entries-mobile').val();
        slide(this.value, 1, filter, pageSize);
    });

    $('#show-entries-mobile').on('change', function () {
        var filter = $('input[name="user-filter"]:checked').val();
        var keyword = $('#search-text-mobile').val();
        slide(keyword, 1, filter, this.value);
    });
});

function slide(keyword, pageNumber, filter, pageSize) {
    var email = keyword;

    var formData = new FormData();
    formData.append("email", email);
    formData.append("page", pageNumber);
    formData.append("filter", filter);
    formData.append("pageSize", pageSize);

    jsRoutes.controllers.UsersController.searchUser().ajax(
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
                var userInfo = JSON.parse(data);
                var users = userInfo["users"];
                var page = userInfo["page"];
                var pages = userInfo["pages"];
                var superUser = userInfo["isSuperUser"];
                var usersFound = "";
                var mobileUserFound = "<tr class='row-space'></tr>";

                if (users.length > 0) {
                    for (var user = 0; user < users.length; user++) {

                        mobileUserFound += "<tr class='session-topic'><td class='session-topic' colspan='2'>" + users[user].email + "</td></tr><tr><td style='padding: 7px;'>";

                        if (superUser) {
                            usersFound += "<tr><td align='center'>" +
                                          "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default'>" +
                                          "<em class='fa fa-pencil'></em>" +
                                          "</a>";
                            if (users[user].admin && users[user].superUser) {
                                usersFound += "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled custom-margin-left'>" +
                                              "<em class='fa fa-trash'></em>" +
                                              "</a>" +
                                              "</td>";
                            } else {
                                usersFound += "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete custom-margin-left'>" +
                                              "<em class='fa fa-trash'></em>" +
                                              "</a>" +
                                              "</td>"
                            }
                        } else {
                            if (users[user].admin && !users[user].superUser) {
                                usersFound += "<td align='center'>" +
                                              "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default'>" +
                                              "<em class='fa fa-pencil'></em>" +
                                              "</a>" +
                                              "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled custom-margin-left'>" +
                                              "<em class='fa fa-trash'></em>" +
                                              "</a>" +
                                              "</td>";
                            } else if (users[user].admin && users[user].superUser) {
                                usersFound += "<tr><td align='center'>" +
                                              "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default disabled'>" +
                                              "<em class='fa fa-pencil'></em>" +
                                              "</a> " +
                                              "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled custom-margin-left'>" +
                                              "<em class='fa fa-trash'></em>" +
                                              "</a>" +
                                              "</td>";
                            } else {
                                usersFound += "<tr><td align='center'>" +
                                              "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default'>" +
                                              "<em class='fa fa-pencil'></em>" +
                                              "</a> " +
                                              "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete custom-margin-left'>" +
                                              "<em class='fa fa-trash'></em>" +
                                              "</a>" +
                                              "</td>"
                            }
                        }
                        usersFound += "<td>" + users[user].email + "</td>";
                        if (users[user].active) {
                            usersFound += "<td class='active-status' style='white-space: nowrap;'><span class='label label-success'>Active</span></td>"
                            mobileUserFound +="<div><span class='label label-success' >Active</span></div>";
                        } else {
                            usersFound += "<td class='suspended' style='white-space: nowrap;'><span class='label label-danger'>Suspended</span></td>"
                            mobileUserFound +="<div><span class='label label-danger' >Suspended</span></div>";
                        }
                        if (users[user].ban) {
                            usersFound += "<td class='active-status' style='white-space: nowrap;'><span class='label label-danger'>Banned</span><p class='ban-text'>" + users[user].banTill + "</p></td>";
                            mobileUserFound += "<div><span class='label label-danger' >Banned</span></div>"
                        } else {
                            usersFound += "<td class='suspended' style='white-space: nowrap;'><span class='label label-info'>Allowed</span></td>";
                            mobileUserFound += "<div><span class='label label-info' >Allowed</span></div>"
                        }

                        mobileUserFound +="</td><td>";
                        if (users[user].superUser && users[user].admin) {
                            usersFound += "<td class='active-status' style='white-space: nowrap;'><span class='label label-superUser'>SuperUser</span>";
                            mobileUserFound += "<div><span class='label label-superUser' >SuperUser</span></div>";
                        } else if (users[user].admin && !users[user].superUser) {
                            usersFound += "<td class='active-status' style='white-space: nowrap;'><span class='label label-warning'>Admin</span>";
                            mobileUserFound += "<div><span class='label label-warning' >Admin</span></div>";
                        } else {
                            usersFound += "<td class='active-status' style='white-space: nowrap;'><span class='label label-normalUser'>Normal User</span>";
                            mobileUserFound += "<div><span class='label label-normalUser' >Normal User</span></div>";
                        }
                        if (users[user].coreMember) {
                            usersFound += "<span class='label label-info meetup-session coreMember'>Core</span></td></tr>";
                            mobileUserFound += "<div><span class='label label-info meetup-session coreMember' >Core</span></div></td></tr>";
                        } else {
                            usersFound += "</td></tr>";
                            mobileUserFound += "</td></tr>"
                        }

                        mobileUserFound += "<tr><td class='table-buttons' colspan='2'>";

                        if (superUser) {
                            mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default manage-btn'>" +
                                               "<em class='fa fa-pencil'></em>" +
                                               "</a>  ";
                            if (users[user].admin && users[user].superUser) {
                                mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled manage-btn '>" +
                                                   "<em class='fa fa-trash'></em>" +
                                                   "</a>" +
                                                   "</td>";
                            } else {
                                mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete manage-btn'>" +
                                                   "<em class='fa fa-trash'></em>" +
                                                   "</a>" +
                                                   "</td>"
                            }
                        } else {
                            if (users[user].admin && !users[user].superUser) {
                                mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default manage-btn'>" +
                                                   "<em class='fa fa-pencil'></em>" +
                                                   "</a> " +
                                                   "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled manage-btn'>" +
                                                   "<em class='fa fa-trash'></em>" +
                                                   "</a>" +
                                                   "</td>";
                            } else if (users[user].admin && users[user].superUser) {
                                mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default disabled manage-btn'>" +
                                                   "<em class='fa fa-pencil'></em>" +
                                                   "</a> " +
                                                   "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete disabled manage-btn'>" +
                                                   "<em class='fa fa-trash'></em>" +
                                                   "</a>" +
                                                   "</td>";
                            } else {
                                mobileUserFound += "<a href='" + jsRoutes.controllers.UsersController.getByEmail(users[user].email)['url'] + "' class='btn btn-default manage-btn'>" +
                                                   "<em class='fa fa-pencil'></em>" +
                                                   "</a> " +
                                                   "<a href='" + jsRoutes.controllers.UsersController.deleteUser(users[user].email)['url'] + "' class='btn btn-danger delete manage-btn'>" +
                                                   "<em class='fa fa-trash'></em>" +
                                                   "</a>" +
                                                   "</td>"
                            }
                        }

                        mobileUserFound += "</tr><tr class='row-space'></tr>";
                    }

                    $('#user-found').html(usersFound);
                    $('#manage-user-tbody-mobile').html(mobileUserFound);

                    var totalUsers = users.length;
                    var startingRange = (pageSize * (page - 1)) + 1;
                    var endRange = (pageSize * (page - 1)) + users.length;

                    $('#starting-range').html(startingRange);
                    $('#ending-range').html(endRange);
                    $('#total-range').html(totalUsers);

                    paginate(page, pages);

                    var paginationLinks = document.querySelectorAll('.paginate');

                    for (var i = 0; i < paginationLinks.length; i++) {
                        paginationLinks[i].addEventListener('click', function (event) {
                            var keyword = document.getElementById('search-text').value;
                            var filter = $('input[name="user-filter"]:checked').val();
                            slide(keyword, this.id, filter, pageSize);
                        });
                    }
                } else {
                    $('#user-found').html(
                        "<tr><td align='center' class='col-md-12' colspan='5'><i class='fa fa-database' aria-hidden='true'></i><span class='no-record-found'>Oops! No Record Found</span></td></tr>"
                    );
                    $('#manage-user-tbody-mobile').html(
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
                    "<tr><td align='center'></td><td align='center'>" + er.responseText + "</td><td align='center'></td><td align='center'></td></tr>"
                );

                $('#manage-user-tbody-mobile').html(
                    "<tr><td align='center'>" + er.responseText + "</td></tr>"
                );
                $('.pagination').html("");

            }
        });
}
