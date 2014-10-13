	var TS = require('sqlspaces');
	
	var uuid = require('node-uuid');
	
	var util = require('./util');
	
	var config = require('../config');
	
	var ts;
	/**
	 * Get Access to TupleSpace from app.js
	 */
	exports.getTupleSpace = function(tuplespace) {
		ts = tuplespace;
	}
	/**
	 * This Webservice return all available templates for Clipit in JSON-Format:
	 * JSON-Format: {
					TemplateId: the templateid,
					Name: name of the template,
					Description: description of the template
				}
	 */
	exports.requestAvailableTemplates = function (req, res) {
		ts.readAll(new TS.Tuple([TS.fString, 3, TS.fString, TS.fString, TS.fString]), function(data) {
			//The Return Array
			var returndata = [];
			
			//Build JSON-Format
			for (var i in data) {
				returndata.push({
					TemplateId: data[i].getField(0).getValue(),
					Name: data[i].getField(2).getValue(),
					Description: data[i].getField(3).getValue()
				});
			}	
			
			res.send(returndata);
		});
	}

	/**
	 * This Webservice get a post analysisrequest and post it to SQLSpaces
	 * The POST-Format: {
					TemplateId: 	the templateid,
					AnalysisData: 	the data to analyse
					ReturnURL:		the answerhost of clipit
					ReturnId: 		the returnid of clipit
				}
	 */
	exports.requestAnalysis = function(req, res) {
		try {
			var TemplateId		= req.body.TemplateId,		//ID of the Template
				ReturnId		= req.body.ReturnId,		//not used
				AnalysisData	= new Buffer(req.body.AnalysisData, 'base64').toString('utf-8'),	//Data to Analyse
				ReturnURL		= req.body.ReturnURL,
				AuthToken       = req.body.AuthToken,
				run_uuid		= uuid.v4();				//a new run UUID
			ts.read(new TS.Tuple([TemplateId, 3, TS.fString, TS.fString, TS.fString]), function(template) {
				/**
				 * 0=templateid		template.getField(0).getValue()
				 * 1=3				template.getField(1).getValue()
				 * 2=name			template.getField(2).getValue()
				 * 3=desc			template.getField(3).getValue()
				 * 4=agents tuple	var agents = new TS.Tuple(); agents.fromJSON(template.getField(4).getValue());    
				 */
				var agentsTupleList = [];
				var agentsList = JSON.parse(template.getField(4).getValue());
				var startProcess;
				for(var i in agentsList) {
					var templateAgent = new TS.Tuple();
					templateAgent.fromJSON(agentsList[i].tuple);
					/**
					 * 4=agents tuples
					 * 0=run_uuid		Overwrite with run_uuid
					 * 1=2
					 * 2=3 				Overwrite with 1 for new run
					 * 3=AgentID
					 * 4=Agent Name
					 * 5=Callback 		ID Agent
					 * 6=Parameter		Parameter
					 */
					if(templateAgent.getField(5).getValue().trim() == '') {
						startProcess = templateAgent.getField(3).getValue();
					}
					var agentTuple = new TS.Tuple([ run_uuid, 2, 1, templateAgent.getField(3).getValue(), templateAgent.getField(4).getValue(), templateAgent.getField(5).getValue(), templateAgent.getField(6).getValue() ]);
					agentsTupleList.push(agentTuple);
				}
				var processTuple = new TS.Tuple([
					run_uuid,
					7,
					TemplateId,
					3,
					util.dateTimeString(new Date()),
					1,
					'run_' + run_uuid,
					''
				]);
				var data = [{filename: 'dummy.net', filetype: 'net', specialfiletype: 'TEXT', filedata: AnalysisData}];
				var startTuple = new TS.Tuple([
					run_uuid,
					1,
					startProcess + '.out_1',
					JSON.stringify(data),
					''
				]);
				var ClipitRequestsTuple = new TS.Tuple([
					run_uuid,
					ReturnURL,
					ReturnId,
					AuthToken
				]);
				var clipitRequestTupleSpace = new TS.TupleSpace({ host: config.tsconfig.host, port: config.tsconfig.port, space: 'ClipitRequests'}, function() {
					clipitRequestTupleSpace.write(ClipitRequestsTuple, function() {
						clipitRequestTupleSpace.disconnect();
						ts.write(processTuple, function() {
							writeTuples(0, agentsTupleList, function() {
								ts.write(startTuple, function() {
									res.send(run_uuid);
								});
							});
						});
					});
				});
				
				
			});
		} catch(err) {
			res.send(err);
		} 
	}

	var writeTuples = function(index, data, callback) {
		if (index < data.length) {
			ts.write(data[index], function() {
				writeTuples(++index, data, callback);
			});
		} else {
			callback();
		}
	}
