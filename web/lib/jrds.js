function calClose(cal)
{
	document.dateForm.scale.selectedIndex = 0;
	cal.hide();
}

function submitScale(scale)
{
	if(scale.selectedIndex > 0) {
		var form = scale.form;
		form.begin.value="";
		form.end.value="";
		return scale.form.submit();
	}
}

function startCal(ifF, trigger)
{
	Calendar.setup( { inputField : ifF, button : trigger, onClose : calClose });
}
//the query string analyzer
qs = new Querystring();

function refresh_onClick()
{
	window.location.reload( false );
}

function history_popup(id, name)
{
	var historyWin = window.open("history.jsp?id=" + id, name, "width=700,menubar=no,status=no,resizable=yes");
}

function keep_onClick()
{
	var historyWin = window.open(window.location, qs.get("id", 0).replace("-","_"), "menubar=no,status=no,resizable=yes");
}

function download_onClick()
{
	var historyWin = window.open("download" + document.location.search, "download" + document.location.search, "menubar=no,status=no");
}

function details(id, name)
{
	var detailsWin = window.open("details?id=" + id + "&" + periodUrl, name, "resizable=yes,scrollbars=yes");
}

function popup(id)
{
	var img = document.getElementById(id);
	var width = "width=" + img.width * 1.1;
	var height = "height=" + img.height * 1.1;
	var title = img.name;
	var popupWin = window.open("popup.jsp?id=" + id + "&" + periodUrl, name , height + "," + width + ",menubar=no,status=no,resizable=no,scrollbars=no");
}

