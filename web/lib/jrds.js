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

function sort_onClick()
{
	var url = location.href.replace(/sort=./, '')
	var separator = "&"
	if(location.search.length <1)
	    separator="?"
	window.location.replace( url + separator + "sort=1" );
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

function save_popup(id)
{
       var popupWin = window.open("download?id=" + id + "&" + periodUrl, name , "menubar=no,status=no,resizable=no,scrollbars=no");
}
