var http = require('http');

if(process.argv.length != 4) {
        console.log("Usage: " + process.argv[0] + " " + process.argv[1] + " <total_requests> <pause_between_requests_in_millis>");
        return;
}

var TOTAL_REQUESTS = process.argv[2];
var PAUSE_BETWEEN_REQUESTS = process.argv[3];


var results = {
        elapsed: []
};
var requestCounter = 0;
var testStartMillis = (new Date()).getTime();
console.log("Start ms: " + testStartMillis);

var load = function(properties) {
        var startMs = (new Date()).getTime();
        var client = http.createClient(properties.port, properties.host);
        var request = client.request('GET', properties.path + "?" + properties.query, {'host': properties.host});
        request.end();

        request.on('response', function (response) {
                console.log('STATUS: ' + response.statusCode);
                //console.log('HEADERS: ' + JSON.stringify(response.headers));
                response.setEncoding('utf8');

                response.on('data', function (chunk) {
                        console.log('BODY: ' + chunk);
                });

                response.on('end', function (chunk) {
                        var elapsed = (new Date()).getTime() - startMs;
                        console.log('Request returned in ' + elapsed + 'ms');
                        results.elapsed.push(elapsed);
                });

                response.on('error', function (err) {
                        console.log(err);
                });
        });

        requestCounter++;
};

var showResults = function() {
        if(results.elapsed.length != TOTAL_REQUESTS) {
                setTimeout(showResults, 1000);
                return;
        }

        console.log("now: " + ((new Date()).getTime()) + ", startMs: " + testStartMillis);
        var testElapsed = (new Date()).getTime() - testStartMillis;
        var sum = 0;
        var min = 999999;
        var max = 0;

        for(var i = 0; i < results.elapsed.length; i++) {
                sum+= results.elapsed[i];
                if(results.elapsed[i] < min) {
                        min = results.elapsed[i];
                }else if(results.elapsed[i] > max) {
                        max = results.elapsed[i];
                }
        }
        var avg = sum/results.elapsed.length;

        console.log("");
        console.log("-- Results --");
        console.log("Average Response Time: " + avg + "ms");
        console.log("Min Response Time: " + min + "ms");
        console.log("Max Response Time: " + max + "ms");
        console.log("Requests Sent: " + requestCounter);
        console.log("Total Test Time: " + testElapsed + "ms");
        console.log("Actual Pause Between Requests: " + (testElapsed / (requestCounter*1.0)) + "ms");

};

try {
        for(var i = 0; i < TOTAL_REQUESTS; i++) {
                setTimeout(function() {
                        load({
                                host: 'HOST',
                                port: 80,
                                path: 'PATH',
                                query: 'method=server.status'
                        });
                }, i*PAUSE_BETWEEN_REQUESTS);
        }
} finally {
        showResults();
}

