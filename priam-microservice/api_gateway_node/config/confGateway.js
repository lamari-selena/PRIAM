// Webserver URL
const debug = require( "debug" )( "ConfServ" );
// Webserver URL
if ( process.env.GETWAY != undefined ) {
  module.exports.servURL = process.env.GETWAY;
} else {
  debug( "PLAGE_ENV environment variable not set. The Node process will be stopped." );
  process.exit( -1 );
}
// Project's name
module.exports.projet = "Plage";
module.exports.key = "8102";
