Continuation of:

- [../cloud-native-system/design.md](../cloud-native-system/design.md)
- [../search-for-the-game.md](../search-for-the-game.md)
- [../origin-cluster/origin-cluster.md](../origin-cluster/origin-cluster.md)
- [../as-vscode-extension.md](../deathstar.ltee/as-vscode-extension.md)
- [../deathstar.ltee/design-notes.md](../deathstar.ltee/design-notes.md)
- [../play-scenarios-in-browser/design-notes.md](../play-scenarios-in-browser/design-notes.md)

## deathstar design

<img height="512px" src="../play-scenarios-in-browser/svg/2020-10-26-IPFS-peers.svg"></img>

- app is some form of desktop instance with an IPFS node included
- IPFS node provides networking and idenetity
- through IPFS node we discover other peers, connect to host peers and bidirectionally exchange data
- the UI runs either in browser or in a webrenderer of a desktop app - so web ui in every case
- docker-to-desktop-app exists?  "take this docker compose deploymen and turn into installable updatable app" ?
  - if exists, app can have browser GUI over rsocket (it's just a renderer)
- app has a jvm-app that is always non-binary so that eval works - this app hosts games
- updates : app or deployment should be installable and auto-updatable
- app comes with: IPFS node, jvm-app, ui
- delivery with docker app: build, share, and run a set of microservices as a single entity
  - users will need to install docker, and docker app plugin
  - then, with a single command install an app (or another instance of it) `docker app install myuser/hello-world:0.1.0 --set hello.port=8181`
  - app ui will notify user about updates and show two commands: one to run a new version of an app, and another (after) to uninstall the previous

## stage 1

- add 
  - traefik (serves ui, proxies rsocket to app directly, serves IPFS node ui)
  - ui-prod ui-dev (dev only builds, ui-prod always serves)
  - jvm-app (has rsocket and can talk to ui)
  - IPFS node (just runs on it's own at this point and we can access it's ui via traefik)
- make this one instance launchable: we launch everything, then jvm-app manually
- make several named instances launchable, such that we only toggle jvm-app 

## stage 2

- explore IPFS node

## one way to develop system and scenarios

- developer or not - there should be a unified single way to develop sceanrios
- and naturally, we developers want to develop them within the system (so we start the whole thing and we can delop it and/or scenarios)
- so what we do it build Death Star game, the same process should be exposed to any user if they want to develop their scenarios (choose only how many player instances to launch in docker)
- but, it should be so that they can use their own repositories - or, simply be able once done to take files and put them themselves wherever
- and the system can download sceanrios from any git repo
- so one system, one way to build it and scenarios within it, so anybody is a user/palyer/developer

## with one system, should browser vscode be in its own tab and game gui in its own?

- if so, VSCode will have a minimal extension for it that will carry out ops originating from jvm-app
- jvm-app and vscode may even share a filesystem inside a docker volume
- jvm-app will say to extension "open this and that file for the user" or say "show timer" or may ask "give me that file" (if they don't share fs, otherwise jvm-app will read itlsef, which is preferable)
- users can download files they edit with browser vscode within the system
- and game GUI will be a standalone app communicating with jvm-app to show scenario's graphics and multiplayer etc.

<img height="512px" src="./svg/2020-10-31-IPFS+vscode.svg"></img>


## OS windows for seeing the editor and game gui simulteneously?

- open game gui tab in the brower tab in a new browser window taking half-screen (rigth side)
- open editor tab in another browser window taking the left side of the scree
- drag middle boudary to make one or another bigger
- why
  - if we build our game ui and editor as part of it, we dicard all existing tooling and we'll need to make an editor,clj extension, repl
  - can we leverage the fact that editors exist and build on that? e.g. by using vscode-in-the-browser
  - from user experience standpoint: it is almost the same as having our own tabs and windows inside our ui, whereas OS level windows do exactly that
  - another: links and decoupling from single-ui/single tool
  - although ui runs on localhost, links like `game/player/stats/?whatever=3` should be exchangable ideally, like it already works on the web
  - in the game, such link can be part of a another app's ui and will eventually result in a global decentralzied query (or local for starters) and any peer will see the resulting page, so links would work
- if we use browser tabs themselves as tabs, we can rely on link and think in terms of apps
- it's a different way to look at it: we don't build a single ui, but rather a docker system with possible several uis (we already have IPFS ui, game ui)
- in simpler words
  - players will edit code -> that will go to jvm-app -> and it will push state to game gui
  - so although we use two browser windows by means of splitting PC's screen, we input into the same app entity