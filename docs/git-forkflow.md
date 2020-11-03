
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


---

## many people, one project: using a branch(s) as a personal lab, hashes as snapshots

- there is `upstream` repo https://github.com/DeathStarGame/DeathStarGame
- a contributor forks, and creates a `design` branch or `lab` branch or uses `main` for that and `upstream` branch to prepare pull requests
- contirbutor's branches are their choice and effectively are unrelated histories, and are never merged
- they are like repos, but as part of the same repo
- they are better than copying into directories (to make snapshots) because all is needed is to create a branch for snapshot and checkout a  new one from any point in history
- there is the `main/lab/design` branch that should be the deafult, so when we open contirbutor's repo we see there current state of mind
- and an `upstream` branch is where we manually copy/rebase/merge somehow files selectively from or lab branch while keeping notes and design
- so it's branches over copying repos and project.lab repo
- a couple more notes:
  - it's elegant, as all our creating process is within one repo, yet still linkable and persistent (we can link to branches, they are like subdirectories of repo)s
  - it's findable: again, we only need to find a fork and can see the whole picture
  - and we clone with a custom name to be able to open multiple snapshots in the editor simultenenously
- the docs and notes stay persistent, while implementations may be snaphostted, experimented
  - so our `main/design` branch has a consistent notes, docs (we don't delete those) but a changing src (we snapshot it, anc can delete-continue or checkout from point
  - but: we cannot simply checkout back in time, we'll lose notes, so better to move-delete-move and keep links/branches for snaphosts (better just hashes, can be checkout out the same)
- so essentailly, our for has only ever **two** branches - `main` and `upstream`, everyhting else are links to hashes
- we can consider `lab` subdir flow: keep all personal/experimetal files in `lab` directory, so that we can easier select/exclude on merge into `upstream`, and `upstream` goes into pull requests
- since we'll rebase only into upstream, this boils down to copying ; if we can somehow do it via interactive rebase - cool, but there is always a danger of accidentally commiting something from `lab` dir
- but that's fine, we can always delete `usptream` branch and copy it from upstream repo itself
- how to have `~/code/` contain both `DeathStarGame` and `cljctools`
  - well, it's trivial - it is exaclty our fork (plus some files)
  - and if we need a snaphost, we can clone in another dir and checkout hash (see both in the editor)

## git how to merge only certain directories from fork

- https://stackoverflow.com/questions/449541/how-can-i-selectively-merge-or-pick-changes-from-another-branch-in-git
  - https://jasonrudolph.com/blog/2009/02/25/git-tip-how-to-merge-specific-files-from-another-branch/

## git pull from fork

- Git rebase from remote fork repo
  - https://gist.github.com/ravibhure/a7e0918ff4937c9ea1c456698dcd58aa

## git attributes to ingore files on merge

- https://stackoverflow.com/questions/15232000/git-ignore-files-during-merge
  - https://git-scm.com/book/en/v2/Customizing-Git-Git-Attributes#_merge_strategies
- https://medium.com/@porteneuve/how-to-make-git-preserve-specific-files-while-merging-18c92343826b