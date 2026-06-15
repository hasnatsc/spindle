$(document).ready(function () {

    $('.hsSelectTwo').each(function () {
        $(this).select2({dropdownParent: $(this).parent(), placeholder: "Select one..", allowClear: true});
    });

    $('.hsPickDate').datepicker({
        format: 'dd-mm-yyyy',
        autoclose: true,
        todayHighlight: true,
        todayBtn: "linked",
        tooltip: false,
    }).on('changeDate', function (e) {
        $(this).datepicker('hide');
        $(this).parsley().validate();
    });

    $('.hsPickDateTime').datetimepicker({
        format: 'dd-mm-yyyy hh:mm',
        showTodayButton: true,
        showClear: true,
        showClose: true,
        sideBySide: true,
        icons: {
            time: 'fa fa-clock',
            date: 'fa fa-calendar',
            up: 'fa fa-chevron-up',
            down: 'fa fa-chevron-down',
            previous: 'fa fa-chevron-left',
            next: 'fa fa-chevron-right',
            today: 'fa fa-crosshairs',
            clear: 'fa fa-trash',
            close: 'fa fa-check'
        }
    });

});

function objectifyForm(form) {
    const formData = {};
    new FormData(form).forEach((value, key) => {
        if (key.includes(".")) {
            const parts = key.split(".");
            let obj = formData;
            for (let i = 0; i < parts.length - 1; i++) {
                obj[parts[i]] = obj[parts[i]] || {};
                obj = obj[parts[i]];
            }
            obj[parts[parts.length - 1]] = value || "";
        } else {
            formData[key] = value || "";
        }
    });
    return formData;
}

function initDatefield(refClass) {
    $(refClass).datepicker({
        format: 'dd-mm-yyyy',
        autoclose: true,
        todayHighlight: true,
        todayBtn: "linked",
        tooltip: false,
    }).on('changeDate', function (e) {
        $(this).datepicker('hide');
        $(this).parsley().validate();
    });
}

function hsServerError(XMLHttpRequest, textStatus, errorThrown) {

    if (XMLHttpRequest.status === 401) {

    } else {
        var msg = '';
        if (XMLHttpRequest.status === 0) {
            msg = 'Not connect.\n Verify Network.';
        } else if (XMLHttpRequest.status == 404) {
            msg = 'Requested page not found. [404]';
        } else if (XMLHttpRequest.status == 500) {
            console.log(XMLHttpRequest)
            console.log(textStatus)
            console.log(errorThrown)
            msg = 'Internal Server Error [500].';
        } else if (textStatus === 'parsererror') {
            msg = 'Requested JSON parse failed.';
        } else if (textStatus === 'timeout') {
            msg = 'Time out error.';
        } else if (textStatus === 'abort') {
            msg = 'Ajax request aborted.';
        } else {
            msg = JSON.parse(XMLHttpRequest.responseText).error;

        }
        $.gritter.add({
            title: "Server Error",
            text: msg,
            fade: false,
            // image: '../assets/img/user/user-2.jpg',
            sticky: true,
            time: '',
            class_name: "my-sticky-class growl-danger",
            before_open: function () {
            },
            after_open: function (e) {
            },
            before_close: function (manual_close) {
            },
            after_close: function (manual_close) {
            }
        });
    }
}

function hsNotification(hsData) {
    var className = hsData.isError == true ? 'my-sticky-class growl-danger' : 'growl-success';
    className = 'growl-success';
    $.gritter.add({
        title: hsData.message,
        text: "",
        fade: false,
        // image: '../assets/img/user/user-2.jpg',
        sticky: false,
        time: 5000,
        class_name: className,
        before_open: function () {
        },
        after_open: function (e) {
        },
        before_close: function (manual_close) {
        },
        after_close: function (manual_close) {
        }
    });
}


function hsAfterDelete(hsData, rowRef) {
    hsNotification(hsData);
    if (hsData.isError == false) {
        $(rowRef).parents('tr').remove();
    }
}

function hsNotificationRemove() {
    $.gritter.removeAll({
        before_close: function (e) {
        },
        after_close: function () {
        }
    });
}

function hsAfterSave(hsData, listTable) {
    hsNotification(hsData);
    if (hsData.isError == false) {
        listTable.DataTable().ajax.reload(null, false);
    }
}

function deleteSweetAlert(titleName, textMessage, iconName, buttonType, buttonText) {
    return swal({
        title: titleName,
        text: textMessage,
        icon: iconName,
        buttons: {
            cancel: {
                text: 'Cancel',
                value: null,
                visible: true,
                className: 'btn btn-default',
                closeModal: true,
            },
            confirm: {
                text: buttonText,
                value: true,
                visible: true,
                className: buttonType,
                closeModal: true
            }
        }
    }).then((confirmed) => {
        return !!confirmed;
    });
}

function saveSweetAlert(saveData, stayTime) {
    swal({
        icon: (saveData.isError == true ? 'error' : 'success'),
        text: saveData.message,
        showConfirmButton: !1,
        timer: stayTime,
        buttonsStyling: !1
    });
}

function beforeFormSubmit(button) {
    // button.children().removeClass('fa-spin');
    // button.contents().first().replaceWith(" Save... ");
    // button.prop('disabled', false);
}

function afterFormSubmit(button) {
    // button.children().addClass('fa-spin');
    // button.contents().first().replaceWith(" Processing... ");
    // button.prop('disabled', true);
}

function ajaxRequest(actions, parameters, type = 'POST') {
    var defaultObject
    jQuery.ajax({
        type: 'POST',
        dataType: 'JSON',
        async: false,
        data: parameters,
        url: actions,
        success: function (data, textStatus) {
            defaultObject = data;
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            hsServerError(XMLHttpRequest, textStatus, errorThrown);
        }
    });
    return defaultObject;
}


