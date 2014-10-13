/**
* Module requirements
*/

var express = require('express');

var fs = require('fs');

var sio = require('socket.io');

var redis = require('connect-redis')(express);

var ssio = require('session.socket.io');

var url = require('url');

var path = require('path');

var TS = require('sqlspaces');

var clipitCom = require('./lib/clipitCommunication');



/**
* Include configuration
*/

var config = require('./config.js');


/**
* Include local libraries
*/

var mysocketio = require('./lib/socketio.js');

var ac = require('./lib/accesscontrol.js');

/**
* Users file for testing purposes
*/

// var users = require('./test-credentials');

/**
* Create server
*/

var app = express();

var server = require('https').createServer(config.ssloptions, app);
var httpServer = require('http').createServer(app);

/**
 * Configure server
 */

var SITE_SECRET = 'our analytics workbench secret';

app.use(express.bodyParser());

var cookieParser = express.cookieParser(SITE_SECRET);
var sessionStore = new redis();

app.use(cookieParser);
//app.use(express.session({ secret: 'our analytics workbench app' }));
app.use(express.session({ store: sessionStore }));

/**
 * Connect Socket.IO to server
 */

var io = sio.listen(server, config.ssloptions);

/**
 * Connect session store to sockets
 */

var sessionSockets = new ssio(io, sessionStore, cookieParser);

/**
 * Socket.IO stuff
 */

//io.sockets.on('connection', sf.onconnect);
sessionSockets.on('connection', sf.onconnect);

/**
 * Tuple Spaces
 */
var ts = new TS.TupleSpace(config.tsconfig, function() {
	clipitCom.getTupleSpace(ts);
	console.log('tspace created');
});

/**
 * First step: Redirect plain http server to secure server
 */
app.use(function (req, res, next) {
    	 if (req.secure) {
    	        // if it is a secure request, do not handle it here
    	        if(req.url.indexOf("newAdministration")!=-1){
    	           mysocketio.authAdmin(req.session.userId, function(isAdmin){
    	                 if(isAdmin){
    	                     next();
    	                 }
    	               else{
    	                     // if it is anything else, tell the user we don't have it
    	                     res.writeHead(404);
    	                     res.end('Not found!');
    	                 }
    	            });

    	        }
    	        else{
    	            next();
    	        }

    	    }
        // if it is a secure request, do not handle it here
        else {
        if ('/' == req.url && 'GET' == req.method) {
            // if it is a secure request for the site, redirect it
            var goal = [];
            goal.push('https://');
            goal.push(req.host);
            if (443 != config.secureport) {
                goal.push(':' + config.secureport);
            }
            goal.push('/');
            console.log('redirecting to ' + goal.join(''));
            res.redirect(goal.join(''));
        } else {
            // if it is anything else, tell the user we don't have it
            res.writeHead(404);
            res.end('Not found!');
        }
    }
});

/**
* Set static route for public files
*/

app.use('/', express.static(__dirname + '/public_html'));

/**
* Dynamic route for results things
*/

app.get(/results\/([^\/]*)\/([^\/]*)\/(.*)/, function (req, res, next) {

    if (req.session.logged_in) {
        console.log('Serving Run ID: ' + req.params[0] + ', Instance ID: ' + req.params[1] + ' to ' + req.session.user_name);
        var filepath = __dirname + path.normalize(decodeURI(url.parse(req.url).pathname));
        fs.stat(filepath, function (err, stat) {
            if (!err && stat.isFile()) {
                res.sendfile(__dirname + req.url);
            } else {
                next();
            }
        });
    } else {
        next();
    }

});

/**
 * Quasi-static route for kml - check if it is possible to do better checking here...
 */
app.get(/kml\/([^\/]*)\/([^\/]*)\/(.*)/, function (req, res, next) {
    console.log('Serving KML for Run ID: ' + req.params[0] + ', Instance ID: ' + req.params[1]);
    var filepath = __dirname + path.normalize(decodeURI(url.parse(req.url).pathname));
    fs.stat(filepath, function (err, stat) {
        if (!err && stat.isFile()) {
            res.sendfile(__dirname + req.url);
        } else {
            next();
        }
    });
});

app.get('/requestAvailableTemplates', clipitCom.requestAvailableTemplates);
app.post('/requestAnalysis', clipitCom.requestAnalysis);

/**
 * Middle ware to react in case of file not found (or not accessible)
 */

app.use(function (req, res, next) {
    res.writeHead(404);
    res.end('Not found!');
});

/**
* Start listening
*/

server.listen(config.secureport);
httpServer.listen(config.plainport);
console.log('Started NodeWorkbench on ' + config.plainport + ' and secure on ' + config.secureport);
