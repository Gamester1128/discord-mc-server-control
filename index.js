const { Client, Intents } = require('discord.js');

const client = new Client({ intents: [Intents.FLAGS.GUILDS] });

client.once('ready', () => {
    console.log('Real Gamester1128 has spawned in your world!');
    
});

client.on()

// Get token and login
const { token } = require('./config.json');
client.login(token);