function selectTwoAjaxInitCall(select2FieldsId, actionUrl, placeholder, ajaxSearchParams = true, modalId, othersSearchParams) {
    $("#" + select2FieldsId).select2({
        dropdownParent: ($("#" + modalId).length ? $("#" + modalId) : null),
        ajax: {
            url: actionUrl,
            dataType: 'JSON',
            delay: 250,
            data: function (params) {
                return {
                    q: params.term,
                    page: params.page,
                    ajaxSearchParams: ajaxSearchParams,
                    othersSearchParams: othersSearchParams ? othersSearchParams.val() : ""
                };
            },
            processResults: function (data, params) {
                params.page = params.page || 1;
                return {
                    results: data.items,
                    pagination: {
                        more: (params.page * 50) < data.total_count
                    }
                };
            },
            cache: true
        },
        placeholder: placeholder,
        allowClear: true,
        width: '100%',
        escapeMarkup: function (markup) {
            return markup;
        },
        minimumInputLength: 0,
        templateResult: function (repo) {
            if (repo.loading) {
                return repo.text;
            }
            return '<div class="widget-todolist-item"> <div class="widget-todolist-content"><h6 className="mb-2px">' + nullCheck(repo.code) + ' - ' + nullCheck(repo.caption) + '</h6><div class="text-gray-600 fw-bold fs-11px">' + nullCheck(repo.details) + '</div></div></div>';
        },
        templateSelection: function (repo) {
            if (repo.id === '') {
                return placeholder;
            } else if (repo.code) {
                return repo.code + ' - ' + repo.caption
            } else {
                return repo.text;
            }
        }
    });
}

function nullCheck(checkvalue) {
    checkvalue = (checkvalue == null || checkvalue == 'null' || checkvalue == undefined) ? '' : checkvalue
    return checkvalue;
}

function isItNull(check_value) {
    return (check_value == null || check_value == undefined || check_value == "null" || check_value == "") ? true : false;
}

function editSelect2Ajax(name, value, caption) {
    $("#" + name).empty();
    $("#" + name).append('<option value="' + value + '" selected>' + caption + '</option>').trigger('change');
}

function hs_add_table_data(fields, properties, prefix, detailsName, tableId, footerEnable = false) {
    $("#" + tableId + " > tbody").html("");
    var rowNumber = 1;
    var hsSplitData;
    $.each(properties, function (key, value) {
        var remainingFields = "";
        var tableRow = "<tr><td>" + (++key) + "</td>";
        value["sort_order"] = rowNumber;
        $.each(fields, function (keys, values) {
            hsSplitData = values.split("__");

            if ((hsSplitData[1]).includes("multiply")) {
                var hsSplitMultiply = (hsSplitData[1]).split("*");
                value[hsSplitData[0]] = (hsFloatConverter(value[hsSplitMultiply[1]]) * hsFloatConverter(value[hsSplitMultiply[2]]));
            }
            remainingFields += "<input type=\"hidden\" class=\"" + keys + "\" name =\"" + detailsName + "[" + rowNumber + "]." + keys + "\" value=\"" + nullCheck(value[hsSplitData[0]]) + "\"/>";
            if ((hsSplitData[1]).includes("table")) {
                tableRow += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
            }
        });
        tableRow += "<td  ref_id=\"" + rowNumber + "\">" + remainingFields + "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "EditEvent(this)\" ref_id=\"" + rowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-pen-to-square\"></i></a><a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "DeleteEvent(this)\" ref_id=\"" + rowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-danger fa-trash-can\"></i></a></td></tr>";
        rowNumber++;
        $("#" + tableId + " > tbody").prepend(tableRow);
    });
    footerEnable ? hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId) : null
}

function hs_divide_table_data(fields, properties, prefix, detailsName, tableId, footerEnable = false) {
    $("#" + tableId + " > tbody").html("");
    var rowNumber = 1;
    var hsSplitData;
    $.each(properties, function (key, value) {
        var remainingFields = "";
        var tableRow = "<tr><td>" + (++key) + "</td>";
        value["sort_order"] = rowNumber;
        $.each(fields, function (keys, values) {
            hsSplitData = values.split("__");
            if ((hsSplitData[1]).includes("multiply")) {
                var hsSplitMultiply = (hsSplitData[1]).split("*");
                value[hsSplitData[0]] = (hsFloatConverter(value[hsSplitMultiply[1]]) * hsFloatConverter(value[hsSplitMultiply[2]]));
            }
            remainingFields += "<input type=\"hidden\" class=\"" + keys + "\" name =\"" + detailsName + "[" + rowNumber + "]." + keys + "\" value=\"" + nullCheck(value[hsSplitData[0]]) + "\"/>";
            if ((hsSplitData[1]).includes("table")) {
                tableRow += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
            }
        });
        tableRow += "<td  ref_id=\"" + rowNumber + "\">" + remainingFields + "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "DividedEvent(this)\" ref_id=\"" + rowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-solid fa-divide\"></i></a></td></tr>";
        rowNumber++;
        $("#" + tableId + " > tbody").prepend(tableRow);
    });
    footerEnable ? hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId) : null
}


function hs_add_table_dataFooter(fields, properties, prefix, detailsName, tableId) {
    const objectMap = {};
    $.each(fields, function (key, value) {
        $.each(properties, function (keys, values) {
            if (value.includes("table")) {
                if (value.includes("sumFooter")) {
                    objectMap[key] = hsFloatConverter(objectMap[key]) + hsFloatConverter(values[key]);
                } else {
                    objectMap[key] = '-';
                }
            }
        });
    });
    var tableRows = "<tr><td>SUM</td>";
    $.each(objectMap, function (key, value) {
        tableRows += "<td>" + value + "</td>";
    });
    tableRows += "<td>-</td></tr>";
    $("#" + tableId + " > tbody").append(tableRows);
}


