function calCloseBegin(cal)
{
	cal.hide();
	document.dateForm.scale.selectedIndex = 0;
	if(document.dateForm.end.value == "") {
	    document.dateForm.end.value = document.dateForm.begin.value;
	}
}

function calCloseEnd(cal)
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

function startCal(ifF, trigger, close)
{
	Calendar.setup( { inputField : ifF, button : trigger, displayArea: ifF, onClose : close });	
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

function autoscale_onClick() {
	document.dateForm.max.value = ""; 
	document.dateForm.min.value = ""; 
 
}

function download_onClick(url)
{
	var historyWin = window.open("download" + document.location.search, "download" + document.location.search, "menubar=no,status=no");
}

function details(url, name)
{
	var detailsWin = window.open(url, name, "resizable=yes,scrollbars=yes");
}

function popup(url,id)
{
	var img = document.getElementById(id);
	if(img != null) {
	   	var width = "width=" + img.width * 1.1;
		var height = "height=" + img.height * 1.1;
		var title = img.name;
	}
	else {
	   	var width = "width=750";
		var height = "height=500";
	}
	return popupWin = window.open(url, "_blank" , height + "," + width + ",menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}

function save_popup(url, name)
{
       var popupWin = window.open(url, name , "menubar=no,status=no,resizable=no,scrollbars=no");
}

function history_popup(url, name)
{
	var historyWin = window.open(url, "_blank", "width=750,menubar=no,status=no,resizable=yes,scrollbars=yes,location=yes");
}
