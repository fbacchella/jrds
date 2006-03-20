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