function hs_add_table_inner_data(fields, properties, prefix, detailsName, tableId, extraAction) {
    $("#" + tableId + " > tbody").html("");
    var hsSplitData;
    var hsTableRowNumber = 1;
    $.each(properties, function (key, value) {
        var remainingFields = "";
        var tableRow = "<tr><td>" + (++key) + "</td>";
        value["sort_order"] = hsTableRowNumber;
        $.each(fields, function (keys, values) {

            if (keys == "dtlLine") {
                tableRow += hsInnerTablesHtml(prefix, hsTableRowNumber, keys, value, values, detailsName)
            } else {
                hsSplitData = values.split("__");
                if ((hsSplitData[1]).includes("multiply")) {
                    var hsSplitMultiply = (hsSplitData[1]).split("*");
                    value[hsSplitData[0]] = (hsFloatConverter(value[hsSplitMultiply[1]]) * hsFloatConverter(value[hsSplitMultiply[2]]));
                }
                remainingFields += "<input type=\"hidden\" class=\"" + keys + "\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + keys + "\" value=\"" + nullCheck(value[hsSplitData[0]]) + "\"/>";
                if ((hsSplitData[1]).includes("table")) {
                    tableRow += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
                }
            }
        });
        tableRow += "<td  ref_id=\"" + hsTableRowNumber + "\">" + remainingFields + "" +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "AddEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-square-plus text-success\"></i></a>" +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "EditEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-pen-to-square\"></i></a>" +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "DeleteEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-danger fa-trash-can\"></i></a>"
        $.each(extraAction, function (key, value) {
            tableRow += ((value) ? "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "ExtraEvent" + key + "(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-success " + value + "\"></i></a>" : "");
        });
        tableRow += "</td></tr>";
        hsTableRowNumber = hsTableRowNumber + 1;
        $("#" + tableId + " > tbody").prepend(tableRow);
    });
}

function hsInnerTablesHtml(prefix, hsTableRowNumber, keys, value, values, detailsName) {
    var tableRowInner = "<td><table class='table table-bordered table-striped table-hover' materTableRef='" + hsTableRowNumber + "'>";
    tableRowInner += "<thead><tr>";
    $.each(values, function (keyInn, valueInn) {
        if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
            tableRowInner += ("<td>" + (valueInn.split(":::")[1]) + "</td>");
        }
    });
    tableRowInner += "<td>Action</td></tr></thead>";
    var hsTableRowNumberLine = 1;
    $.each(value[keys], function (keyInnVal, valueInnVal) {
        tableRowInner += "<tr>";
        var remainingLineFields = ""
        valueInnVal["sort_order"] = hsTableRowNumberLine;
        $.each(values, function (keyInn, valueInn) {
            remainingLineFields += "<input type=\"hidden\" class=\"" + keyInn + "\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + keys + "[" + hsTableRowNumberLine + "]." + keyInn + "\" value=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\"/>";
            if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                tableRowInner += ("<td>" + (valueInnVal[valueInn.split("__")[0]]) + "</td>");
            }
        });
        tableRowInner += "<td ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\">" + remainingLineFields +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "_" + keys + "EditEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-pen-to-square\"></i></a>" +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "_" + keys + "DeleteEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-danger fa-trash-can\"></i></a>" +
            "</td></tr>";
        remainingLineFields = "";
        hsTableRowNumberLine++;
    });
    tableRowInner += "</table></td>";
    return tableRowInner;
}

function hs_add_table_inner_data_new(fields, properties, prefix, detailsName, tableId, extraAction, detailCreateFalse, detailCloneTrue) {
    $("#" + tableId + " > tbody").html("");
    var hsSplitData;
    var hsTableRowNumber = 1;
    $.each(properties, function (key, value) {
        var remainingFields = "<input type=\"hidden\" class=\"id\" name =\"" + detailsName + "[" + hsTableRowNumber + "].id\" value=\"" + nullCheck(value['id']) + "\"/><input type=\"hidden\" class=\"sortOrder\" name =\"" + detailsName + "[" + hsTableRowNumber + "].sortOrder\" value=\"" + nullCheck(hsTableRowNumber) + "\"/>";
        var tableRow = "<tr><td>" + (++key) + "</td>";
        value["sort_order"] = hsTableRowNumber;
        $.each(fields, function (keys, values) {
            if (keys == "dtlLine") {
                tableRow += hsInnerTablesHtmlNew(hsTableRowNumber, prefix, detailsName, keys, value.dtlLine, values)
            } else {
                hsSplitData = values.split("__");
                if ((hsSplitData[1]).includes("multiply")) {
                    var hsSplitMultiply = (hsSplitData[1]).split("*");
                    value[hsSplitData[0]] = (hsFloatConverter(value[hsSplitMultiply[1]]) * hsFloatConverter(value[hsSplitMultiply[2]]));
                }
                remainingFields += "<input type=\"hidden\" class=\"" + keys + "\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + keys + "\" value=\"" + nullCheck(value[hsSplitData[0]]) + "\"/>";
                if ((hsSplitData[1]).includes("table")) {
                    if ((hsSplitData[1]).includes("function")) {
                        tableRow += "<td>(" + keys + "_" + ((hsSplitData[1]).split("__")[1]) + ")</td>";
                    } else {
                        tableRow += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
                    }
                }
            }
        });
        tableRow += "<td  ref_id=\"" + hsTableRowNumber + "\">" + remainingFields + "" +
            ((!detailCreateFalse && !isItNull(fields.dtlLine)) ? ("<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "AddEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-square-plus text-success\"></i></a>") : "") +
            (!detailCreateFalse ? ("<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "EditEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-pen-to-square\"></i></a>") : "") +
            (detailCloneTrue ? ("<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "CloneEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-clone\"></i></a>") : "") +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "DeleteEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-danger fa-trash-can\"></i></a>"
        $.each(extraAction, function (key, value) {
            tableRow += ((value) ? "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "ExtraEvent" + key + "(this)\" ref_id=\"" + hsTableRowNumber + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-success " + value + "\"></i></a>" : "");
        });
        tableRow += "</td></tr>";
        hsTableRowNumber = hsTableRowNumber + 1;
        $("#" + tableId + " > tbody").prepend(tableRow);
    });
}

function hsInnerTablesHtmlNew(hsTableRowNumber, prefix, detailsName, detailsNameLine, values, properties) {
    var tableRowInner = "<td><table class='table table-bordered table-striped table-hover' materTableRef='" + hsTableRowNumber + "'>";
    tableRowInner += "<thead><tr>";
    var fileRow = "";
    $.each(properties, function (keyInn, valueInn) {
        if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
            tableRowInner += ("<td>" + nullCheck((valueInn.split(":::")[1])) + "</td>");
            if (valueInn.includes("fileUploadShow")) {
                if (valueInn.includes("fileUploadInput")) {
                    fileRow = nullCheck(valueInn.split(":::")[0]);
                }
            }
        }
    });
    tableRowInner += "<td>Action</td></tr></thead>";
    var hsTableRowNumberLine = 1;
    $.each(values, function (keyInnVal, valueInnVal) {
        tableRowInner += "<tr>";
        var remainingLineFields = "<input type=\"hidden\" class=\"id\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + detailsNameLine + "[" + hsTableRowNumberLine + "].id\" value=\"" + nullCheck(valueInnVal['id']) + "\"/><input type=\"hidden\" class=\"sortOrder\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + detailsNameLine + "[" + hsTableRowNumberLine + "].sortOrder\" value=\"" + nullCheck(hsTableRowNumberLine) + "\"/>";
        valueInnVal["sort_order"] = hsTableRowNumberLine;
        $.each(properties, function (keyInn, valueInn) {
            remainingLineFields += "<input type=\"hidden\" class=\"" + keyInn + "\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + detailsNameLine + "[" + hsTableRowNumberLine + "]." + keyInn + "\" value=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\"/>";
            if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                if (valueInn.includes("fileUploadShow")) {
                    if (valueInn.includes("fileUploadInput")) {
                        tableRowInner += (isItNull(fileRow) ? "" : "<td><input style='width: 100px;' type=\"file\" class=\"" + fileRow + "\" name =\"" + detailsName + "[" + hsTableRowNumber + "]." + detailsNameLine + "[" + hsTableRowNumberLine + "]." + keyInn + "\"/> &nbsp; &nbsp;" + ((isItNull(valueInnVal[valueInn.split("__")[0]])) ? "" : ("<a href=\"javascript:;\" onclick=\"" + keyInn + "FileDownloadEvent(" + nullCheck(valueInnVal["id"]) + ")\"><i class=\"fa fa-cloud-download text-danger\"></i></a>")) + "</td>");
                    } else {
                        tableRowInner += "<td><a href=\"javascript:;\" onclick=\"" + keyInn + "FileDownloadEvent(" + nullCheck(valueInnVal["id"]) + ")\"><i class=\"fa fa-cloud-download text-danger\"></i></a></td>"
                    }
                } else {
                    tableRowInner += ("<td>" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "</td>");
                }
            }
        });
        tableRowInner += "<td ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\">" + remainingLineFields +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "_" + detailsNameLine + "EditEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fa-pen-to-square\"></i></a>" +
            "<a href=\"javascript:;\" onclick=\"hs_" + prefix + "_" + detailsName + "_" + detailsNameLine + "DeleteEvent(this)\" ref_id=\"" + hsTableRowNumber + "\" ref_line_id=\"" + hsTableRowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-danger fa-trash-can\"></i></a>" +
            "</td></tr>";
        remainingLineFields = "";
        hsTableRowNumberLine++;
    });
    tableRowInner += "</table></td>";
    return tableRowInner;
}

