const { Client, Intents, Emoji, ClientUser } = require('discord.js');
const copypasta = require("./copypasta.js")

const CHANNEL_GENERAL = '912818100305559585';

//const channel = Client.channels.cache.get(912818100305559585);

const client = new Client({
    intents: [
        Intents.FLAGS.GUILDS,
        Intents.FLAGS.GUILD_MESSAGES
    ]
});

client.once('ready', () => {
    console.log('Real Gamester1128 has spawned in your world!');

});

client.on('messageCreate', (message) => {

    //command for bot
    if (message.content.startsWith('!')) {
        let command = message.content.substring(1, message.content.length);
        sendToCP('/m/' + command + '/e/', PORT, HOST);

        //console.log(command);
    }
    //channel = client.channels.cache.get('912818100305559585');
    //channel.send('content');
    if (message.content.toLowerCase().includes('checkmate argument')) message.reply({ content: copypasta.checkmateArgument })
    else if (message.content.includes('rahul')) message.reply({ content: "the victim of someone's evil wrenches!!!!! his poor lambda duck friend Sadge" })
    else if (message.content === 'ping') message.react('👀')
    else if (message.content === 'deleena') message.channel.send({ content: copypasta.susASCII })
    else if (message.content.toLowerCase().includes('peak')) message.channel.send(copypasta.peakingAnimeGirl);
    else if (message.content.toLowerCase().includes('sadge')) message.channel.send(copypasta.sadge);

})


// Get token and login
const { token } = require('./config.json');
client.login(token);

// CLIENT FOR CONSOLE PROCESS SIDE STUFF

const PRINT_PACKETS = true;

const PREFIX_PING = '/i/';
const PREFIX_DISCONNECT = '/d/';
const PREFIX_CONNECT = '/c/';
const PREFIX_OUTPUT = '/o/';
const PREFIX_OUTPUT_START = '/s/'
const PREFIX_OUTPUT_END = '/e/'
const PREFIX_MESSAGE = "/m/";

const dgram = require('dgram');
const { channel } = require('diagnostics_channel');
const cp_client = dgram.createSocket('udp4');

const PORT = 7272;
const HOST = '127.0.0.1';
//server.bind(PORT, HOST);

var output = []

cp_client.on('listening', () => {
    console.log("DiscordBot ConsoleProcess Listener started on [" + HOST + "|" + PORT + "]");
});

cp_client.on('message', (msg, rinfo) => {
    var message = msg.toString();
    if (PRINT_PACKETS) console.log('R:' + message);
    if (message.startsWith(PREFIX_PING)) sendToCP(PREFIX_PING, PORT, HOST);
    else if (message.startsWith(PREFIX_OUTPUT_START)) output = new Array(parseInt(message.substring(3)));
    else if (message.startsWith(PREFIX_OUTPUT)) output.push(message.substring(3));
    else if (message.startsWith(PREFIX_OUTPUT_END)) flushToDiscord();
    else if (message.startsWith(PREFIX_MESSAGE)) client.channels.cache.get(CHANNEL_GENERAL).send(message.substring(3));

});

function flushToDiscord() {
    var message = output.join('');

    var messages = message.match(/(.|[\r\n]){1,2000}/g);
    //console.log(messages);
    //console.log("----------------num of messages: " + messages.length + " message size: " + message.length);
    for (let i = 0; i < messages.length; i++) {
        client.channels.cache.get(CHANNEL_GENERAL).send(messages[i]);
        //console.log(i + ": length " + messages[i].length);
    }

}

function sendToCP(msg, port, host) {
    cp_client.send(msg, port, host, sendingCallback);
    console.log('S:' + msg);
}

function sendingCallback(err) {
    if (err) throw err;
}

cp_client.send(PREFIX_CONNECT, PORT, HOST, sendingCallback)

process.on("SIGINT", () => {
    sendToCP(PREFIX_DISCONNECT, PORT, HOST);
    console.log("Closing discord bot.");
    client.destroy();
    process.nextTick(() => {
        cp_client.close();
        process.exit();
    });
});

