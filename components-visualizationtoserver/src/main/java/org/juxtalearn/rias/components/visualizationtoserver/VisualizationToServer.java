package org.juxtalearn.rias.components.visualizationtoserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;


import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.w3c.dom.CDATASection;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import info.collide.sqlspaces.client.TupleSpace;
import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;
import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.SISOBProperties;

class PlaceHolderFile {
	public String placeholderName;

	public String fileData;
}

public class VisualizationToServer extends Agent {

	private int errorCallbackNo;

	public VisualizationToServer(Tuple commandTuple, String serverlocation,
			int port) {
		super(commandTuple, serverlocation, port);
		this.commandTupleStructure = commandTuple;
		Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue()
				.toString(), 1, commandTuple.getField(5).getValue().toString(),
				Field.createWildCardField());
		setDataTupleStructure(dataTemplate);
	}

	@Override
	public void initializeAgent() {
		super.initializeAgent();
		try {
			// Error: Runid, 5, String String String
			this.errorCallbackNo = sisobspace.eventRegister(Command.WRITE, new Tuple(this.commandTupleStructure.getField(0).getValue().toString(), 5, String.class, String.class, String.class), this, true);
		} catch (TupleSpaceException e) {
			logger.severe(e.getMessage());
		}
	}

	@Override
	public void call(Command cmnd, int callbackNo, Tuple afterTuple, Tuple beforeTuple) {
		if (this.errorCallbackNo==callbackNo) 
			errorHandler(afterTuple);
		else
			super.call(cmnd, callbackNo, afterTuple, beforeTuple);
	}

	private void errorHandler(Tuple fetchedTuple) {
		try {
			TupleSpace ts = new TupleSpace(super.serverLocation, super.port, "ClipitRequests");
			
			//If there are Parameters from NodeWorkbench Visual Komponent, write them to ClipitRequests
			writeParameterTupleToClipitRequests(ts);
			
			String data = fetchedTuple.getField(2).getValue().toString() + " : " + fetchedTuple.getField(3).getValue().toString() + " : " + fetchedTuple.getField(4).getValue().toString();
			
			//get URL and ReturnID Parameter from ClipitRequest Tuple
			Tuple clipitRequestTuple = ts.take(new Tuple(
					this.commandTupleStructure.getField(0).getValue()
							.toString(), String.class, String.class));
			
			if(clipitRequestTuple != null) {
				String url = clipitRequestTuple.getField(1).getValue().toString(); 
						//getPostURL(clipitRequestTuple);
				String returnId = clipitRequestTuple.getField(2).getValue()
						.toString();
				postToURL(url, returnId, data, 5);
			}
			
		} catch (TupleSpaceException e) {
			logger.severe(e.getMessage());
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}
	}
	
	private void writeParameterTupleToClipitRequests(TupleSpace ts) throws TupleSpaceException {
		String parameters = this.getCommandTupleStructure().getField(6)
				.getValue().toString();
		if(parameters.trim() != "") {
			ts.write(new Tuple(this.commandTupleStructure
					.getField(0).getValue().toString(), parameters, ""));
		}
	}
	
	@Override
	public void executeAgent(Tuple fetchedTuple) {
		this.indicateRunning();
		try {
			// 0 = host
			// 1 = port
			// 2 = Endpoint
			
			String data = generateHTMLData(fetchedTuple);

			TupleSpace ts = new TupleSpace(super.serverLocation, super.port, "ClipitRequests");
			
			//If there are Parameters from NodeWorkbench Visual Komponent, write them to ClipitRequests
			writeParameterTupleToClipitRequests(ts);
			
			Tuple clipitRequestTuple = ts.take(new Tuple(
					this.commandTupleStructure.getField(0).getValue()
							.toString(), String.class, String.class));

			if (clipitRequestTuple != null) {
				String url = clipitRequestTuple.getField(1).getValue().toString(); 
//						getPostURL(clipitRequestTuple);
				String returnId = clipitRequestTuple.getField(2).getValue()
						.toString();
				String returnValue = postToURL(url, returnId, data, 3);
				logger.fine(returnValue);
			}

			this.indicateDone();

		} catch (JSONException e) {
			this.indicateError(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			this.indicateError(e.getMessage());
			e.printStackTrace();
		} catch (TupleSpaceException e) {
			this.indicateError(e.getMessage());
		}

	}

	public String postToURL(String urlString, String returnId, String data,
			int status) throws IOException {
		if (!urlString.startsWith("http://")
				&& !urlString.startsWith("https://")) {
			urlString = "http://" + urlString;
		}
		URL url = new URL(urlString);
		System.out.println(urlString);
		

		String body = "returnId=" + URLEncoder.encode(returnId, "UTF-8") + "&"
				+ "data=" + URLEncoder.encode(data, "UTF-8") + "&"
				+ "statuscode=" + URLEncoder.encode(Integer.toString(status), "UTF-8");

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		connection.setRequestProperty("Content-Length",
				String.valueOf(body.length()));

		OutputStreamWriter writer = new OutputStreamWriter(
				connection.getOutputStream());
		writer.write(body);
		writer.flush();

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));

		String returnData = "";
		for (String line; (line = reader.readLine()) != null;) {
			returnData += line;
		}

		reader.close();
		writer.close();

		return returnData;
	}

	private String generateHTMLData(Tuple fetchedTuple) throws JSONException,
			IOException {
		// Read fetchedTuple Parameter from TupleSpace
		JSONObject data = new JSONObject(fetchedTuple.getField(3).getValue()
						.toString());
		
		File jsHome = new File(data.get("folder").toString());
		// get html File
		File htmlFile = jsHome.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".html");
			};
		})[0];

		// write css and js files to
		Document htmlDoc = Jsoup.parse(htmlFile, "UTF-8", "http://localhost/");
		// Html Head
		Node htmlHead = htmlDoc.head();

		List<PlaceHolderFile> placeholderFiles = new ArrayList<PlaceHolderFile>();

		for (int i = 0; i < htmlHead.childNodes().size(); i++) {
			// old
			// Element nodeAttr = (Element) htmlHead.childNodes().get(i);
			Node nodeAttr = htmlHead.childNode(i);
			if (nodeAttr.nodeName() == "script") {

				if (nodeAttr.hasAttr("src")) {
					// javascript file
					if (!nodeAttr.attr("src").startsWith("http")) {
						// Only relative Path

						// get SRC Value
						String jsFilePath = data.get("folder").toString() + nodeAttr.attr("src");

						try {
							// Load JS-File and Minify it
							FileReader file = new FileReader(jsFilePath);
							ClosureMinifier minify = new ClosureMinifier(file);
							String jsFile = minify.minify();

							// Set a Placeholder Object with JS-Data and a
							// placeholderID
							PlaceHolderFile filedata = new PlaceHolderFile();
							filedata.placeholderName = "###" + jsFilePath
									+ new Date().getTime() + "###";
							filedata.fileData = jsFile;
							placeholderFiles.add(filedata);

							// write JS File PLACEHOLDER to script body
							((Element) nodeAttr).html(filedata.placeholderName);

							// remove SRC Link
							nodeAttr.removeAttr("src");
						} catch (FileNotFoundException ex) {
							ex.printStackTrace();
						}
					}
				}
			} else if (nodeAttr.nodeName() == "link") {
				// CSS Files
				if (!nodeAttr.attr("href").startsWith("http")) {
					// get HREF Value
					String cssFilePath = data.get("folder").toString() + nodeAttr.attr("href");

					try {
						// read CSS File
						String cssFile = readAndCompressCSSFile(cssFilePath);
						// create new Style Node
						Element cssStyle = htmlDoc.createElement("style");
						cssStyle.attr("type", "text/css");
						// Set a Placeholder Object with CSS-Data and a
						// placeholderID
						PlaceHolderFile filedata = new PlaceHolderFile();
						filedata.placeholderName = "###" + cssFilePath
								+ new Date().getTime() + "###";
						filedata.fileData = cssFile;
						placeholderFiles.add(filedata);

						// Write CSS File PLACEHOLDER to style body
						cssStyle.html(filedata.placeholderName);
						// replace existing link element with new style
						nodeAttr.replaceWith(cssStyle);
					} catch (IOException ex) {
						ex.printStackTrace();
					}

				}
			}
		}

		// Create a JS-inline Tag to overwrite the start Method
		Element overwriteStartScript = htmlDoc.createElement("script");
		overwriteStartScript.attr("type", "text/javascript");
		
		JSONArray src = new JSONArray(data.get("src").toString());
		// JSONArray resultData = new JSONArray();
		String resultData = "[";
		String srcData = "src = " + src.toString() + ";";
		
		for (int i = 0; i < src.length(); i++) {
			resultData += data.get(src.get(i).toString()) + ",";
			// System.out.println(data.get(src.get(i).toString()));
			// resultData.put(data.get(src.get(i).toString()));
		}
		resultData = resultData.substring(0, resultData.length() - 1);
		resultData += "]";
		
		PlaceHolderFile startScript = new PlaceHolderFile();
		startScript.placeholderName = "###" + "OVERWRITESTARTSCRIPT"
				+ new Date().getTime() + "###";
		startScript.fileData = "function start() {" + srcData + "inputData = "
				+ resultData.toString() + ";" + "for(var i in inputData) {"
				+ "this.data = inputData[i].data;"
				+ "initialize(inputData[i].metadata, inputData[i].data);" + "}"
				+ "}";
		placeholderFiles.add(startScript);
		overwriteStartScript.html(startScript.placeholderName);
		htmlDoc.head().appendChild(overwriteStartScript);

		String htmlData = htmlDoc.toString();

		// Replace all placeholderitems in html with css and js data, because
		// Jsoup has a problem with JavaScript Files!
		for (int i = 0; i < placeholderFiles.size(); i++) {
			htmlData = htmlData.replace(
					placeholderFiles.get(i).placeholderName,
					placeholderFiles.get(i).fileData);
		}

		return htmlData;

	}

	@Override
	public void executeAgent(ArrayList<Tuple> fetchedTuples) {

	}

	@Override
	protected void uploadResults() {
		super.indicateDone();

	}

	private String readAndCompressCSSFile(String inputFile)
			throws FileNotFoundException, IOException {
		Writer out = null;
		Reader in = null;
		try {
			in = new InputStreamReader(new FileInputStream(inputFile), "UTF-8");
			CssCompressor compressor = new CssCompressor(in);
			in.close();

			out = new StringWriter();
			compressor.compress(out, -1);

			return out.toString();
		} catch (EvaluatorException e) {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			return readFromFile(inputFile);
		}

	}

	private String readFromFile(String filePath) throws FileNotFoundException,
			IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		StringBuffer content = new StringBuffer();
		String s;
		while ((s = br.readLine()) != null) {
			content.append(s);
		}
		br.close();
		return content.toString();
	}

}