function hs_view_table_data(fields, dataObj, prefix, detailsName, tableId, extraAction, footerEnable = false) {
    $("#" + tableId + " > tbody").html("");
    var rowNumber = 1;
    var hsSplitData;
    var tableRow = "";
    var tableRowBody = "";
    $.each(dataObj, function (key, value) {
        value["sort_order"] = rowNumber;
        var hsAttributes = {};
        var hsAttributesHtml = "";
        $.each(fields, function (keys, values) {
            if (typeof values == "object") {
                var tableRowInner = "<table class='table table-bordered table-striped table-hover'>";
                tableRowInner += "<thead><tr>";
                $.each(values, function (keyInn, valueInn) {
                    if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                        tableRowInner += ("<td>" + nullCheck(valueInn.split(":::")[1]) + "</td>");
                    }
                });
                tableRowInner += "<td>Action</td></tr></thead>";

                $.each(value[keys], function (keyInnVal, valueInnVal) {
                    var rowNumberLine = 1;
                    tableRowInner += "<tr>";
                    var remainingLineFields = ""
                    var hsAttributesHtmlLine = "";
                    $.each(values, function (keyInn, valueInn) {
                        remainingLineFields += "<input type=\"hidden\" class=\"" + keys + "\"  value=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\"/>";
                        hsAttributesHtmlLine += (keyInn + "=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\" ");
                        if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                            tableRowInner += ("<td>" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "</td>");
                        }
                    });
                    tableRowInner += "<td ref_id=\"" + tableRow + "\">" + remainingLineFields +
                        "<a href=\"javascript:;\" " + hsAttributesHtmlLine + " onclick=\"hs_" + prefix + "_" + detailsName + "_" + keys + "ShowEvent(this)\" ref_id=\"" + rowNumber + "\" ref_line_id=\"" + rowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fas fa-book-open-reader text-success\"></i></a>" +
                        "</td></tr>";
                    remainingLineFields = "";
                    rowNumberLine++;
                });
                tableRowInner += "</table>";
                tableRowBody += "<td>" + tableRowInner + "</td>";
            } else {
                hsSplitData = values.split("__");
                if ((hsSplitData[1]).includes("table")) {
                    tableRowBody += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
                }
                hsAttributes[keys] = nullCheck(value[hsSplitData[0]]);
                hsAttributesHtml += (keys + "=\"" + nullCheck(value[hsSplitData[0]]) + "\" ");
            }
            rowNumber++;
        });
        tableRow = "<tr><td>" + (++key) + "</td>" + tableRowBody +
            // tableRow = "<tr><td>" + (++key) + "</td>" + tableRowBody + "<td>" +
            // "<a " + hsAttributesHtml + " href=\"javascript:;\" onclick=\"hs_" + tableId + "Event(this)\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fas fa-book-open-reader text-success\"></i></a>"
            $.each(extraAction, function (key, values) {
                tableRow += ((values) ? "<a href=\"javascript:;\" onclick=\"hs_" + tableId + "ShowEvent" + key + "(" + value["id"] + ")\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-success " + values + "\"></i></a>" : "");
            });
        tableRow += "</tr>";
        // tableRow += "</td></tr>";
        $("#" + tableId + " > tbody").append(tableRow);
        rowNumber++;
        tableRow = ""
        hsAttributesHtml = ""
        tableRowBody = ""
        footerEnable ? hs_add_table_dataFooter(fields, dataObj, prefix, detailsName, tableId) : null
    });
}

