var params = Spry.Utils.getLocationParamsAsObject();

var qsParm = new Array();

qsParm['OverviewPanel'] = null;
qsParm['TreeInferenceProgramsPanel'] = null;
qsParm['SOWHTestPanel'] = null;
qsParm['CreditPanel'] = null;
qsParm['HelpPanel'] = null;
qsParm['TechnicalPanel'] = null;

qs();

function qs() {
	var query = window.location.search.substring(1);
	var parms = query.split('&');
	for (var i=0; i<parms.length; i++) {
		var pos = parms[i].indexOf('=');
		if (pos > 0) {
			var key = parms[i].substring(0,pos);
			var val = parms[i].substring(pos+1);
			qsParm[key] = val;
		}
	}
}

parameterString = function()
{
	var paramString = '';
	if (OverviewPanel.isOpen()) {
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'OverviewPanel=open';
	}
	if (LocationPanel.isOpen()) {
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'LocationPanel=open';
	}
	if (TreeInferenceProgramsPanel.isOpen()) {
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'TreeInferenceProgramsPanel=open';
	}
	if (SOWHTestPanel.isOpen()){
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'SOWHTestPanel=open';
	}
	if (CreditPanel.isOpen()){
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'CreditPanel=open';
	}
	if (HelpPanel.isOpen()){
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'HelpPanel=open';
	}
	if (TechnicalPanel.isOpen()){
		if (paramString=='') paramString+='?'; else paramString+='&';
		paramString+= 'TechnicalPanel=open';
	}

	return paramString;
};


pageLink = function(page)
{
	document.location.href = page + parameterString();
};

