 // ********************************************
 // Récupération de paramètre d'une requête HTTP
 // ou récupération des données d'un formulaire.
 // Auteur : Oznog (www.trucsweb.com)
 // ********************************************

 // NE PAS MODIFIER CE CODE
 var paramOk = true;

 function FaitTableau(n) {
   // Création d'un tableau (array)
   // aux dimensions du nombre de paramètres.
   this.length = n;
   for (var i = 0; i <= n; i++) {
     this[i] = 0
   }
   return this
 }

 function ParamValeur(nValeur) {
   // Récupération de la valeur d'une variable
   // Pour créer la variable en Javascript.
   var nTemp = "";
   for (var i=0;i<(param.length+1);i++) {
     if (param[i].substring(0,param[i].indexOf("=")) == nValeur)
       nTemp = param[i].substring(param[i].indexOf("=")+1,param[i].length)
   }
   return Decode(nTemp)
 }

 // Extraction des paramètres de la requête HTTP
 // et initialise la variable "paramOk" à false
 // s'il n'y a aucun paramètre.
 if (!location.search) {
   paramOk = false;
 }
 else {
   // Éliminer le "?"
   nReq = location.search.substring(1,location.search.length)
   // Extrait les différents paramètres avec leur valeur.
   nReq = nReq.split("&");
   param = new FaitTableau(nReq.length-1)
   for (var i=0;i<(nReq.length);i++) {
     param[i] = nReq[i]
   }
 }

 // Décoder la requête HTTP
 // manuellement pour le signe (+)
 function Decode(tChaine) {
   while (true) {
     var i = tChaine.indexOf('+');
     if (i < 0) break;
     tChaine = tChaine.substring(0,i) + '%20' + tChaine.substring(i + 1, tChaine.length);
   }
   return unescape(tChaine)
 }

function setCgiArg(key, val) {
	var retValue;
	var newArg = escape(key) + "=" + escape(val);
	var param = location.search;
	var rtoreplace = new RegExp("[?&]" + key + "=[^&]*(&?)");
	param = param.replace(rtoreplace, "$1");
	if(param.substring(0,1) == "?")
		retValue=param + "&" + newArg;
	else  if(param.substring(0,1) == "&")
		retValue= "?" + newArg + param;
	else
		retValue = "?" + newArg;
	return retValue;
}

function clearCgiArg(key) {
	var retValue;
	var param = location.search;
	var rtoreplace = new RegExp("([?&])" + key + "=[^&]*&?");
	retValue = param.replace(rtoreplace, "$1");
	if(retValue.length == 1)
		retValue="";
	else if(retValue.substring(retValue.length -1, retValue.length) == "&")
		retValue=retValue.substring(0, retValue.length - 1);
	return retValue;
}