function hs_view_table_dataNew(properties, dataObj, prefix, detailsName, tableId, extraAction) {
    $("#" + tableId + " > tbody").html("");
    var rowNumber = 1;
    var rowNumberDetail = 1;
    var hsSplitData;
    var tableRow = "";
    var tableRowBody = "";
    $.each(dataObj, function (key, value) {
        value["sort_order"] = rowNumber;
        var hsAttributes = {};
        var hsAttributesHtml = "";
        $.each(properties, function (keys, values) {
            if (keys == "dtlLine") {
                var tableRowInner = "<table class='table table-bordered table-striped table-hover'>";
                tableRowInner += "<thead><tr>";
                $.each(values, function (keyInn, valueInn) {
                    if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                        tableRowInner += ("<td>" + nullCheck(valueInn.split(":::")[1]) + "</td>");
                    }
                });
                tableRowInner += "<td>Action</td></tr></thead>";
                $.each(value.dtlLine, function (keyInnVal, valueInnVal) {
                    var rowNumberLine = 1;
                    tableRowInner += "<tr>";
                    var remainingLineFields = ""
                    var hsAttributesHtmlLine = "";
                    $.each(values, function (keyInn, valueInn) {
                        remainingLineFields += "<input type=\"hidden\" class=\"" + keys + "\"  value=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\"/>";
                        hsAttributesHtmlLine += (keyInn + "=\"" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "\" ");
                        if (!(keyInn == "id" || keyInn == "sortOrder") && (valueInn.includes("table"))) {
                            if (valueInn.includes("fileUploadShow")) {
                                tableRowInner += (isItNull(valueInnVal[valueInn.split("__")[0]]) ? "<td>-</td>" : "<td><a href=\"javascript:;\" onclick=\"" + keyInn + "FileDownloadEvent(" + nullCheck(valueInnVal["id"]) + ")\"><i class=\"fa fa-cloud-download text-danger\"></i></a></td>");
                            } else if (valueInn.includes("customCaption")) {
                                tableRowInner += ("<td>" + nullCheck(valueInnVal[valueInn.split("__")[0] + "_custom_caption"]) + "</td>");
                            } else {
                                tableRowInner += ("<td>" + nullCheck(valueInnVal[valueInn.split("__")[0]]) + "</td>");
                            }
                        }
                    });
                    tableRowInner += "<td ref_id=\"" + tableRow + "\">" + remainingLineFields +
                        "<a href=\"javascript:;\" " + hsAttributesHtmlLine + " onclick=\"hs_" + prefix + "_" + detailsName + "_dtlLineViewTableEvent(this)\" ref_id=\"" + rowNumberDetail + "\" ref_line_id=\"" + rowNumberLine + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fas fa-book-open-reader text-success\"></i></a>" +
                        "</td></tr>";
                    remainingLineFields = "";
                    rowNumberLine++;
                });
                tableRowInner += "</table>";
                tableRowBody += "<td>" + tableRowInner + "</td>";
            } else {
                hsSplitData = values.split("__");
                if ((hsSplitData[1]).includes("table")) {
                    if ((hsSplitData[1]).includes("fileUploadShow")) {
                        // tableRowInner += (isItNull(valueInnVal[valueInn.split("__")[0]]) ? "<td>-</td>" : "<td><a href=\"javascript:;\" onclick=\"" + keyInn + "FileDownloadEvent(" + nullCheck(valueInnVal["id"]) + ")\"><i class=\"fa fa-cloud-download text-danger\"></i></a></td>");
                    } else if ((hsSplitData[1]).includes("customCaption")) {
                        tableRowBody += "<td>" + nullCheck(value[hsSplitData[0] + "_custom_caption"]) + "</td>";
                        ;
                    } else {
                        tableRowBody += "<td>" + nullCheck(value[hsSplitData[0]]) + "</td>";
                    }
                }
                hsAttributes[keys] = nullCheck(value[hsSplitData[0]]);
                hsAttributesHtml += (keys + "=\"" + nullCheck(value[hsSplitData[0]]) + "\" ");
            }
            rowNumber++;
        });

        tableRow = "<tr><td>" + (++key) + "</td>" + tableRowBody + "<td>" +
            "<a href=\"javascript:;\" onclick=\"hs_" + tableId + "Event(this)\" ref_id=\"" + rowNumberDetail + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular fas fa-book-open-reader text-success\"></i></a>"
        $.each(extraAction, function (key, values) {
            tableRow += ((values) ? "<a href=\"javascript:;\" onclick=\"hs_" + tableId + "ShowEvent" + key + "(" + value["id"] + ")\"  ref_id=\"" + rowNumberDetail + "\" class=\"btn btn-white btn-sm\"><i class=\"fa-regular text-success " + values + "\"></i></a>" : "")
        });
        tableRow += "</td></tr>";
        $("#" + tableId + " > tbody").append(tableRow);

        rowNumberDetail++;
        tableRow = ""
        hsAttributesHtml = ""
        tableRowBody = ""
    });
}

function hsFloatConverter(value) {
    if (value == null || value == 'null' || value == '' || value == 'NULL') {
        return parseFloat('0');
    } else {
        return parseFloat(value);
    }
}

function hsOnChangeSetSelectTwoValue(actionUrl, paramsData, referenceSelect, actionOnlyNotNull = false, actionValue = "", actionType = "POST") {
    if (actionOnlyNotNull && isItNull(actionValue)) {
        referenceSelect.select2("destroy");
        referenceSelect.empty();
        referenceSelect.select2({width: '100%'});
        return false;
    }
    jQuery.ajax({
        type: actionType,
        dataType: 'JSON',
        async: false,
        data: paramsData,
        url: actionUrl,
        success: function (data, textStatus) {
            referenceSelect.select2("destroy");
            referenceSelect.empty();
            $.each(data.obj, function (i, item) {
                referenceSelect.append($("<option></option>").attr("value", item.key).text(item.value));
            });
            referenceSelect.select2({dropdownParent: referenceSelect.parent()});
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            hsServerError(XMLHttpRequest, textStatus, errorThrown);
        }
    });
}

function dateDiffByPicker(first, second) {
    return ((isItNull(first) || isItNull(second)) ? "" : (Math.round((moment(second, "DD-MM-YYYY") - moment(first, "DD-MM-YYYY")) / (1000 * 60 * 60 * 24))));
}

function asgSetDateToPicker(currentDate) {
    var newDate = new Date();
    var dd = newDate.getDate();
    var mm = newDate.getMonth() + 1;
    var y = newDate.getFullYear();
    var dateString = ("0" + dd).slice(-2) + '-' + ("0" + mm).slice(-2) + '-' + y;
    return dateString
}

function selectTwoIdValues(selectedValue) {
    selectedValue = selectedValue.select2('data');
    if (!isItNull(selectedValue)) {
        if (Object.keys(selectedValue).length > 0) {
            if (!isItNull(selectedValue[0].id)) {
                return {id: selectedValue[0].id, value: selectedValue[0].text};
            }
        }
    }
    return {id: "", value: ""};
}


