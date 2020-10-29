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