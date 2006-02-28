<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<jsp:useBean id="jrdsBean" class="jrds.webapp.TreeJspBean" />
<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Tree Frame</title>
		<style type="text/css"><!--
A:visited { color: rgb(0, 0, 238); }
--></style>
			<script type="text/javascript" src="lib/ua.js"> </script>
			<script type="text/javascript" src="lib/ftiens4.js"> </script>
			<script type="text/javascript">
USETEXTLINKS = 1  
STARTALLOPEN = 0
HIGHLIGHT = 1
PRESERVESTATE = 0
GLOBALTARGET = 'R'
ICONPATH="img/"

<%
	jrdsBean.getJavascriptTree(1, "hostTree_0", out, request);
	jrdsBean.getJavascriptTree(2, "viewTree_0", out, request); 
%>

foldersTree = gFld("<i>Graph List</i>");
foldersTree.addChildren([hostTree_0, viewTree_0]);
</script>
	</head>

	<body bgcolor="#ffffff">
<div style="position:absolute; top:0; left:0; "><table border=0><tr><td><font size=-2><a style="font-size:7pt;text-decoration:none;color:silver" href="http://www.treemenu.net/" target="_blank">JavaScript Tree Menu</a></font></td></tr></table></div>
		<p>
<script type="text/javascript"><!--
initializeDocument();
 //-->
</script>
		</p>
<noscript>
A tree for site navigation will open here if you enable JavaScript in your browser.
</noscript>
	</body>

</html>