function formatCurrency(value, currency = "BDT") {
    console.log(value);
    if (value === null || value === undefined || value === "" || isNaN(value)) {
        return "0.00";
    }
    const num = parseFloat(value.toString().replace(/,/g, ''));
    console.log(num);
    return new Intl.NumberFormat('en-BD', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(num);
}

function hsAfterSaveMessages(messages, success, form, dataTables, customSuccessMessages) {
    const alertMsg = document.createElement('div');
    alertMsg.className = `alert alert-${success ? 'success' : 'danger'} alert-dismissible fade show`;
    alertMsg.innerHTML = ` ${messages} <button type="button" class="btn-close" data-bs-dismiss="alert"></button> `;
    const container = document.getElementById("successMessages") || document.getElementById(customSuccessMessages);
    container.appendChild(alertMsg);
    setTimeout(() => alertMsg.remove(), 5000);
    if (success) {
        form.reset();
        form.querySelectorAll('input[type="hidden"]').forEach(input => input.value = '');
        $(form).find("select").val("").trigger("change");
        if (typeof hsCustomForm === "function") {
            hsCustomForm();
        }
        dataTables.ajax.reload(null, false);
    }
}

function hsDisableButton(submitBtn, spinner, btnText) {
    submitBtn.disabled = true;
    spinner.classList.remove('d-none');
    btnText.textContent = 'Processing...';
}

function hsEnableButton(submitBtn, spinner, btnText) {
    submitBtn.disabled = false;
    spinner.classList.add('d-none');
    btnText.textContent = 'Save';
}

function hsDisableBtn(id) {
    const btn = document.getElementById(id);
    if (!btn) return;

    btn.disabled = true;
    btn.classList.add('disabled');
    btn.style.opacity = "0.6";
    btn.style.cursor = "not-allowed";
}

function hsEnableBtn(id) {
    const btn = document.getElementById(id);
    if (!btn) return;

    btn.disabled = false;
    btn.classList.remove('disabled');
    btn.style.opacity = "";
    btn.style.cursor = "";
}

function hsOpenModal(defaultModal) {
    let modal = new bootstrap.Modal(defaultModal);
    modal.show();
}

function hsResetForm(form) {
    form.reset();
    form.querySelectorAll('input[type="hidden"]').forEach(input => input.value = '');
    $(form).find("select").val("").trigger("change");
}

function hsInitDataTable(selector, ajaxUrl, columns) {
    return $(selector).DataTable({
        processing: true,
        responsive: true,
        serverSide: true,
        ajax: {url: ajaxUrl, type: "GET"},
        columns: columns
    });
}


function hsConfirmDelete(url, reloadTableSelector) {
    Swal.fire({
        title: 'Are you sure?',
        text: "This action cannot be undone!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Yes, delete it!'
    }).then(result => {
        if (result.isConfirmed) {
            secureFetch(url, {method: 'DELETE'})
                .then(data => {
                    Swal.fire({
                        icon: data.success ? 'success' : 'error',
                        title: data.success ? 'Deleted!' : 'Error!',
                        text: data.message
                    });
                    if (data.success) $(reloadTableSelector).DataTable().ajax.reload();
                });
        }
    });
}

function hsFetchAndShowModal(url, modalId, fillCallback) {

    // ✅ Read CSRF dynamically
    const csrfTokenMeta  = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    const csrfToken  = csrfTokenMeta?.getAttribute('content');
    const csrfHeader = csrfHeaderMeta?.getAttribute('content');

    const headers = new Headers();

    // Attach CSRF header if available
    if (csrfToken && csrfHeader) {
        headers.set(csrfHeader, csrfToken);
    }

    fetch(url, {
        method: "GET",
        headers: headers,
        credentials: "same-origin"
    })
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch data");
            return res.json();
        })
        .then(data => {
            fillCallback(data);
            new bootstrap.Modal(document.getElementById(modalId)).show();
        })
        .catch(err => {
            console.error(err);
            alert("Error loading data");
        });
}


function hsChangeStatus(url, tableSelector, entityName = 'Item') {
    Swal.fire({
        title: `Change ${entityName} Status?`,
        text: `This will toggle the ${entityName.toLowerCase()} status!`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Yes, change it!',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            secureFetch(url, {method: 'POST'})
                .then(data => {
                    if (data.success) {
                        Swal.fire({
                            icon: 'success',
                            title: 'Status Changed!',
                            text: data.message,
                            timer: 2000,
                            showConfirmButton: false
                        });
                        if (tableSelector) {
                            $(tableSelector).DataTable().ajax.reload();
                        }
                    } else {
                        Swal.fire({
                            icon: 'error',
                            title: 'Error!',
                            text: data.message
                        });
                    }
                })
                .catch(error => {
                    Swal.fire({
                        icon: 'error',
                        title: 'Oops...',
                        text: 'Something went wrong: ' + error.message
                    });
                });
        }
    });
}

function hsInitAjaxForm(
    formId,
    submitBtnId,
    spinnerId,
    btnTextId,
    saveUrl,
    reloadTableSelector = null,
    modifyDataCallback = null
) {

    const form = document.querySelector(formId);
    const submitBtn = document.querySelector(submitBtnId);
    const spinner = document.querySelector(spinnerId);
    const btnText = document.querySelector(btnTextId);

    if (!form) {
        console.warn(`Form with ID ${formId} not found.`);
        return;
    }

    $(form).parsley();

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        if (!$(form).parsley().validate()) return;

        hsDisableButton(submitBtn, spinner, btnText);

        let nestedData = objectifyForm(form);

        if (modifyDataCallback && typeof modifyDataCallback === 'function') {
            nestedData = modifyDataCallback(nestedData) || nestedData;
        }

        secureFetch(saveUrl, {
            method: "POST",
            body: JSON.stringify(nestedData)
        })
            .then(data => {
                hsAfterSaveMessages(
                    data.message,
                    data.success,
                    form,
                    reloadTableSelector ? $(reloadTableSelector).DataTable() : null
                );

            })
            .catch(error => {
                hsAfterSaveMessages(
                    error.message || "Request failed",
                    false,
                    null,
                    null
                );

            })
            .finally(() => {
                hsEnableButton(submitBtn, spinner, btnText);
            });
    });
}


function haPopulateSelect(url, selectId, optionTextBuilder) {
    secureFetch(url)
        .then(data => {
            const select = document.getElementById(selectId);
            select.innerHTML = ''; // Clear existing
            data.forEach(item => {
                const option = document.createElement('option');
                option.value = item.id;
                option.text = optionTextBuilder(item);
                select.appendChild(option);
            });
        })
        .catch(err => console.error("Error loading options:", err));
}


function hsOpenModalForm(modalId, formId) {
    const modal = new bootstrap.Modal(document.getElementById(modalId));
    modal.show();
    hsResetForm(document.getElementById(formId));
}


