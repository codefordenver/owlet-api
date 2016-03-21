/**
 * to be run inside auth0 rules api
 */
function (user, context, callback) {
    var request = require('request');
    var url = 'TOP-SECRET';
    var admin = false;
    user.admin = admin;
    var payload = {
        user: user,
        context: context
    }
    request({url: url, method: 'PUT', json: payload}, function (error, response, body) {
        if (response.statusCode === 200) {
            console.log('success');
        } else {
            console.log('error: ' + response.statusCode);
            console.log(body);
        }
        callback(null, user, context);
    });
}
