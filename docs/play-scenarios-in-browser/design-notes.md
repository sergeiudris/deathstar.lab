## app architecture

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