function hsPostAction({
                          id,
                          action,
                          url,
                          title,
                          text,
                          icon = 'question',
                          confirmButtonColor = '#3085d6',
                          confirmButtonText = 'Yes, proceed!',
                          cancelButtonText = 'Cancel',
                          successTitle = 'Success!',
                          successIcon = 'success',
                          successColor = '#28a745',
                          postDataKey = 'actionBy',
                          user = 'CurrentUser'
                      }) {
    Swal.fire({
        title: title,
        text: text,
        icon: icon,
        showCancelButton: true,
        confirmButtonColor: confirmButtonColor,
        cancelButtonColor: '#d33',
        confirmButtonText: confirmButtonText,
        cancelButtonText: cancelButtonText
    }).then((result) => {
        if (result.isConfirmed) {
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
            fetch(`${url}/${id}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
                },
                body: `${postDataKey}=${encodeURIComponent(user)}`
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        Swal.fire({
                            icon: successIcon,
                            title: successTitle,
                            text: data.message,
                            timer: 2000,
                            showConfirmButton: false
                        });
                        $(dataTable).DataTable().ajax.reload();
                    } else {
                        Swal.fire({
                            icon: 'error',
                            title: 'Error!',
                            text: data.message
                        });
                    }
                })
                .catch(error => {
                    console.error(error);
                    Swal.fire({
                        icon: 'error',
                        title: 'Error!',
                        text: 'Something went wrong.'
                    });
                });
        }
    });
}

function importData(label, endpoint, dataTable = null, payload = null) {

    const requestData = payload || { url: label.toLowerCase() };

    Swal.fire({
        title: `Importing ${label}...`,
        text: `Please wait while ${label.toLowerCase()} data is being imported.`,
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    secureFetch(endpoint, {
        method: "POST",
        body: JSON.stringify(requestData)
    })
        .then(data => {

            Swal.close();

            if (data.success) {
                Swal.fire({
                    icon: 'success',
                    title: `${label} Import Completed`,
                    html: `
                    <p>${data.message || ''}</p>
                    <p><b>Successful:</b> ${data.successCount || 0}</p>
                    <p><b>Failed:</b> ${data.failedCount || 0}</p>
                `,
                    timer: 5000
                });

                if (dataTable) {
                    $(dataTable).DataTable().ajax.reload();
                }

            } else {
                Swal.fire({
                    icon: 'error',
                    title: `${label} Import Failed`,
                    text: data.message || 'Unknown error occurred.'
                });
            }
        })
        .catch(error => {
            Swal.close();
            Swal.fire({
                icon: 'error',
                title: `${label} Import Error`,
                text: `Failed to import ${label.toLowerCase()}: ${error.message}`
            });
        });
}

window.secureFetch = async function (url, options = {}) {

    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});

    // Always read CSRF dynamically
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    const csrfToken = csrfTokenMeta?.getAttribute('content');
    const csrfHeader = csrfHeaderMeta?.getAttribute('content');

    // ✅ FIXED: Do NOT set JSON if body is FormData
    if (
        options.body &&
        !(options.body instanceof FormData) &&
        !headers.has("Content-Type")
    ) {
        headers.set("Content-Type", "application/json");
    }

    // Attach CSRF for non-GET requests
    if (method !== "GET" && csrfToken && csrfHeader) {
        headers.set(csrfHeader, csrfToken);
    }

    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        method,
        headers
    });

    if (response.status === 204) {
        return {success: true, message: "Operation completed successfully"};
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Request failed");
        return data;
    }

    const text = await response.text();
    if (!response.ok) throw new Error(text || "Request failed");

    return {success: true, message: text};
};

/**
 * Centralized confirmation + secureFetch + feedback handler
 */
window.confirmAndExecute = function ({
                                         title = "Are you sure?",
                                         text = "You won't be able to revert this!",
                                         icon = "warning",
                                         confirmText = "Yes, proceed!",
                                         cancelText = "Cancel",
                                         confirmColor = "#3085d6",
                                         cancelColor = "#d33",
                                         url,
                                         method = "POST",
                                         body = null,
                                         successTitle = "Success!",
                                         successMessage = null,
                                         reloadTable = false,
                                         dataTable = null
                                     }) {
    Swal.fire({
        title,
        text,
        icon,
        showCancelButton: true,
        confirmButtonColor: confirmColor,
        cancelButtonColor: cancelColor,
        confirmButtonText: confirmText,
        cancelButtonText: cancelText
    }).then(result => {

        if (!result.isConfirmed) return;

        secureFetch(url, {
            method,
            body: body ? JSON.stringify(body) : null
        })
            .then(data => {

                if (!data.success) {
                    throw new Error(data.message || "Operation failed");
                }

                Swal.fire({
                    icon: "success",
                    title: successTitle,
                    text: successMessage || data.message,
                    timer: 2000,
                    showConfirmButton: false
                });

                if (reloadTable && dataTable) {
                    $(dataTable).DataTable().ajax.reload();
                }
            })
            .catch(error => {
                Swal.fire({
                    icon: "error",
                    title: "Error!",
                    text: error.message
                });
            });
    });
};

window.submitWithSecureFetch = function ({
                                             url,
                                             method = "POST",
                                             body,
                                             submitBtn,
                                             spinner,
                                             btnText,
                                             loadingText = "Processing...",
                                             successTitle = "Success!",
                                             successMessage = null,
                                             onSuccess = null,
                                             onFinally = null
                                         }) {
    // Disable button
    if (submitBtn) submitBtn.disabled = true;
    if (spinner) spinner.classList.remove('d-none');
    if (btnText) btnText.textContent = loadingText;
    secureFetch(url, {
        method,
        body: JSON.stringify(body)
    })
        .then(data => {

            if (!data.success) {
                throw new Error(data.message || "Operation failed");
            }

            Swal.fire({
                icon: "success",
                title: successTitle,
                text: successMessage || data.message,
                timer: 2000,
                showConfirmButton: false
            });

            if (typeof onSuccess === "function") {
                onSuccess(data);
            }
        })
        .catch(error => {
            Swal.fire({
                icon: "error",
                title: "Error!",
                text: error.message
            });
        })
        .finally(() => {
            if (submitBtn) submitBtn.disabled = false;
            if (spinner) spinner.classList.add('d-none');
            if (btnText) btnText.textContent = "Change Password";

            if (typeof onFinally === "function") {
                onFinally();
            }
        });
};


function actionWithRemarks({
                               title,
                               confirmText,
                               confirmColor,
                               url,
                               successMessage = 'Action completed successfully'
                           }) {
    Swal.fire({
        title,
        html: `
            <div class="mb-3">
                <label class="form-label text-start d-block">Remarks</label>
                <textarea id="actionRemarks"
                          class="form-control"
                          rows="3"
                          placeholder="Enter remarks (optional)"></textarea>
            </div>
        `,
        showCancelButton: true,
        confirmButtonColor: confirmColor,
        confirmButtonText: confirmText,
        preConfirm: () => ({
            remarks: document.getElementById('actionRemarks').value || ''
        })
    }).then(result => {
        if (!result.isConfirmed) return;

        const payload = result.value;

        const headers = {
            'Content-Type': 'application/json'
        };

        // 🔒 CSRF
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(url, {
            method: 'POST',
            headers,
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    Swal.fire('Success!', data.message || successMessage, 'success');
                    dataTables.ajax.reload(null, false);
                } else {
                    Swal.fire('Error!', data.message || 'Action failed', 'error');
                }
            })
            .catch(() => {
                Swal.fire('Error!', 'Server communication failed', 'error');
            });
    });
}

function approvalActionDialog({
                                    title,
                                    icon,
                                    confirmText,
                                    confirmColor,
                                    textareaLabel,
                                    textareaPlaceholder,
                                    textareaRequired = false,
                                    endpoint,
                                    successTitle,
                                    tableToReload
                                }) {
    Swal.fire({
        title,
        icon,
        html: `
<input
    id="actionRemarks"
    class="swal2-input"
    placeholder="${textareaPlaceholder}"
    maxlength="250">

        `,
        showCancelButton: true,
        confirmButtonText: confirmText,
        confirmButtonColor: confirmColor,
        cancelButtonText: 'Cancel',
        focusConfirm: false,

        preConfirm: () => {
            const remarks = document.getElementById('actionRemarks')?.value.trim();

            if (textareaRequired && !remarks) {
                Swal.showValidationMessage('Remarks are required');
                return false;
            }
            return { remarks: remarks || '' };
        }
    }).then(result => {
        if (!result.isConfirmed) return;

        Swal.fire({
            title: 'Processing...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        secureFetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(result.value)
        })
            .then(data => {
                if (data.success) {
                    Swal.fire(successTitle, data.message, 'success');
                    tableToReload?.ajax.reload(null, false);
                } else {
                    handleEnterpriseError(data);
                }
            })
            .catch(() => {
                Swal.fire('Error', 'Unexpected server error', 'error');
            });
    });
}


function asgLoadDropdown({ url = null, data = null, elementId, selectedValue = null }) {

    const populate = (items) => {
        const dropdown = document.getElementById(elementId);

        // Keep first option (e.g., "Select...")
        const firstOption = dropdown.querySelector("option");
        dropdown.innerHTML = "";
        if (firstOption) dropdown.appendChild(firstOption);

        items.forEach(item => {
            const option = document.createElement("option");
            option.value = item.value;
            option.textContent = item.display;

            if (selectedValue && item.value === selectedValue) {
                option.selected = true;
            }
            dropdown.appendChild(option);
        });
    };

    // If data already provided → use it
    if (data) {
        populate(data);
        return;
    }

    // Otherwise fetch from API
    if (url) {
        secureFetch(url)
            .then(populate)
            .catch(error => {
                console.error(`Error loading ${elementId}:`, error);
            });
    }
}

window.ActionHandler = (function () {

    let cfg = {};

    function init(userCfg) {
        cfg = userCfg;
    }

    function handle(id, action) {
        const actionCfg = cfg.actions[action];
        if (!actionCfg) return console.warn('Unknown action:', action);

        const urlBase = `${cfg.baseUrl}/${id}`;

        // =========================
        // FETCH (VIEW / EDIT)
        // =========================
        if (actionCfg.type === 'fetch') {
            return secureFetch(urlBase).then(res => {
                if (!res.success) return Swal.fire('Error', res.message, 'error');

                if (actionCfg.modal === 'view') {
                    populateDocView(res.data);
                    bootstrap.Modal.getOrCreateInstance(
                        document.getElementById('defaultModalView')
                    ).show();
                }

                if (actionCfg.modal === 'edit') {
                    populateDocForm(res.data);
                    bootstrap.Modal.getOrCreateInstance(
                        document.getElementById('defaultModal')
                    ).show();
                }
            });
        }

        // =========================
        // DELETE
        // =========================
        if (actionCfg.type === 'delete') {
            return confirmAndExecute({
                title: 'Delete?',
                text: 'This cannot be undone.',
                icon: 'warning',
                confirmText: 'Delete',
                confirmColor: '#ef4444',
                url: urlBase,
                method: 'DELETE',
                successTitle: 'Deleted!',
                reloadTable: true,
                dataTable: cfg.table
            });
        }

        // =========================
        // DIALOG (APPROVE / SEND / ETC)
        // =========================
        if (actionCfg.type === 'dialog') {
            return approvalActionDialog({
                title: actionCfg.title,
                icon: 'question',
                confirmText: actionCfg.confirmText,
                confirmColor: actionCfg.confirmColor,
                textareaLabel: '',
                textareaPlaceholder: actionCfg.textareaPlaceholder,
                textareaRequired: actionCfg.textareaRequired,
                endpoint: `${urlBase}/${action}`,
                successTitle: actionCfg.successTitle,
                tableToReload: cfg.table
            });
        }
    }

    return {
        init,
        handle
    };

})();


window.S2HSHelper = (function () {

    function init(selector, url, placeholder, preId, preText, extraParams = {}) {
        const $el = $(selector);

        // 🔹 Destroy if already initialized
        if ($el.hasClass('select2-hidden-accessible')) {
            $el.select2('destroy');
        }

        $el.select2({
            dropdownParent: getDropdownParent($el),
            width: '100%',
            placeholder: placeholder || 'Select option',
            allowClear: true,
            minimumInputLength: 0,

            ajax: {
                url: url,
                dataType: 'json',
                delay: 250,

                data: function (params) {
                    return {
                        search: params.term || '',
                        page: params.page || 1,
                        ...extraParams   // 🔥 dynamic params
                    };
                },

                processResults: function (data) {
                    return {
                        results: (data.items || []).map(item => ({
                            id: item.id,
                            text: item.text
                        })),
                        pagination: {
                            more: data.hasMore
                        }
                    };
                },

                cache: true
            }
        });

        // 🔹 Preselect (for edit mode)
        if (preId) {
            const option = new Option(preText, preId, true, true);
            $el.append(option).trigger('change');
        }
    }

    // 🔹 Detect modal automatically
    function getDropdownParent($el) {
        const modal = $el.closest('.modal');
        return modal.length ? modal : $(document.body);
    }

    // 🔹 Clear value
    function clear(selector) {
        $(selector).val(null).trigger('change');
    }

    // 🔹 Set value programmatically
    function setValue(selector, id, text) {
        const $el = $(selector);
        const option = new Option(text, id, true, true);
        $el.append(option).trigger('change');
    }

    // 🔹 Get value
    function getValue(selector) {
        return $(selector).val();
    }

    return {
        init,
        clear,
        setValue,
        getValue
    };

})();