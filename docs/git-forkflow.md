
## vocabulary

- upstream - the source of truth repo we fork from

## merging on github

- use rebase pull request, it will put incoming commit after the last commit
- say upstream has commits `1-2-5` where numbers represetn time of commits
- and a fork has `1-2-3-4`
- after merge it will be `1-2-5-3-4`, and `3 4` commit hashes will change, as we wanted

## updating local fork afterwards

- say, when all that happened fork added another commit and has `1-2-5-6`, wheres upstream has `1-2-5-3-4`
- we do
```shell
git remote add upstream https://github.com/DeathStarGame/DeathStarGame
git fetch upstream
git rebase -i upstream/main

```
- it will show us only commit `6`, and after rebase it will be `1-2-5-3-4-6`, which is the desired outcome

## updating manually

- it's the same
- we clone the fork, add upstream, rebase onto it fork's changes and push to upstream
- the only difference from fork author is that we have credentials to push to upstream repo
- github does that with rebase pull request, except manually we even can squash commits (but unnecessary - fork author should do that and conflicts)

## conflicts: reoslving is up to fork/PR author

- simple: before creating a pull request or indicating that fork can be merged, fork author should rebase on top of upstream and resolve conflicts if any

## summary

- it's always *linear*, it's always *rebase* onto tip of the upstream and then that on tip of the fork