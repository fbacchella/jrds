<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<jsp:useBean id="jrdsBean" class="jrds.TreeJspBean" />
<jsp:setProperty name="jrdsBean" property="*" />

<html>

	<head>
		<meta http-equiv="content-type" content="text/html;charset=utf-8">
		<title>Tree Frame</title>
		<style type="text/css"><!--
A:visited { color: rgb(0, 0, 238); }
--></style>
	</head>

	<body bgcolor="#ffffff">
		<p>
			<script type="text/javascript" src="lib/cooltree.js"></script>
			<script type="text/javascript"><!--
			 // Part 1 - Tree nodes definition
var TREE1_NODES = [
	['Host tree', '', '_', <%= jrdsBean.getJavascriptTree(1) %> ],
	['View tree', '', '_', <%= jrdsBean.getJavascriptTree(2) %>	 ],
	
];				

 // Part 2 - Tree format
 var TREE1_FORMAT = [
	10,							// 0. x coordinate
 	10,							// 1. y coordinate
	true,						// 2. button images flag
	[							// 3. button images:
		"img/c.gif",			//	collapsed,
 		"img/e.gif",			//	expanded,
		"img/b.gif"				//	blank
 	],
 	[ 16, 16, 0],				// 4. button images size: width, height,
								// and indentation for childless nodes
 	true,						// 5. folder images flag
 	[							// 6. folder images:
		"img/fc.gif",			//		closed,
		"img/fe.gif", 			//		opened,
		"img/d.gif"				//		document
 	],
	[ 16, 16],					// 7. folder images size: width, height
 	[ 0, 16, 32, 48, 64, 80 ],	// 8. indentation for each level
 	"white",					// 9. background color for the whole tree
 	"clsNode",					// 10. default CSS class for nodes
 	[ "cl1", "cl2" ], 			// 11. CSS classes for each level
 	false,						// 12. single branch mode flag
 	[ 0, 0 ]					// 13. item padding and spacing
 ];
 var myTree = new COOLjsTree("tree1", TREE1_NODES, TREE1_FORMAT);
//-->
</script>
		</p>
	</body>

</html>
