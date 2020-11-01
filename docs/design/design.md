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

## source code of the system (DeathStarGame repo) shared between containers? 

- VScode container and jvm-app preferably should share a filesystem, so that only jvm-app did all fs writes
- another thing - when the system runs, ideally, it should be possible to REPL into it using its own VScode browser ui, and for that the source code should come with the system
- another case - when traefik, IPFS , ui-prod ... etc. containers will be lauchned, they will need to access the config files (which are usually COPYd into on image build or passed as docker-compose env variables)
- but what if it was possible to give all caontainers access to a volume of sorts, that would contain the source - github.com/DeathStarGame/DeathStarGame - a central singular instance of all source
- than, every container could access it's config from this root directory (via volume) , and there would be no need to COPY it in every Dockerfile
- can the dev/prod separation be avoided and can the system come as an OS of sorts?
- this way, the ui build container (with shadow-cljs) could output files as is into respective out dirs within DeathStarGame repo tree, and ui-prod could serve that path directly, no need for copying
- another approach: single container with scripts to start/stop binaries (can even consider a scripting alternative to bash as with a container it's a trivial apt install)

## running the system inside a single container

- first of all, it works (no docker-compose files, restart:always and container tools, but scripts instead)
- it works with giving repo as a volume to several containers( ok that build will override files ,.user dirs will be shared),apps will point to the same src code, so restarting apps ok
- needed an alternative to bash - a lisp, preferably clojure - so that all scripts (even in f files when docking containers) were in a sane language
- with one container, we lose cloud tools and have to script, but gain a bit more flexibility(programmability) and a sort of simplicity/singularity
- bash alternative: only if it has *interoperability* with bash, such that we translate bash examples in docs back and forth without guessing

## distinct builder and runner docker-compose services(containers)

- one builder will run shadow-cljs that will compile both ui and vscode extension
- another will build uberjar which will be run from a slimmer jre-only container in release version (if ran without REPLs into system itself)
- all services will share a volume (DeathStarGame repo), builders will output to usual target/out dirs and runners will run from them

## what installing a scenario looks like?

- sidenote: scenario process, running on the server, should control it's own render (even if it's embedded into game gui), so system only talks to one scneario process
- can installing a scenario mean spinning up a container?
- can scenario gui run in a separate tab? 
- can scenario server-side be a nodejs app? a jvm-app?
- so can scanerios be built as apps interacting with DeathStarGame apis
- if scenario is installed as a regular dep, deos it mean (require-ing) it in jvm builder and adding a :build target and compiling renderer in shadow-cljs builder?

## ~~yes, scenarios should be apps, spinned up in containers~~

- <s>we are already in docker
- sceanrio creaters should be elevated to building real apps
- when relying on apis, scenario dependencies and ideas stay even more free, with the feeling of building an appliaction, not jsut scripting, so the quality of scenarios will rise exponentially as more people will be willing and excited to really go for it with scenarios
- users should build apps that use DeathStarGame api or are used by it
- communicate bidirectionally over rsocket
- system and scenraios talk using data, each having its own runtime (system has several), giving natuaral isolation
- calrity and ease of development: we launch the system and then start/stop scneario app only, whereas system is always running as a whole and exposes only apis
- scnearios being apps of their own provide isolation, definitiveness of api, dependency freedom, decoupling of system and scnearios
- scenarios become true extensions, addons, or microservices to the system
- users creating scenraios build literally apps of their own, while system focuses on api communication over how-do-we-intergate-these-scripts (although we have docker and containers)
- should be possible to develop such scenario apps using the system itself
  - browser vscode would open a directory with such app's code (copied from a template directory from DeathStarGame repo)
  - system's shadow-cljs builder would add a build target and compile the app, expose nrepl, system would connect vscode to nrepl and spin up a container with this new scenario
  - now we have a repl into an app and see scenario's gui inside a tab (iframe)
  - we have a button that restarts the app container if needed (or posisbly even use VSCode's terminal into apps container, so user can restart themselves)
  - one we done, we can copy the code from the system on PC and put into a repo or smth</s>


## ~~sceanrio ui as iframe inside game ui~~

- <s>from user experience, we need to see player's scenario views and a combined view, meaning being able to switch between multiple small tabs (or open them alongide each other) inside single browser tab
- if scencario gui is an app, openable even in a standlone tab, it should be used by the as iframe</s>

<img height="512px" src="./svg/2020-11-01-iframes.svg"></img>


## installing scenarios: as namespaces

- sceanrios are simply code
- are developable only within the system, which is as it should be
- installing would mean 
  - copying files from a url into the app entity's filesystem
  - jvm app would compile(or even eval them) thus creating scenario's namespace
  - option A: ui builder would adds a new build target and compiles an app, which is used by iframe
  - option B: renderer code is directly sent by jvm-app to game-ui and evaled there

## scenario as a library: game forms the state on jvm and delivers to game-ui, which passes it to scenario renderers

- simulations run on jvm and new state is formed for that game in it's unique generated namespace
- and game passes it over its connection to game-ui and subsequently to sceanrio renderer
- as opposed to scenario updating its own renderers over unique connection
- why: game may choose to render that scenario multiple times and it should be in charge of that
- if so, than every such renderer cannot have a whole connection back to scenario app (otherwise there will be a connection for each tab rendering state of a particular player)
- or tabs for dev and latest game state for the player themselves
- point is: game should be free to render state multiple times and in a selective manner