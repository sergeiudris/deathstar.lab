## app design

<img height="512px" src="./svg/2020-10-25-extensions.svg"></img>

- extensions self "register" - perform needed ops using `deathstar.ext.main.chan` api, main has app's db(state) and exposes ops
  - e.g. `(ext.main.chan/add-tab ops{name 'resolvable-symbol-to-renderer ..})` - assoc symbol into db atom and we see gui updated with for example new tab
  `(ext.main.chan/another-state-changing-op..)`
- keep state as data: only serializable (use symbols if needed)
- `src/ext` dir structure
  - `main-meta` - has channel api, spec, protocols ..
  - `main-impl` - implementation
  - `router-meta`
  - `router-impl`
  - `scenario-list-meta`
  - `scenario-list-impl`
  ...
- app is siply a main.cljs file that creates channels, creates state(s), imports extensions and starts them like `(ext.foo/proc-ops channels state)`
- `scenario-list`, for exmaple, when it starts, adds item to the list of tabs (navigation) and respective tab content (via  `(main.chan/add-this)` `(main.chan/add-that)`
- extension explorer(panel) is a list, on click coresponding symbol (like `ext.foo.renderer/rc-main`) is resolved and used for rendering
- channels are formed as usual - by merging `(ext.foo/create-channels)`
- as usual, all kewords are namespaced, no collisions/ambiguity
- extensions are smart : they tap/pipe into needed channels
- GUI-wise: VSCode best practices, e.g. use collapsible sidebar (left) for extensions
- state 
  - extensions on entry create their default state and merge it into ext.main state (it's an atom they get as arg)
  - app's state is a flat map of fully qualified keywords (ext.foo/some-val ext.bar/pred? ..)
  - esentially, extensions are free to change state, as if they write to db via connection
- extensions have `pipes` method
  - a function that does piping so that proc-ops stays clean
  - proc-ops calls `pipes` itself
- scenarios
  - also expose `create-proc-ops` and api in `sceanrio.chan` (`create-simulation`, `start` ... and other api for game to use)
  - have state for gui and game space(simulation), we render those
  - scenarios know how to determine the winner and notify the game (or the game uses some `check-if-game-complete` api fn
- 0, 1 or more players hub process with an api
  - design in such way that player "connects/joins" the "hub", so that it's possible to add players (hotseat, AI(bots) or some other way)
  - it should be abstracted in a way that we join the "multiplayer", when 1 player it becomes single player
  - hub is an extension (just writes state to db atom) and exposes api that "virtual" user uses (us, or hotseat or bot or libp2p..)
  - hotseat: nope, not needed, boring and obsolete - if multiple people are at a single pc, they can enjoy completeing scenarios together and possibly playing vs AI
  - so design is always for simultaneous simulations (0,1 or more) players
- conquest and 1 player: continue playing option
  - simple: if scenario implies conquest victory and we start single player, we see "vicotry" and option "continue playing" - so we play on
- identity
  - on opening browser page, user enters their name
  - that name plus some id (random uuid or maybe smth from libp2p) - is a unique id for games
  - data is ephimeral, play-scenarios-in-browser by design does not store data, so name+id is sufficient
- no HTTP (*sigh of joy*): first it's a legacy protocol, won't be used in DeathStarGame overall, second - play-scenarios-in-browser is single-player and *maybe* will use p2p
- versions
  - name like v20201025, keep previous deployments so possible to open DeathStarGame.github.io/play-scenarios-in-browser/v20201025
- evaluating user's code
  - use self-hosted cljs obviously