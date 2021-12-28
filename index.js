const { Client, Intents, Emoji } = require('discord.js');
const copypasta = require("./copypasta.js")

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
    if (message.content.startsWith('/')) {
        var command = message.content.substring(1, message.content.length);
        console.log(command);

        sendToCP('/m/' + command + '/e/', PORT, HOST);
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

const dgram = require('dgram');
const cp_client = dgram.createSocket('udp4');

const PORT = 7272;
const HOST = '127.0.0.1';
//server.bind(PORT, HOST);

cp_client.on('listening', () => {
    console.log("DiscordBot ConsoleProcess Listener started on [" + HOST + "|" + PORT + "]");
});

cp_client.on('message', (msg, rinfo) => {
    message = msg.toString();
    if (message === "/i/") sendToCP('/i/', PORT, HOST);
    console.log('HI');
});

function sendToCP(msg, port, host) {
    cp_client.send(msg, port, host, sendingCallback);
    console.log('S:' + msg);
}

function sendingCallback(err) {
    if (err) throw err;
}

cp_client.send('/c/', PORT, HOST, sendingCallback)

process.on("SIGINT", () => {
    sendToCP('/d/', PORT, HOST);
    console.log("Closing discord bot.");
    client.destroy();
    process.nextTick(() => {
        cp_client.close();
        process.exit();
    });
});

