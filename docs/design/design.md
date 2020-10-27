# github.com/DeathStarGame idea and design

## contents

- the idea
  - enjoying watching e-esports on twitch.tv
  - wanting to play a gmae and/or compete in e-sports
  - games on planet Earth in 2020
  - outraged with every e-sports game being about clicking, micro and praising mechanical repetitiveness
  - the type of games and e-sports we're missing
  - wanting a global automated seasonal tournament system
  - thinking turn-based strategy like Heroes of Might and Magic 3
  - realizing limitations of just-one-game, factions/races
  - disgusted with competetion-for-resources mandatory (and only) nature of every game
  - disgusted with randomness and the culture of "oh, bad RNG, I was unlucky" lowering e-sports down to the level of some gambly card game
  - games should allow for building, creation, as well as competetion-for-resources, conquest
  - wondering: what is building? if not clicking, than what?
  - realizing that with the language gameplay the number of game scnearios can be unlimited
  - programming is a game in itself, it can be abstracted into a dynamic competetive e-sports
  - dreaming a decentralized global app with an official non-commercial automated tournament system as well as user created tournaments
- non-commercial: listening to Jesus, cause he knows
  - best place to build an inspiring spacious free game is on a playground
- observing the marvel of open source, Linux, github, youtube
  - mailing list
  - forks
  - youtube channels for projects - example of IPFS
  - realizing contributors' individual repos are naturally namespaced for reserach and expriments
- thinking self-hostable in docker system with browser ui
  - a standard idea of a game server, but using modern technolgy and browser ui
  - realzing it is always a full-fledged system of users and their data
  - building a authentication and understanding: identity should be a layer, has nothing to do with the game
  - thinking a complete micro-service system with identity, data flow, observability and app being an addon/extension
  - realizing limitation of HTTP as being request/response only
  - discovering RSocket
  - still, tools, tools, tools, but not the game
- thinking of making a game browser-only with WebRTC
  - looking into libp2p and how peers could connect from many browser tabs to one
  - WebRTC is fragile: relying on signal servers is not a browser-only app
  - understanding single threaded limitation: webworkers yes, but they cannot talk to each other
  - and overall, hosting in browser it's not a long-term great game, but a trick, a shortcut - and those always lead to even more problems
- discovering IPFS: peer-to-peer, distributed, decentralized web3.0
  - IPFS node is networking and identity, things that were missing in self-hostable system design
  - it's peers, not sockets: we already have a global planetary network where peers can host games and connect
  - thinking developing multiplayer on a single machine: every virtual palyer must have an instance of IPFS node
  - thinking delivery: what should user install? an app? a docker container? how to keep it updated?
  - yes, it's not yet global as there is no global-database-tool, but we can host-connect right way, through living peer network
- design
  - hosting a game
  - installation and updates
  - creating scenarios



